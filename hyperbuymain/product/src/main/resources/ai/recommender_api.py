# recommender_api.py
from fastapi import FastAPI
from typing import List
import pickle
import numpy as np
from scipy.sparse import load_npz

from train_recommender import (
    recommend_for_user,
    build_interaction_weight,
    load_data_union,
    build_mappings_and_matrix,
    train_als
)

app = FastAPI(title="CF Recommendation Service")

# =============================
# LOAD MODEL KHI START SERVER
# =============================
print("Loading data & training model...")

df = load_data_union()
df_w = build_interaction_weight(df)
user_to_idx, idx_to_user, item_to_idx, idx_to_item, X_user_item = build_mappings_and_matrix(df_w)
X_item_user = X_user_item.T.tocsr()
model = train_als(X_item_user)

print("Model ready!")

# =============================
# API: recommend cho user
# =============================
@app.get("/recommend", response_model=List[int])
def recommend(username: str, n: int = 10):
    return recommend_for_user(
        model=model,
        X_user_item=X_user_item,
        user_to_idx=user_to_idx,
        idx_to_item=idx_to_item,
        username=username,
        N=n
    )

# =============================
# API: guest (fallback)
# =============================
@app.get("/guest", response_model=List[int])
def recommend_guest(n: int = 10):
    # fallback đơn giản: top popular (ở đây demo: random)
    return list(idx_to_item.values())[:n]



@app.get("/similar", response_model=List[int])
def similar(product_id: int, n: int = 10):
    # item-to-item bằng implicit built-in similar_items
    if product_id not in item_to_idx:
        return []

    i = int(item_to_idx[product_id])

    recs = model.similar_items(i, N=n + 1)  # có cả chính nó
    # recs có thể là (ids, scores) hoặc list tuples
    if isinstance(recs, tuple) and len(recs) == 2:
        ids, _scores = recs
        ids = list(ids)
    else:
        ids = [int(x[0]) for x in recs]  # Nx2

    out = []
    for j in ids:
        j = int(j)
        if j == i:
            continue
        out.append(int(idx_to_item[j]))
        if len(out) >= n:
            break

    return out