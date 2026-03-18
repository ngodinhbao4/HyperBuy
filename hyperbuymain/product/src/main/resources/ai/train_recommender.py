# train_recommender.py
# CF Recommendation bằng Implicit ALS (Collaborative Filtering)
# - Dùng 3 tín hiệu: view, purchase, rating
# - Train: ALS (implicit) trên item-user matrix
# - Recommend: truyền 1 dòng user_items = X_user_item[u] (1 x items) để hợp với implicit version hiện tại
# - Không cần lưu score/rank vào DB (demo in ra kết quả)

import os

# (Tuỳ chọn) giảm warning hiệu năng OpenBLAS
os.environ.setdefault("OPENBLAS_NUM_THREADS", "1")

import numpy as np
import pandas as pd
import pymysql

from scipy.sparse import coo_matrix
from implicit.als import AlternatingLeastSquares

# =============================
# 0. TRỌNG SỐ & KẾT NỐI
# =============================
VIEW_WEIGHT   = 1.0
BUY_WEIGHT    = 20.0
RATING_WEIGHT = 5.0
BASE_RATING   = 3.0

MYSQL_HOST = "localhost"
MYSQL_PORT = 3307
MYSQL_USER = "root"
MYSQL_PASSWORD = "baotrong"

PRODUCT_DB = "hyperbuy_product_db"
ORDER_DB   = "hyperbuy_order_db"


def get_connection(db_name: str):
    return pymysql.connect(
        host=MYSQL_HOST,
        port=MYSQL_PORT,
        user=MYSQL_USER,
        password=MYSQL_PASSWORD,
        database=db_name,
        cursorclass=pymysql.cursors.DictCursor
    )

# =============================
# 1. LOAD DATA (GỘP 3 NGUỒN)
# =============================
def load_data_union() -> pd.DataFrame:
    conn = get_connection(PRODUCT_DB)

    sql = """
        SELECT
            username,
            product_id,
            SUM(view_count)   AS view_count,
            SUM(buy_count)    AS buy_count,
            AVG(rating_value) AS rating_value
        FROM (

            SELECT
                pvh.username    AS username,
                pvh.product_id  AS product_id,
                1               AS view_count,
                0               AS buy_count,
                NULL            AS rating_value
            FROM hyperbuy_product_db.product_view_history pvh
            WHERE pvh.username IS NOT NULL

            UNION ALL

            SELECT
                o.user_id       AS username,
                oi.product_id   AS product_id,
                0               AS view_count,
                oi.quantity     AS buy_count,
                NULL            AS rating_value
            FROM hyperbuy_order_db.orders o
            JOIN hyperbuy_order_db.order_items oi
                ON o.id = oi.order_id
            WHERE o.user_id IS NOT NULL
              AND o.status IN ('CONFIRMED','DELIVERED')

            UNION ALL

            SELECT
                pr.username     AS username,
                pr.product_id   AS product_id,
                0               AS view_count,
                0               AS buy_count,
                pr.rating_value AS rating_value
            FROM hyperbuy_product_db.product_ratings pr
            WHERE pr.username IS NOT NULL

        ) AS t
        GROUP BY username, product_id;
    """

    with conn.cursor() as cur:
        cur.execute(sql)
        rows = cur.fetchall()
    conn.close()

    df = pd.DataFrame(rows)
    if df.empty:
        return df

    # Clean
    df = df.dropna(subset=["username", "product_id"])
    df["username"] = df["username"].astype(str).str.strip()

    df["product_id"] = pd.to_numeric(df["product_id"], errors="coerce")
    df = df.dropna(subset=["product_id"])
    df["product_id"] = df["product_id"].astype(int)

    df["view_count"] = pd.to_numeric(df["view_count"], errors="coerce").fillna(0.0)
    df["buy_count"] = pd.to_numeric(df["buy_count"], errors="coerce").fillna(0.0)
    df["rating_value"] = pd.to_numeric(df["rating_value"], errors="coerce").fillna(BASE_RATING)
    df["rating_value"] = df["rating_value"].clip(1.0, 5.0)

    return df

