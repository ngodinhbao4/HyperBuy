document.addEventListener("DOMContentLoaded", () => {
  loadStorePage();
});

async function loadStorePage() {
  const params = new URLSearchParams(window.location.search);
  const sellerId = params.get("sellerId");

  const infoBox = document.getElementById("store-info");
  const grid = document.getElementById("store-products-grid");

  if (!sellerId) {
    infoBox.innerHTML = `<p class="error-message">Thiếu sellerId.</p>`;
    grid.innerHTML = "";
    return;
  }

  const res = await callApi(
    USER_API_BASE_URL,
    `/users/${sellerId}/store`,
    "GET",
    null,
    true
  );

  if (!res.ok || !res.data?.result) {
    infoBox.innerHTML = `<p class="error-message">Không tải được thông tin cửa hàng.</p>`;
    grid.innerHTML = `<p class="error-message">Không tải được sản phẩm.</p>`;
    return;
  }

  const store = res.data.result;
  console.log("🏪 STORE DATA =", store);

  document.title = `${store.storeName} | HyperBuy`;

  infoBox.innerHTML = `
    <div class="store-info-header">
      <h1>${store.storeName}</h1>
      <p>Chủ cửa hàng: <strong>${store.username || "N/A"}</strong></p>
    </div>
    <div class="store-info-body">
      <p><strong>GPKD:</strong> ${store.businessLicense || "Chưa cập nhật"}</p>
    </div>
  `;

  const products = Array.isArray(store.products) ? store.products : [];

  if (!products.length) {
    grid.innerHTML = "<p>Chưa có sản phẩm nào.</p>";
    return;
  }

  grid.innerHTML = products.map(p => `
    <div class="product-card">
      <img src="${resolveProductImage(p.imageUrl)}" alt="${p.name || "Sản phẩm"}">
      <h4>${p.name || "Sản phẩm"}</h4>
      <p class="price">${Number(p.price || 0).toLocaleString("vi-VN")} đ</p>
      <a href="product-detail.html?id=${p.id}" class="btn btn-primary btn-sm">
        Xem chi tiết
      </a>
    </div>
  `).join("");
}

function resolveProductImage(url) {
  if (!url) return "https://placehold.co/300x200?text=No+Image";

  if (url.startsWith("http://productservice")) {
    return url.replace(/^http:\/\/productservice:\d+/, PRODUCT_IMAGE_BASE_URL);
  }

  if (url.startsWith("http://localhost:8081")) {
    return url.replace("http://localhost:8081", PRODUCT_IMAGE_BASE_URL);
  }

  if (url.startsWith("http")) return url;

  return `${PRODUCT_IMAGE_BASE_URL}${url.startsWith("/") ? "" : "/"}${url}`;
}