# =============================
# 2. TẠO WEIGHT CHO IMPLICIT CF
# =============================
def build_interaction_weight(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()

    w_view = VIEW_WEIGHT * np.log1p(df["view_count"].astype(float))
    w_buy  = BUY_WEIGHT  * df["buy_count"].astype(float)
    w_rate = np.where(df["rating_value"].astype(float) >= 4.0, RATING_WEIGHT, 0.0)

    df["weight"] = w_view + w_buy + w_rate

    # Bỏ dòng không có tín hiệu
    df = df[df["weight"] > 0].copy()

    return df[["username", "product_id", "weight"]]

# =============================
# 3. BUILD MAPPING + SPARSE MATRIX
# =============================
def build_mappings_and_matrix(df_w: pd.DataFrame):
    users = df_w["username"].unique()
    items = df_w["product_id"].unique()

    user_to_idx = {u: i for i, u in enumerate(users)}
    idx_to_user = {i: u for u, i in user_to_idx.items()}

    item_to_idx = {p: i for i, p in enumerate(items)}
    idx_to_item = {i: p for p, i in item_to_idx.items()}

    df_w = df_w.copy()
    df_w["u"] = df_w["username"].map(user_to_idx)
    df_w["i"] = df_w["product_id"].map(item_to_idx)

    # users x items
    X_user_item = coo_matrix(
        (df_w["weight"].astype(float), (df_w["u"].astype(int), df_w["i"].astype(int))),
        shape=(len(users), len(items))
    ).tocsr()

    return user_to_idx, idx_to_user, item_to_idx, idx_to_item, X_user_item

# =============================
# 4. TRAIN ALS (CF)
# =============================
def train_als(X_item_user, factors=64, reg=0.01, iterations=20):
    model = AlternatingLeastSquares(
        factors=factors,
        regularization=reg,
        iterations=iterations
    )
    model.fit(X_item_user)  # items x users (CSR)
    return model

# =============================
# 5. RECOMMEND (FIX: truyền 1 dòng user_items)
# =============================
def recommend_for_user(model, X_user_item, user_to_idx, idx_to_item, username, N=10):
    if username not in user_to_idx:
        return []

    u = int(user_to_idx[username])
    user_items_1row = X_user_item[u]  # (1 x items)

    recs = model.recommend(
        userid=0,
        user_items=user_items_1row,
        N=N,
        filter_already_liked_items=True
    )

    # ----- parse output of implicit (nhiều version) -----
    item_indices = None

    # Case 1: (ids, scores)
    if isinstance(recs, tuple) and len(recs) == 2:
        ids, _scores = recs
        item_indices = np.asarray(ids)

    else:
        arr = np.asarray(recs)

        # Case 2: ndarray NxK (K>=2). Thường item id nằm ở cột 0 hoặc 1 tùy version.
        if arr.ndim == 2:
            # thử cột 0 trước
            cand0 = arr[:, 0]
            # nếu cand0 có nhiều giá trị không nằm trong mapping mà cột 1 có vẻ đúng -> dùng cột 1
            cand0_ok = np.mean([int(x) in idx_to_item for x in np.asarray(cand0).astype(int)]) if len(cand0) else 0
            if arr.shape[1] > 1:
                cand1 = arr[:, 1]
                cand1_ok = np.mean([int(x) in idx_to_item for x in np.asarray(cand1).astype(int)]) if len(cand1) else 0
                item_indices = cand1 if cand1_ok > cand0_ok else cand0
            else:
                item_indices = cand0

        # Case 3: list/array 1D: [id1, id2, ...]
        elif arr.ndim == 1:
            item_indices = arr

    if item_indices is None:
        return []

    # ----- sanitize indices to avoid KeyError -----
    num_items = len(idx_to_item)
    out = []
    for x in np.asarray(item_indices):
        try:
            i = int(x)
        except Exception:
            continue

        # Nếu bị 1-based (1..num_items) thì chỉnh về 0-based
        if i not in idx_to_item and (i - 1) in idx_to_item:
            i = i - 1

        # Bỏ các id không hợp lệ
        if i < 0 or i >= num_items:
            continue
        if i not in idx_to_item:
            continue

        out.append(int(idx_to_item[i]))

    # unique nhưng giữ thứ tự
    seen = set()
    final = []
    for pid in out:
        if pid not in seen:
            seen.add(pid)
            final.append(pid)
        if len(final) >= N:
            break

    return final


def similar_items(model, item_ext_id, item_to_idx, idx_to_item, N=10):
    if item_ext_id not in item_to_idx:
        return []

    i = int(item_to_idx[item_ext_id])

    # implicit có hàm similar_items sẵn
    recs = model.similar_items(i, N=N+1)  # gồm cả chính nó
    # recs có thể là (ids, scores) hoặc list tuple, nên parse giống recommend

    item_indices = None
    if isinstance(recs, tuple) and len(recs) == 2:
        ids, _scores = recs
        item_indices = np.asarray(ids)
    else:
        arr = np.asarray(recs)
        item_indices = arr[:, 0] if arr.ndim == 2 else arr

    out = []
    for j in item_indices:
        j = int(j)
        if j == i:
            continue
        if j in idx_to_item:
            out.append(int(idx_to_item[j]))
        if len(out) >= N:
            break
    return out

# =============================
# 6. MAIN
# =============================
def main():
    print("=== LOAD DATA (union 3 nguồn) ===")
    df = load_data_union()
    if df.empty:
        print("Không có dữ liệu → dừng.")
        return

    df_w = build_interaction_weight(df)
    if df_w.empty:
        print("Sau khi tạo weight, không còn tương tác → dừng.")
        return

    user_to_idx, idx_to_user, item_to_idx, idx_to_item, X_user_item = build_mappings_and_matrix(df_w)

    # Train cần items x users
    X_item_user = X_user_item.T.tocsr()

    print(f"Users: {len(user_to_idx)} Items: {len(item_to_idx)} Interactions: {X_user_item.nnz}")

    print("=== TRAIN ALS (CF implicit) ===")
    model = train_als(X_item_user, factors=64, reg=0.01, iterations=20)

    print("=== DEMO RECOMMEND ===")
    demo_user = list(user_to_idx.keys())[0]
    rec_products = recommend_for_user(model, X_user_item, user_to_idx, idx_to_item, demo_user, N=10)

    print(f"Demo user: {demo_user}")
    print("Recommended product_ids:", rec_products)

    demo_item = list(item_to_idx.keys())[0]
    print("Similar to item", demo_item, "=>", similar_items(model, demo_item, item_to_idx, idx_to_item, N=10))


    print("=== DONE ===")

if __name__ == "__main__":
    main()
