const USER_API_BASE_URL = 'http://localhost:8080/user';
const PRODUCT_API_BASE_URL = 'http://localhost:8081/api/v1';
const PRODUCT_IMAGE_BASE_URL = 'http://localhost:8081';
const CART_API_BASE_URL = 'http://localhost:8082/api/v1/carts';
const ORDER_API_BASE_URL = 'http://localhost:8083/api/v1';
const PAYMENT_API_BASE_URL = 'http://localhost:8084/';
const NOTIFICATION_API_BASE_URL = 'http://localhost:8085';
const MINIGAME_API_BASE_URL = "http://localhost:8087/minigame";
const VOUCHER_API_BASE_URL = "http://localhost:8089/voucher";
const CHECKOUT_ITEMS_KEY = "CHECKOUT_SELECTED_ITEMS";
let checkoutItemsPayload = [];
let categoryMap = new Map();
window.appliedVoucherDiscount = 0;

function getParam(param) {
    const url = new URL(window.location.href);
    return url.searchParams.get(param);
}
// Khai báo các biến global cho sidebar để hàm closeSidebar có thể truy cập
let categorySidebarGlobal, sidebarOverlayGlobal, closeSidebarButtonInternalGlobal;

// Hàm tiện ích để gọi API
async function callApi(baseUrl, endpoint, method = 'GET', body = null, requiresAuth = false, isFormData = false, additionalHeaders = {}) { // Thêm additionalHeaders
    const headers = { ...additionalHeaders }; // Khởi tạo headers với additionalHeaders
    const token = localStorage.getItem('authToken'); // Đảm bảo key 'authToken' là đúng

    if (requiresAuth && token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    // Chỉ đặt Content-Type nếu không phải FormData, body là object, và chưa được đặt trong additionalHeaders
    if (!isFormData && (method === 'POST' || method === 'PUT' || method === 'PATCH') && body && typeof body === 'object') {
        if (!headers['Content-Type']) { // Không ghi đè nếu Content-Type đã có trong additionalHeaders
            headers['Content-Type'] = 'application/json';
        }
    }
    // Nếu isFormData, trình duyệt sẽ tự đặt Content-Type (multipart/form-data)

    const config = { method, headers };
    if (body) {
        // Nếu là FormData hoặc không phải object, gửi body trực tiếp
        // Nếu là object và không phải FormData, chuyển thành JSON string
        config.body = (isFormData || typeof body !== 'object') ? body : JSON.stringify(body);
    }

    try {
        const response = await fetch(`${baseUrl}${endpoint}`, config);
        const responseContentType = response.headers.get('content-type');
        let data = null;

        if (response.status === 204) { // No Content
            return { ok: response.ok, status: response.status, data: null };
        }

        if (responseContentType?.includes('application/json')) {
            data = await response.json();
        } else {
            const textData = await response.text();
            try {
                // Cố gắng parse textData thành JSON, phòng trường hợp server trả về JSON nhưng Content-Type sai
                data = JSON.parse(textData);
            } catch (e) {
                // Nếu không parse được, trả về textData như một phần của message
                // Hoặc nếu textData rỗng (ví dụ lỗi 500 không có body), thì dùng thông báo mặc định
                data = { message: textData || `Lỗi từ server với mã trạng thái ${response.status}` };
            }
        }
        return { ok: response.ok, status: response.status, data: data };
    } catch (error) {
        console.error(`Lỗi gọi API ${method} ${baseUrl}${endpoint}:`, error);
        return { ok: false, status: 0, data: null, error: error.message || 'Lỗi mạng hoặc không thể kết nối.' };
    }
}

function saveToken(token) { localStorage.setItem('authToken', token); }
function getToken() { return localStorage.getItem('authToken'); }
function clearToken() { localStorage.removeItem('authToken'); localStorage.removeItem('userRole'); }
function isLoggedIn() { return !!getToken(); }
function getUserRole() { return localStorage.getItem('userRole'); }

function updateNav() {
    const loggedIn = isLoggedIn();
    const userRole = getUserRole(); // Lấy vai trò 1 lần
    const isAdmin = userRole === 'ADMIN';
    const isSeller = userRole === 'SELLER';

    const navLogin = document.getElementById('nav-login');
    const navRegister = document.getElementById('nav-register');
    const navProfile = document.getElementById('nav-profile');
    const navLogout = document.getElementById('nav-logout');
    const navMyOrders = document.getElementById('nav-my-orders');
    const navNotificationBell = document.getElementById('nav-notification-bell');
    const navAddProduct = document.getElementById('nav-add-product');
    const navAdminDashboard = document.getElementById('nav-admin-dashboard') || document.getElementById('nav-admin-dashboard-li');

    if (navLogin) navLogin.style.display = loggedIn ? 'none' : 'block';
    if (navRegister) navRegister.style.display = loggedIn ? 'none' : 'block';
    if (navProfile) navProfile.style.display = loggedIn ? 'block' : 'none';
    if (navLogout) navLogout.style.display = loggedIn ? 'block' : 'none';
    if (navMyOrders) navMyOrders.style.display = loggedIn ? 'block' : 'none';

    // *** SỬA ĐỔI Ở ĐÂY ***
    // Hiển thị Thêm SP cho Admin hoặc Seller
    if (navAddProduct) navAddProduct.style.display = loggedIn && (isAdmin || isSeller) ? 'block' : 'none';
    // *** KẾT THÚC SỬA ĐỔI ***

    if (navAdminDashboard) navAdminDashboard.style.display = loggedIn && isAdmin ? 'block' : 'none';
    if (navNotificationBell) navNotificationBell.style.display = loggedIn ? 'inline-block' : 'none';
}

async function initializeCart() {
    if (!isLoggedIn()) return false;
    const result = await callApi(CART_API_BASE_URL, '/init', 'POST', '1', true);
    if (result.ok) {
        await fetchCartData();
        return true;
    } else {
        console.error(`initializeCart: Lỗi gọi API /init. Status: ${result.status}`, result.data || result.error);
        return false;
    }
}
async function handleRegister(event) {
    event.preventDefault();
    const form = event.target;
    const data = { username: form.username.value, password: form.password.value, name: form.name.value, email: form.email.value, dob: form.dob.value };
    const msgEls = { success: document.getElementById('register-success-message'), error: document.getElementById('register-error-message') };
    Object.values(msgEls).forEach(el => { if (el) { el.style.display = 'none'; el.textContent = ''; } });
    const result = await callApi(USER_API_BASE_URL, '/users/register', 'POST', data);
    if (result.ok && result.data?.code === 1000) {
        if (msgEls.success) { msgEls.success.textContent = 'Đăng ký thành công! Bạn có thể đăng nhập.'; msgEls.success.style.display = 'block'; }
        form.reset();
    } else {
        if (msgEls.error) { msgEls.error.textContent = `Lỗi: ${result.data?.message || result.error || 'Đăng ký thất bại.'}`; msgEls.error.style.display = 'block'; }
    }
}

async function handleLogin(event) {
    event.preventDefault();
    const form = event.target;
    const errorMsgEl = document.getElementById('login-error-message');
    if (errorMsgEl) { errorMsgEl.style.display = 'none'; errorMsgEl.textContent = ''; }

    const result = await callApi(USER_API_BASE_URL, '/auth/signin', 'POST', { username: form.username.value, password: form.password.value });

    if (result.ok && result.data?.result?.token) {
        saveToken(result.data.result.token);
        await loadProfileData();

        // *** THÊM DÒNG NÀY ***
        await fetchAndStoreMyStoreId();
        // *** KẾT THÚC THÊM ***

        updateNav();
        await fetchCartData();
        await fetchMyNotifications();


        const userRole = getUserRole();
        if (userRole === 'ADMIN') {
            window.location.href = 'admin.html';
        } else {
            window.location.href = 'profile.html';
        }
    } else {
        if (errorMsgEl) {
            errorMsgEl.textContent = `Lỗi: ${result.data?.message || result.error || 'Đăng nhập thất bại.'}`;
            errorMsgEl.style.display = 'block';
        }
    }
}
async function handleLogout() {
    if (getToken()) {
        try { await callApi(USER_API_BASE_URL, '/auth/logout', 'POST', { token: getToken() }, true); }
        catch (error) { console.error("Lỗi API đăng xuất:", error); }
    }
    clearToken();
    updateNav();
    updateCartUI(null);
    updateNotificationUI(null);
    window.location.href = 'login.html';
}
async function loadProfileData() {
    if (!isLoggedIn()) {
        clearToken();
        return null;
    }
    const result = await callApi(USER_API_BASE_URL, '/users/myInfo', 'GET', null, true);
    if (result.ok && result.data?.result) {
        const user = result.data.result;
        let userRole = 'USER';
        if (user.role?.some(r => r.name?.toUpperCase() === 'ADMIN')) userRole = 'ADMIN';
        else if (user.role?.some(r => r.name?.toUpperCase() === 'SELLER')) userRole = 'SELLER';
        localStorage.setItem('userRole', userRole);

        if (user.id) {
            localStorage.setItem('currentUserId', user.id);
        } else {
            localStorage.removeItem('currentUserId');
        }
        // LƯU sellerRequestStatus TỪ API NẾU CÓ
        if (typeof user.sellerRequestStatus !== 'undefined') {
            localStorage.setItem('sellerRequestStatus', user.sellerRequestStatus);
        } else {
            localStorage.removeItem('sellerRequestStatus');
        }
        return user;
    }
    clearToken();
    return null;
}


// Hàm xử lý yêu cầu đăng ký bán hàng (đã cung cấp ở lượt trước, đảm bảo nó ở đây)
async function handleSellerRegistrationRequest(event) {
    event.preventDefault();
    const form = event.target;
    const storeName = form.storeName.value.trim();
    const businessLicense = form.businessLicense.value.trim();
    const messageEl = document.getElementById('seller-request-message');

    if (messageEl) {
        messageEl.textContent = '';
        messageEl.className = '';
        messageEl.style.display = 'none';
    }

    if (!storeName) {
        if (messageEl) {
            messageEl.textContent = 'Vui lòng nhập tên cửa hàng mong muốn.';
            messageEl.className = 'error-message';
            messageEl.style.display = 'block';
        }
        return;
    }

    const requestBody = { storeName, businessLicense };
    const result = await callApi(USER_API_BASE_URL, '/users/request-seller', 'POST', requestBody, true);

    if (result.ok) {
        if (messageEl) {
            messageEl.textContent = 'Yêu cầu đăng ký bán hàng đã được gửi. Vui lòng chờ quản trị viên phê duyệt.';
            messageEl.className = 'success-message';
            messageEl.style.display = 'block';
        }
        form.reset();
        const openBtn = document.getElementById('openSellerRequestModalBtn');
        if (openBtn) openBtn.style.display = 'none';
        const sellerStatusMsg = document.getElementById('seller-status-message');
        if (sellerStatusMsg) sellerStatusMsg.textContent = 'Yêu cầu trở thành người bán đang chờ xử lý.';

        setTimeout(() => {
            const sellerRequestModal = document.getElementById('sellerRequestModal');
            if (sellerRequestModal) sellerRequestModal.style.display = 'none';
        }, 3000);
    } else {
        if (messageEl) {
            messageEl.textContent = `Lỗi: ${result.data?.message || result.error || 'Gửi yêu cầu thất bại.'}`;
            messageEl.className = 'error-message';
            messageEl.style.display = 'block';
        }
    }
}

async function displayProfileOnPage() {
    const els = {
        info: document.getElementById('profile-info'),
        loading: document.getElementById('profile-loading'),
        error: document.getElementById('profile-error')
    };
    const openSellerRequestModalBtn = document.getElementById('openSellerRequestModalBtn');
    const sellerStatusMsg = document.getElementById('seller-status-message');
    const profileStoreActionsDiv = document.getElementById('profile-store-actions'); // Lấy div cha của nút xem cửa hàng
    const btnViewMyStore = document.getElementById('btn-view-my-store');

    // Reset trạng thái ban đầu
    if (els.loading) els.loading.style.display = 'block';
    if (els.info) els.info.style.display = 'none';
    if (els.error) els.error.style.display = 'none';
    if (openSellerRequestModalBtn) openSellerRequestModalBtn.style.display = 'none';
    if (sellerStatusMsg) sellerStatusMsg.textContent = '';
    if (profileStoreActionsDiv) profileStoreActionsDiv.style.display = 'none'; // Ẩn cả khối action cửa hàng
    if (btnViewMyStore) btnViewMyStore.style.display = 'none';


    if (!isLoggedIn()) {
        if (els.error) {
            els.error.textContent = 'Bạn cần đăng nhập để xem thông tin.';
            els.error.style.display = 'block';
        }
        if (els.loading) els.loading.style.display = 'none';
        return;
    }

    const user = await loadProfileData(); // loadProfileData đã lưu userRole và sellerRequestStatus
    // fetchAndStoreMyStoreId cũng đã được gọi ở DOMContentLoaded hoặc handleLogin

    if (els.loading) els.loading.style.display = 'none';

    if (user) {
        if (els.info) {
            els.info.style.display = 'block';
            document.getElementById('profile-username').textContent = user.username || 'N/A';
            document.getElementById('profile-name').textContent = user.name || 'N/A';
            document.getElementById('profile-email').textContent = user.email || 'N/A';
            document.getElementById('profile-dob').textContent = user.dob ? new Date(user.dob).toLocaleDateString('vi-VN') : 'N/A';
            const currentRole = getUserRole();
            document.getElementById('profile-role').textContent = currentRole || 'User';
        }

        // Xử lý hiển thị nút/trạng thái đăng ký bán hàng và nút xem cửa hàng
        const currentRole = getUserRole();
        const sellerRequestStatus = localStorage.getItem('sellerRequestStatus'); // Lấy từ LS
        const userStoreId = localStorage.getItem('userStoreId'); // Lấy từ LS

        if (openSellerRequestModalBtn && sellerStatusMsg) {
            if (currentRole === 'ADMIN') {
                openSellerRequestModalBtn.style.display = 'none';
                sellerStatusMsg.textContent = 'Bạn là Quản trị viên.';
            } else if (currentRole === 'SELLER') {
                openSellerRequestModalBtn.style.display = 'none';
                if (userStoreId) {
                    sellerStatusMsg.textContent = 'Bạn đã là người bán và có cửa hàng.';
                    sellerStatusMsg.className = 'success-message';
                    if (profileStoreActionsDiv) profileStoreActionsDiv.style.display = 'block'; // Hiện khối action
                    if (btnViewMyStore) btnViewMyStore.style.display = 'inline-block'; // Hiện nút xem cửa hàng
                } else {
                    sellerStatusMsg.textContent = 'Bạn là người bán nhưng thông tin cửa hàng chưa sẵn sàng.';
                    sellerStatusMsg.className = 'info-message';
                }
            } else if (sellerRequestStatus === 'PENDING') { // Là USER và đang chờ duyệt
                openSellerRequestModalBtn.style.display = 'none';
                sellerStatusMsg.textContent = 'Yêu cầu trở thành người bán của bạn đang chờ xử lý.';
                sellerStatusMsg.className = 'info-message';
            } else { // Là USER và chưa gửi yêu cầu hoặc bị từ chối
                openSellerRequestModalBtn.style.display = 'inline-block';
                sellerStatusMsg.textContent = '';
            }
        }
    } else {
        if (els.error) {
            els.error.textContent = 'Không thể tải thông tin hồ sơ. Vui lòng thử lại.';
            els.error.style.display = 'block';
        }
    }
}
async function handleViewMyStore() {
    const myStoreInfoContainer = document.getElementById('my-store-info-container');
    const myStoreNameEl = document.getElementById('my-store-name');
    const myStoreIdEl = document.getElementById('my-store-id');
    const myStoreLicenseEl = document.getElementById('my-store-license');
    const myStoreProductsEl = document.getElementById('my-store-products-profile');

    if (!myStoreInfoContainer || !myStoreNameEl || !myStoreIdEl || !myStoreLicenseEl || !myStoreProductsEl) {
        console.error("Một hoặc nhiều element để hiển thị thông tin cửa hàng của tôi không tìm thấy.");
        alert("Lỗi giao diện: Không thể hiển thị thông tin cửa hàng.");
        return;
    }

    const currentUserId = localStorage.getItem('currentUserId');
    const currentUserStoreId = localStorage.getItem('userStoreId');

    if (!currentUserId || !currentUserStoreId) {
        alert("Không tìm thấy thông tin người dùng hoặc cửa hàng. Vui lòng đăng nhập lại với tài khoản Seller.");
        myStoreInfoContainer.style.display = 'none'; // Ẩn nếu có lỗi
        return;
    }

    myStoreInfoContainer.style.display = 'block'; // Hiển thị container
    myStoreNameEl.textContent = 'Đang tải...';
    myStoreIdEl.textContent = 'Đang tải...';
    myStoreLicenseEl.textContent = 'Đang tải...';
    myStoreProductsEl.innerHTML = '<p>Đang tải sản phẩm của cửa hàng...</p>';

    // Gọi API GET /users/{userId}/store (API của bạn)
    const result = await callApi(USER_API_BASE_URL, `/users/${currentUserId}/store`, 'GET', null, true);
    console.log("API response cho thông tin cửa hàng của tôi:", result);

    if (result.ok && result.data && result.data.result) {
        const storeData = result.data.result;
        if (storeData.storeId !== currentUserStoreId) {
            console.warn("Store ID từ API không khớp với userStoreId trong localStorage. Có thể là lỗi dữ liệu.", storeData.storeId, currentUserStoreId);
            // Bạn có thể quyết định hiển thị lỗi hoặc dùng storeData.storeId
        }

        myStoreNameEl.textContent = storeData.storeName || 'N/A';
        myStoreIdEl.textContent = storeData.storeId || 'N/A';
        myStoreLicenseEl.textContent = storeData.businessLicense || 'N/A';

        // Hiển thị sản phẩm
        myStoreProductsEl.innerHTML = '';
        const productList = storeData.products; // Dựa theo image_92349c.jpg

        if (productList && Array.isArray(productList) && productList.length > 0) {
            productList.forEach(p => {
                let imgUrl = p.imageUrl || `https://placehold.co/130x90/EFEFEF/AAAAAA&text=Ảnh`;
                // Xử lý PRODUCT_IMAGE_BASE_URL
                if (imgUrl.startsWith('http://productservice')) {
                    imgUrl = imgUrl.replace(/^http:\/\/productservice:\d+/, PRODUCT_IMAGE_BASE_URL);
                } else if (imgUrl.startsWith('http://localhost:8081')) {
                    imgUrl = imgUrl.replace('http://localhost:8081', PRODUCT_IMAGE_BASE_URL);
                } else if (!imgUrl.startsWith('http://') && !imgUrl.startsWith('https://') && imgUrl.includes('/')) {
                    imgUrl = `${PRODUCT_IMAGE_BASE_URL}${imgUrl}`;
                } else if (!imgUrl.startsWith('http://') && !imgUrl.startsWith('https://') && !imgUrl.includes('/')) {
                    imgUrl = `${PRODUCT_IMAGE_BASE_URL}/product-images/${imgUrl}`;
                }

                const productItemDiv = document.createElement('div');
                productItemDiv.className = 'modal-product-item'; // Tái sử dụng class CSS
                productItemDiv.innerHTML = `
                    <a href="product-detail.html?id=${p.id}" target="_blank" title="${p.name || ''}">
                       <img src="${imgUrl}" alt="${p.name || 'Sản phẩm'}" 
                            onerror="this.onerror=null; this.src='https://placehold.co/130x90/EFEFEF/AAAAAA&text=Ảnh lỗi';">
                       <p class="product-name-modal">${p.name || 'N/A'}</p>
                    </a>
                    <p class="price-modal">${(parseFloat(p.price) || 0).toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })}</p>
                `;
                myStoreProductsEl.appendChild(productItemDiv);
            });
        } else {
            myStoreProductsEl.innerHTML = '<p>Cửa hàng của bạn chưa có sản phẩm nào.</p>';
        }
    } else {
        myStoreNameEl.textContent = 'Lỗi';
        myStoreIdEl.textContent = 'N/A';
        myStoreLicenseEl.textContent = 'N/A';
        myStoreProductsEl.innerHTML = `<p class="error-message">Không thể tải thông tin cửa hàng: ${result.data?.message || result.error || 'Lỗi không xác định.'}</p>`;
    }
}
// --- Categories & Sidebar ---
async function loadCategoriesAndBuildMap(selectId = null) {
    const selectEl = selectId ? document.getElementById(selectId) : null;

    if (selectId && !selectEl) {
        console.error(`loadCategoriesAndBuildMap: Select element with ID "${selectId}" không tìm thấy.`);
    }

    const defaultText = selectId === 'filter-categoryId' ? "-- Tất cả danh mục --" : "-- Chọn danh mục --";

    if (selectEl) {
        selectEl.innerHTML = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = "";
        defaultOption.textContent = defaultText;
        selectEl.appendChild(defaultOption);
        selectEl.value = "";
    }

    const result = await callApi(PRODUCT_API_BASE_URL, '/categories', 'GET', null, isLoggedIn());
    categoryMap.clear();

    if (result.ok && result.data) {
        // Hỗ trợ tất cả các kiểu JSON trả về
        let categories = [];
        if (Array.isArray(result.data)) {
            categories = result.data;
        } else if (Array.isArray(result.data.content)) {
            categories = result.data.content;
        } else if (Array.isArray(result.data.result)) {
            categories = result.data.result;
        }

        categories.forEach(cat => {
            if (cat.id && cat.name) {
                categoryMap.set(String(cat.id), cat.name);
                if (selectEl) {
                    const o = document.createElement('option');
                    o.value = cat.id;
                    o.textContent = cat.name;
                    selectEl.appendChild(o);
                }
            }
        });
        console.log("✅ Đã tải danh mục:", categories);
    } else {
        console.error('❌ Lỗi tải danh mục từ API:', result.error || result.data?.message);
    }
}
// ================== LỌC & TÌM KIẾM SẢN PHẨM (TRANG products.html) ==================
async function fetchProductsByFilters(page = 0) {
    try {
        const nameInput = document.getElementById("name");
        const categorySelect = document.getElementById("category");
        const minPriceInput = document.getElementById("minPrice");
        const maxPriceInput = document.getElementById("maxPrice");
        const sortSelect = document.getElementById("sort");

        const name = nameInput ? nameInput.value.trim() : "";
        const categoryId = categorySelect ? categorySelect.value : "";
        const minPrice = minPriceInput ? minPriceInput.value : "";
        const maxPrice = maxPriceInput ? maxPriceInput.value : "";
        const sortValue = sortSelect ? sortSelect.value : "";

        // Filters gửi lên backend
        const filters = {
            page,
            size: 12
        };

        // BE: tìm kiếm tên sản phẩm dùng param q
        if (name) filters.q = name;
        if (categoryId) filters.categoryId = categoryId;
        if (minPrice) filters.minPrice = minPrice;
        if (maxPrice) filters.maxPrice = maxPrice;

        if (sortValue === "priceAsc") {
            filters.sort = "price,asc";
        } else if (sortValue === "priceDesc") {
            filters.sort = "price,desc";
        }

        // --- Cập nhật query string trên URL (để F5 / copy link vẫn giữ bộ lọc) ---
        const urlParams = new URLSearchParams();
        if (name) urlParams.set("name", name);
        if (categoryId) urlParams.set("category", categoryId);
        if (minPrice) urlParams.set("minPrice", minPrice);
        if (maxPrice) urlParams.set("maxPrice", maxPrice);
        if (sortValue) urlParams.set("sort", sortValue);

        const newUrl =
            urlParams.toString().length > 0
                ? `${window.location.pathname}?${urlParams.toString()}`
                : window.location.pathname;

        window.history.replaceState({ path: newUrl }, "", newUrl);

        // --- Gọi lại hàm chung để load & hiển thị sản phẩm ---
        // Container của trang products.html là #product-list
        await loadProducts("product-list", filters);

    } catch (err) {
        console.error("❌ Lỗi fetchProductsByFilters:", err);
        const container = document.getElementById("product-list");
        if (container) {
            container.innerHTML =
                `<p class="error-message">Lỗi khi tải sản phẩm. Vui lòng thử lại.</p>`;
        }
    }
}


function openSidebar() {
    if (categorySidebarGlobal) categorySidebarGlobal.classList.add('sidebar-visible');
    if (sidebarOverlayGlobal) sidebarOverlayGlobal.style.display = 'block';
    document.body.classList.add('sidebar-open');
}

function closeSidebar() {
    if (categorySidebarGlobal) categorySidebarGlobal.classList.remove('sidebar-visible');
    if (sidebarOverlayGlobal) sidebarOverlayGlobal.style.display = 'none';
    document.body.classList.remove('sidebar-open');
}

// categoryMap: bạn đã set khi gọi loadCategoriesAndBuildMap()
function populateCategorySidebar() {
    const sidebarList = document.getElementById('category-sidebar-list');
    if (!sidebarList || !window.categoryMap) return;

    sidebarList.innerHTML = '';

    // categoryMap dạng { id: { id, name, ... } }
    Object.values(categoryMap).forEach(cat => {
        const li = document.createElement('li');
        const a = document.createElement('a');

        // 👉 Chuyển sang products.html và truyền categoryId trên query
        a.href = `products.html?categoryId=${encodeURIComponent(cat.id)}`;
        a.textContent = cat.name;
        a.classList.add('category-sidebar-link');

        li.appendChild(a);
        sidebarList.appendChild(li);
    });
}
function setActiveNavLink() {
    const currentPage =
        window.location.pathname.split("/").pop() || "index.html";

    const links = document.querySelectorAll(
        "#main-nav a, #main-nav .nav-notification-container > a"
    );

    links.forEach((link) => {
        // bỏ active mặc định
        link.classList.remove("nav-active");

        let linkPage =
            (link.getAttribute("href") || "").split("/").pop() ||
            "index.html";

        // riêng chuông thông báo → map sang notifications.html
        if (
            link.id === "notification-bell-link" &&
            currentPage === "notifications.html"
        ) {
            linkPage = "notifications.html";
        }

        // element bao chuông (có thể KHÔNG tồn tại trên 1 số trang)
        const notifWrapper =
            link.id === "notification-bell-link"
                ? link.closest(".nav-notification-container")
                : null;

        if (linkPage === currentPage) {
            link.classList.add("nav-active");
            if (notifWrapper) notifWrapper.classList.add("nav-active");
        } else {
            if (notifWrapper) notifWrapper.classList.remove("nav-active");
        }
    });
}
// Áp dụng bộ lọc + load sản phẩm cho products.html
function applyFiltersAndLoad(page = 0) {
    const form = document.getElementById('filterForm');
    if (!form) {
        // Nếu không có form (vd: trang chủ) thì chỉ load theo trang
        loadProducts('product-grid-all', { page });
        return;
    }

    // --- LẤY TỪ KHOÁ TÊN SẢN PHẨM ---
    // Dù bạn đặt name là q / productName / name / searchName thì đều lấy được
    let keyword = '';
    if (form.q) {
        keyword = form.q.value.trim();
    } else if (form.productName) {
        keyword = form.productName.value.trim();
    } else if (form.name) {
        keyword = form.name.value.trim();
    } else if (form.searchName) {
        keyword = form.searchName.value.trim();
    }

    // --- TẠO OBJECT FILTERS GỬI LÊN API ---
    const filters = {
        q: keyword,                                           // 👈 param đúng như API của bạn
        minPrice: form.minPrice ? form.minPrice.value : '',
        maxPrice: form.maxPrice ? form.maxPrice.value : '',
        categoryId: form.categoryId ? form.categoryId.value : '',
        sort: form.sort ? form.sort.value : '',
        page: page,
        size: 12                                              // số sản phẩm mỗi trang
    };

    // Xoá các field rỗng
    Object.keys(filters).forEach(key => {
        if (filters[key] === null || filters[key] === undefined || filters[key] === '') {
            delete filters[key];
        }
    });
    filters.page = page; // đảm bảo luôn có page

    // Cập nhật query trên URL (để F5 / share link vẫn giữ filter)
    const urlParams = new URLSearchParams();
    Object.keys(filters).forEach(key => urlParams.append(key, filters[key]));
    window.history.replaceState(
        { path: `${window.location.pathname}?${urlParams.toString()}` },
        '',
        `${window.location.pathname}?${urlParams.toString()}`
    );

    // Gọi API /api/v1/products với đúng q, minPrice, maxPrice...
    loadProducts('product-grid-all', filters);
}


// Thêm hàm này để hiển thị phân trang
function renderPagination(paginationData, containerId = 'pagination-controls') {
    const controlsContainer = document.getElementById(containerId);
    if (!controlsContainer) return;

    const { totalPages, number: currentPage, first, last } = paginationData;
    controlsContainer.innerHTML = ''; // Xóa phân trang cũ

    if (totalPages <= 1) return; // Không cần phân trang nếu chỉ có 1 trang

    const ul = document.createElement('ul');
    ul.className = 'pagination';

    // Hàm tạo 1 nút
    const createPageItem = (text, pageNum, isDisabled = false, isActive = false) => {
        const li = document.createElement('li');
        li.className = `page-item ${isDisabled ? 'disabled' : ''} ${isActive ? 'active' : ''}`;
        const a = document.createElement('a');
        a.className = 'page-link';
        a.href = '#';
        a.innerHTML = text; // Dùng innerHTML để có thể thêm icon
        if (!isDisabled) {
            a.dataset.page = pageNum;
        }
        li.appendChild(a);
        return li;
    };

    // Nút Trước
    ul.appendChild(createPageItem('&laquo;', currentPage - 1, first));

    // Hiển thị các trang (logic đơn giản: hiện 5 trang quanh trang hiện tại)
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);

    if (startPage > 0) {
        ul.appendChild(createPageItem('1', 0));
        if (startPage > 1) {
            ul.appendChild(createPageItem('...', -1, true)); // Nút ...
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        ul.appendChild(createPageItem(i + 1, i, false, i === currentPage));
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            ul.appendChild(createPageItem('...', -1, true));
        }
        ul.appendChild(createPageItem(totalPages, totalPages - 1));
    }

    // Nút Sau
    ul.appendChild(createPageItem('&raquo;', currentPage + 1, last));

    controlsContainer.appendChild(ul);

    // Thêm listener (chỉ 1 lần cho container)
    if (!controlsContainer.dataset.listenerAttached) {
        controlsContainer.addEventListener('click', (e) => {
            e.preventDefault();
            const link = e.target.closest('.page-link');
            if (link && !link.closest('.disabled') && !link.closest('.active')) {
                const page = link.dataset.page;
                if (page) {
                    applyFiltersAndLoad(parseInt(page, 10));
                }
            }
        });
        controlsContainer.dataset.listenerAttached = 'true';
    }
}

// --- Products Listing ---
// ===================================================================
// LOAD SẢN PHẨM DÙNG CHUNG CHO index.html, products.html, v.v.
// ===================================================================
async function loadProducts(containerId, filters = {}) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<p>Đang tải sản phẩm...</p>';

    const paginationContainer = document.getElementById('pagination-controls');
    if (paginationContainer) {
        paginationContainer.innerHTML = ''; // Xóa phân trang khi tải lại
    }

    const params = new URLSearchParams();

    // Thêm filters vào params
    Object.keys(filters).forEach(key => {
        if (filters[key] !== null && filters[key] !== undefined && filters[key] !== '') {
            params.append(key, filters[key]);
        }
    });

    // Nếu không có page/size, đặt mặc định
    if (!params.has('page')) params.append('page', 0);
    if (!params.has('size')) params.append('size', 12);

    const endpoint = `/products?${params.toString()}`;
    console.log("Calling API:", PRODUCT_API_BASE_URL + endpoint);

    // Nếu đã đăng nhập thì gửi token, chưa thì gọi public
    let result = await callApi(
        PRODUCT_API_BASE_URL,
        endpoint,
        'GET',
        null,
        isLoggedIn()
    );

    if (result.ok && result.data && Array.isArray(result.data.content)) {
        const products = result.data.content;

        if (products.length > 0) {
            container.innerHTML = '';

            products.forEach(p => {
                const card = document.createElement('div');
                card.className = 'product-card-v2';

                let imgUrl = `https://placehold.co/300x200/EFEFEF/AAAAAA&text=${encodeURIComponent(p.name || 'SP')}`;
                const imageUrlFromApi = p.imageUrl;

                if (imageUrlFromApi) {
                    if (imageUrlFromApi.startsWith('http://') || imageUrlFromApi.startsWith('https://')) {
                        imgUrl = imageUrlFromApi.replace('http://localhost:8081', PRODUCT_IMAGE_BASE_URL);
                    } else if (imageUrlFromApi.startsWith('/')) {
                        imgUrl = `${PRODUCT_IMAGE_BASE_URL}${imageUrlFromApi}`;
                    } else {
                        imgUrl = `${PRODUCT_IMAGE_BASE_URL.replace(/\/$/, '')}/product-images/${imageUrlFromApi}`;
                    }
                }

                card.innerHTML = `
                    <div class="img-wrap" onclick="window.location.href='product-detail.html?id=${p.id}'" style="cursor:pointer;">
                        <img src="${imgUrl}" onerror="this.onerror=null;this.src='https://placehold.co/300x200/EFEFEF/AAAAAA&text=Ảnh lỗi';">
                    </div>
                    <div class="card-body" onclick="window.location.href='product-detail.html?id=${p.id}'" style="cursor:pointer;">
                        <h3 class="card-title">${p.name || 'Tên SP không rõ'}</h3>
                        <div class="card-price">${(parseFloat(p.price) || 0).toLocaleString('vi-VN')} đ</div>
                    </div>
                    <div class="card-footer">
                        <button class="btn-add btn-add-to-cart" data-product-id="${p.id}" onclick="event.stopPropagation(); addToCartAPI(${p.id})">
                            <i class="fas fa-cart-plus"></i> Thêm vào giỏ
                        </button>
                    </div>
                `;

                container.appendChild(card);
            });

            // Hiển thị phân trang nếu backend trả về
            if (result.data.totalPages !== undefined) {
                renderPagination(result.data);
            }
        } else {
            container.innerHTML = `<p>Không tìm thấy sản phẩm nào phù hợp với bộ lọc.</p>`;
        }
    } else {
        container.innerHTML = `
            <p class="error-message">
                Lỗi tải sản phẩm: ${result.data?.message || result.error || `Server error with status ${result.status}`}
            </p>`;
    }
}

function renderPagination(pageData) {
    const paginationContainer = document.getElementById('pagination-controls');
    if (!paginationContainer) return;

    const { number, totalPages, first, last } = pageData;
    if (totalPages <= 1) {
        paginationContainer.innerHTML = '';
        return;
    }

    let html = `<div style="display:flex; justify-content:center; gap:8px; margin-top:30px; margin-bottom: 20px;">`;

    if (!first) {
        html += `<button onclick="fetchProductsByFilters(${number - 1})" class="btn" style="padding:8px 16px; border:1px solid #ddd; background:#fff; cursor:pointer; border-radius:6px; transition:0.2s;">&laquo; Lùi</button>`;
    }

    let startPage = Math.max(0, number - 2);
    let endPage = Math.min(totalPages - 1, number + 2);

    if (startPage > 0) {
        html += `<button onclick="fetchProductsByFilters(0)" class="btn" style="padding:8px 16px; border:1px solid #ddd; background:#fff; cursor:pointer; border-radius:6px; transition:0.2s;">1</button>`;
        if (startPage > 1) html += `<span style="padding:8px 16px; color:#888;">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        if (i === number) {
            html += `<button class="btn" style="padding:8px 16px; border:1px solid #6366f1; background:#6366f1; color:#fff; cursor:default; border-radius:6px;">${i + 1}</button>`;
        } else {
            html += `<button onclick="fetchProductsByFilters(${i})" class="btn" style="padding:8px 16px; border:1px solid #ddd; background:#fff; cursor:pointer; border-radius:6px; transition:0.2s;">${i + 1}</button>`;
        }
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) html += `<span style="padding:8px 16px; color:#888;">...</span>`;
        html += `<button onclick="fetchProductsByFilters(${totalPages - 1})" class="btn" style="padding:8px 16px; border:1px solid #ddd; background:#fff; cursor:pointer; border-radius:6px; transition:0.2s;">${totalPages}</button>`;
    }

    if (!last) {
        html += `<button onclick="fetchProductsByFilters(${number + 1})" class="btn" style="padding:8px 16px; border:1px solid #ddd; background:#fff; cursor:pointer; border-radius:6px; transition:0.2s;">Tiến &raquo;</button>`;
    }

    html += `</div>`;
    paginationContainer.innerHTML = html;
}

// === Guest Recommendations ===
async function fetchGuestRecommendations(limit = 12) {
    try {
        const res = await callApi(
            PRODUCT_API_BASE_URL,
            `/recommendations/guest?limit=${limit}`,
            "GET",
            null,
            false
        );
        if (!res.ok) return [];
        return Array.isArray(res.data?.result) ? res.data.result : [];
    } catch (err) {
        console.error("❌ Lỗi fetchGuestRecommendations:", err);
        return [];
    }
}

function renderGuestRecommendations(products) {
    const container = document.getElementById("guest-recommend-products");
    if (!container) return;

    if (!products.length) {
        container.innerHTML = "<p></p>";
        return;
    }

    container.innerHTML = products.map(p => `
            <div class="product-card">
                <img src="${PRODUCT_IMAGE_BASE_URL}${p.imageUrl}" alt="${p.name}">
                <h4>${p.name}</h4>
                <p class="price">${p.price.toLocaleString("vi-VN")} đ</p>
                <a href="product-detail.html?id=${p.id}" class="btn btn-primary btn-sm">Xem chi tiết</a>
            </div>
        `).join("");
    const cards = container.querySelectorAll(".product-card");
    cards.forEach((card, i) => {
        setTimeout(() => card.classList.add("card-show"), i * 30);
    });
}
// ✅ Render danh sách sản phẩm vào 1 grid bất kỳ theo containerId
function renderProductsToGrid(containerId, products) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!products || products.length === 0) {
        container.innerHTML = "<p>Không có sản phẩm.</p>";
        return;
    }

    container.innerHTML = products.map(p => {
        let img = "https://placehold.co/300x200?text=No+Image";
        if (p.imageUrl) {
            if (p.imageUrl.startsWith("http")) img = p.imageUrl;
            else if (p.imageUrl.startsWith("/")) img = `http://localhost:8081${p.imageUrl}`;
            else img = `http://localhost:8081/files/${p.imageUrl}`;
        }

        return `
      <div class="product-card">
        <a href="product-detail.html?id=${p.id}">
          <img src="${img}" alt="${p.name || "Sản phẩm"}"
               onerror="this.onerror=null;this.src='https://placehold.co/300x200?text=Image+Error';">
        </a>
        <h4>${p.name || "Không tên"}</h4>
        <p class="price">${Number(p.price || 0).toLocaleString("vi-VN")} đ</p>
        <a href="product-detail.html?id=${p.id}" class="btn btn-primary btn-sm">Xem chi tiết</a>
      </div>
    `;
    }).join("");
    const cards = container.querySelectorAll(".product-card");
    cards.forEach((card, i) => {
        setTimeout(() => card.classList.add("card-show"), i * 30);
    });
}

// ✅ Render danh sách sản phẩm vào 1 grid bất kỳ theo containerId
function renderProductsToGrid(containerId, products) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!products || !products.length) {
        container.innerHTML = "<p>Không có sản phẩm gợi ý.</p>";
        return;
    }

    container.innerHTML = products.map(p => `
        <div class="product-card">
        <img src="${PRODUCT_IMAGE_BASE_URL}${p.imageUrl}" alt="${p.name}">
        <h4>${p.name}</h4>
        <p class="price">${(p.price || 0).toLocaleString("vi-VN")} đ</p>
        <a href="product-detail.html?id=${p.id}" class="btn btn-primary btn-sm">Xem chi tiết</a>
        </div>
    `).join("");
}

// === User Recommendations (đã đăng nhập) ===
async function fetchUserRecommendations(limit = 12) {
    try {
        const res = await callApi(
            PRODUCT_API_BASE_URL,
            `/recommendations/me?limit=${limit}`,
            "GET",
            null,
            true // cần token
        );

        if (!res.ok) return [];

        // ✅ API của bạn trả { code, message, result: [] }
        return Array.isArray(res.data?.result) ? res.data.result : [];
    } catch (err) {
        console.error("❌ Lỗi fetchUserRecommendations:", err);
        return [];
    }
}
async function loadHomeRecommendations(limit = 12) {
    const container = document.getElementById("guest-recommend-products");
    if (!container) return;

    try {
        // Yêu cầu: Gợi ý sản phẩm dùng API /recommendations/me
        // Thêm requiresAuth = false (để nó gửi request cho mọi trường hợp, có token hay ko)
        const result = await callApi(
            PRODUCT_API_BASE_URL,
            `/recommendations/me?limit=${limit}`,
            "GET",
            null,
            isLoggedIn()
        );

        const products = (result.ok && result.data) ? (result.data.result || []) : [];

        // Nếu API này trả về rỗng do chưa đăng nhập, tự động lấy sản phẩm mới nhất làm fallback?
        // Thiết kế này tôn trọng API yêu cầu của người dùng, nếu không có dữ liệu ta hiện tin nhắn.
        if (!products.length) {
            container.innerHTML = `<p style="color:#777; padding:10px;">Bạn cần mua thêm hoặc đánh giá sản phẩm để hệ thống gợi ý món đồ phù hợp!</p>`;
            return;
        }

        // Tái sử dụng product-card-v2 style như layout chính
        container.innerHTML = products.map(p => {
            let imgUrl = `https://placehold.co/300x200/EFEFEF/AAAAAA&text=${encodeURIComponent(p.name || 'SP')}`;
            if (p.imageUrl) {
                if (p.imageUrl.startsWith('http://') || p.imageUrl.startsWith('https://')) {
                    imgUrl = p.imageUrl.replace('http://localhost:8081', PRODUCT_IMAGE_BASE_URL);
                } else if (p.imageUrl.startsWith('/')) {
                    imgUrl = `${PRODUCT_IMAGE_BASE_URL}${p.imageUrl}`;
                } else {
                    let base = PRODUCT_IMAGE_BASE_URL.endsWith('/') ? PRODUCT_IMAGE_BASE_URL.slice(0, -1) : PRODUCT_IMAGE_BASE_URL;
                    imgUrl = `${base}/product-images/${p.imageUrl}`;
                }
            }
            return `
            <div class="product-card-v2" onclick="window.location.href='product-detail.html?id=${p.id}'" style="cursor:pointer;">
                <div class="img-wrap">
                    <img src="${imgUrl}" onerror="this.onerror=null;this.src='https://placehold.co/300x200/EFEFEF/AAAAAA&text=Ảnh lỗi';">
                </div>
                <div class="card-body">
                    <h3 class="card-title">${p.name}</h3>
                    <div class="card-price">${(parseFloat(p.price) || 0).toLocaleString('vi-VN')} đ</div>
                </div>
                <div class="card-footer">
                    <button class="btn-add btn-add-to-cart" data-product-id="${p.id}" onclick="event.stopPropagation(); addToCartAPI(${p.id})">
                        <i class="fas fa-cart-plus"></i> Thêm vào giỏ
                    </button>
                </div>
            </div>`;
        }).join("");
    } catch (err) {
        console.error("❌ Lỗi loadHomeRecommendations:", err);
        container.innerHTML = `<p class="error-message">Không thể tải danh sách gợi ý.</p>`;
    }
}

// === Cart Management via API ===
function updateCartUI(cartDataObject) {
    console.log("updateCartUI: Nhận được cartDataObject:", JSON.stringify(cartDataObject, null, 2));

    // Mặc định là giỏ hàng trống nếu cartDataObject là null hoặc không có items
    const itemsForCount = cartDataObject?.items || [];
    const totalDisplayItems = itemsForCount.reduce((sum, item) => {
        const quantity = parseInt(item.quantity, 10); // Đảm bảo quantity là số
        return sum + (isNaN(quantity) ? 0 : quantity);
    }, 0);

    // Cập nhật số lượng trên tất cả các span.cart-item-count-nav
    document.querySelectorAll('span.cart-item-count-nav').forEach(el => {
        if (el) {
            el.textContent = totalDisplayItems > 0 ? ` (${totalDisplayItems})` : '';
            el.style.display = totalDisplayItems > 0 ? 'inline' : 'none';
        }
    });

    // Cập nhật icon giỏ hàng nổi
    const floatIcon = document.getElementById('floating-cart-icon');
    const floatCount = document.getElementById('floating-cart-count');
    if (floatIcon && floatCount) {
        floatIcon.style.display = totalDisplayItems > 0 ? 'flex' : 'none'; // Giả sử dùng flex để căn giữa
        if (totalDisplayItems > 0) {
            floatCount.textContent = totalDisplayItems;
        }
    }

    // Chỉ gọi renderCartPageItemsAPI nếu đang ở trang cart.html
    // và các element cần thiết cho việc render giỏ hàng tồn tại
    if (window.location.pathname.includes('cart.html')) {
        const gridEl = document.getElementById('cart-grid');
        const summaryEl = document.getElementById('cart-summary');
        if (gridEl && summaryEl) {
            console.log("updateCartUI: Đang ở trang cart.html, gọi renderCartPageItemsAPI với cartDataObject:", JSON.stringify(cartDataObject, null, 2));
            renderCartPageItemsAPI(cartDataObject); // Truyền cartDataObject (có thể là null)
        } else {
            console.warn("updateCartUI: Không thể gọi renderCartPageItemsAPI vì #cart-grid hoặc #cart-summary không tìm thấy trên trang cart.html, mặc dù URL khớp.");
        }
    }
}
async function fetchCartData() {
    if (!isLoggedIn()) {
        updateCartUI(null); // Cập nhật UI thành giỏ hàng trống nếu chưa đăng nhập
        console.log("fetchCartData: Người dùng chưa đăng nhập, không fetch giỏ hàng.");
        return null;
    }

    console.log("fetchCartData: Đang gọi API GET /my-cart...");
    const result = await callApi(CART_API_BASE_URL, '/my-cart', 'GET', null, true);

    console.log("fetchCartData: Raw result từ /my-cart:", JSON.stringify(result, null, 2));

    // Xử lý kết quả từ API
    if (result.ok && result.data) {
        if (result.data.result) {
            // API trả về cấu trúc có trường "result" chứa dữ liệu giỏ hàng
            console.log("fetchCartData: Tải giỏ hàng thành công, truyền data.result vào updateCartUI:", result.data.result);
            updateCartUI(result.data.result);
            return result.data.result;
        } else if (result.data.code === 200 && result.data.result === null) {
            // API trả về thành công nhưng không có giỏ hàng (result là null)
            console.log("fetchCartData: Tải giỏ hàng thành công, nhưng không có dữ liệu giỏ hàng (result is null).", result.data);
            updateCartUI(null); // Coi như giỏ hàng trống
            return null;
        } else if (result.data.id && Array.isArray(result.data.items)) {
            // Trường hợp API trả về đối tượng giỏ hàng trực tiếp trong result.data (không có trường "result" lồng nhau)
            console.log("fetchCartData: Tải giỏ hàng thành công, truyền data trực tiếp vào updateCartUI (vì có items và id):", result.data);
            updateCartUI(result.data);
            return result.data;
        }
        else {
            // Trường hợp response.ok nhưng cấu trúc data không như mong đợi (không có result.data.result hoặc result.data.items)
            console.warn("fetchCartData: Tải giỏ hàng thành công nhưng cấu trúc dữ liệu không như mong đợi.", result.data);
            updateCartUI(null); // Không có dữ liệu giỏ hàng hợp lệ
            return null;
        }
    } else if (result.status === 401 || result.status === 403) {
        console.warn("fetchCartData: Lỗi xác thực khi tải giỏ hàng. Token có thể đã hết hạn. Đang đăng xuất...");
        updateCartUI(null);
        handleLogout(); // Tự động đăng xuất
        return null;
    } else {
        // Các lỗi khác (ví dụ: 500, lỗi mạng đã được callApi xử lý phần nào)
        console.error(`WorkspaceCartData: Lỗi tải giỏ hàng. Status: ${result.status}`, result.data?.message || result.error);
        updateCartUI(null);
        return null;
    }
}
async function addToCartAPI(productId, quantity = 1, showAlert = true) { /* ... Giữ nguyên logic cũ ... */
    if (!isLoggedIn()) { alert("Bạn cần đăng nhập."); window.location.href = 'login.html'; return false; }
    if (!productId || quantity <= 0) { if (showAlert) alert("Thông tin sản phẩm không hợp lệ."); return false; }
    const body = { productId: String(productId), quantity };
    const result = await callApi(CART_API_BASE_URL, '/my-cart/items', 'POST', body, true);
    if (result.ok) {
        if (showAlert) alert("Đã thêm sản phẩm vào giỏ!");
        await fetchCartData();
    } else {
        let userMessage = `Lỗi thêm vào giỏ: ${result.data?.message || result.error || 'Lỗi không xác định.'}`;
        if (result.data?.code === "2102") { // Kiểm tra mã lỗi cụ thể
            userMessage = "Lỗi thêm vào giỏ: Sản phẩm này hiện không có sẵn hoặc thông tin sản phẩm không thể được truy xuất. Vui lòng thử lại sau.";
        }
        if (showAlert) alert(userMessage);
    }
}
/**
 * Render các mục sản phẩm trong giỏ hàng và tóm tắt giỏ hàng trên trang cart.html.
 * @param {object | null} cartData Dữ liệu giỏ hàng từ API (chứa trường 'items' và 'grandTotal') hoặc null.
 */
/**
 * Render các mục sản phẩm trong giỏ hàng và tóm tắt giỏ hàng trên trang cart.html.
 * @param {object | null} cartData Dữ liệu giỏ hàng từ API (chứa trường 'items' và 'grandTotal') hoặc null.
 */
// Trong file main.js

function renderCartPageItemsAPI(cartData) {
    console.log("renderCartPageItemsAPI: Nhận data:", cartData);

    const gridEl = document.getElementById('cart-grid');
    const summaryEl = document.getElementById('cart-summary');

    if (!gridEl || !summaryEl) {
        console.warn("Không tìm thấy #cart-grid hoặc #cart-summary trên trang.");
        return;
    }

    gridEl.innerHTML = '';
    summaryEl.innerHTML = '';

    const items = cartData?.items || [];
    const totalAmount = cartData?.grandTotal || 0;

    if (!items.length) {
        gridEl.innerHTML = '<p>Giỏ hàng trống.</p>';
        summaryEl.innerHTML = '';
        return;
    }

    // ================= BẢNG GIỎ HÀNG =================
    const table = document.createElement('table');
    table.className = 'cart-table';

    const thead = document.createElement('thead');
    thead.innerHTML = `
    <tr>
        <th style="width: 40px; text-align:center;">
            <input type="checkbox" id="cart-select-all">
        </th>
        <th style="width: 120px; text-align:center;">Ảnh</th>
        <th>Sản phẩm</th>
        <th style="width: 120px; text-align:center;">Giá</th>
        <th style="width: 120px; text-align:center;">Số lượng</th>
        <th style="width: 140px; text-align:center;">Thành tiền</th>
        <th style="width: 160px; text-align:center;">Hành động</th>
    </tr>
`;
    table.appendChild(thead);

    const tbody = document.createElement('tbody');

    items.forEach((item, index) => {
        const productId = item.productId;
        const productName = item.productName || `Sản phẩm #${productId || index + 1}`;
        const quantity = item.quantity || 0;
        const productPrice = item.currentPrice || item.priceAtAddition || 0;
        const itemSubtotal = productPrice * quantity;

        // Xử lý ảnh
        let imageUrlFromApi = item.imageUrl;
        let resolvedProductImageUrl =
            `https://placehold.co/60x60/EFEFEF/AAAAAA&text=${encodeURIComponent(productName.substring(0, 10))}`;

        if (imageUrlFromApi) {
            if (imageUrlFromApi.startsWith('http://') || imageUrlFromApi.startsWith('https://')) {
                if (imageUrlFromApi.startsWith('http://localhost:8081')) {
                    resolvedProductImageUrl = imageUrlFromApi.replace('http://localhost:8081', PRODUCT_IMAGE_BASE_URL);
                } else {
                    resolvedProductImageUrl = imageUrlFromApi;
                }
            } else if (imageUrlFromApi.startsWith('/')) {
                resolvedProductImageUrl = `${PRODUCT_IMAGE_BASE_URL}${imageUrlFromApi}`;
            } else {
                resolvedProductImageUrl = `${PRODUCT_IMAGE_BASE_URL.replace(/\/$/, '')}/product-images/${imageUrlFromApi}`;
            }
        }

        if (!productId) {
            console.error(`renderCartPageItemsAPI: Item thứ ${index + 1} thiếu 'productId'. Item data:`, item);
        }

        const tr = document.createElement('tr');
        tr.className = 'cart-item-row';

        tr.innerHTML = `
    <td class="cart-item-select-cell" style="text-align:center;">
        <input type="checkbox"
               class="cart-item-select-checkbox"
               data-product-id="${productId}"
               data-price="${productPrice}">
    </td>

    <td class="cart-item-image-cell" style="text-align:center;">
        <img src="${resolvedProductImageUrl}"
             alt="${productName}"
             class="cart-item-image"
             style="width:80px; height:80px; object-fit:contain;">
    </td>

    <td class="cart-item-name-cell">${productName}</td>

    <td class="cart-item-price-cell" style="text-align:center;">
        ${productPrice.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })}
    </td>

    <td class="cart-item-quantity-cell" style="text-align:center;">
        <input type="number"
               class="cart-item-quantity-input-api"
               value="${quantity}"
               min="1">
    </td>

    <td class="cart-item-subtotal-cell" style="text-align:center;">
        ${itemSubtotal.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })}
    </td>

    <td class="cart-item-action-cell" style="text-align:center;">
        <button class="btn btn-checkout-single btn-outline-primary btn-sm" data-product-id="${productId}">
            Thanh toán món này
        </button>
        <button class="btn btn-remove-from-cart-api btn-danger btn-sm" data-product-id="${productId}">
            Xóa
        </button>
    </td>
`;

        tbody.appendChild(tr);
    });

    table.appendChild(tbody);
    gridEl.appendChild(table);

    // =============== TÓM TẮT GIỎ HÀNG (FOOTER) ===============
    summaryEl.innerHTML = `
        <div class="cart-summary-content">
            <h3>Tổng cộng đã chọn:
                <span id="cart-selected-total">0 đ</span>
            </h3>
            <button type="button" class="btn btn-checkout-selected btn-success">
                Thanh toán đã chọn
            </button>
            <button type="button" class="btn btn-checkout btn-primary">
                Thanh toán tất cả
            </button>
        </div>
    `;

    // ===== HÀM TÍNH TỔNG CÁC SẢN PHẨM ĐƯỢC TÍCH =====
    function updateSelectedTotal() {
        const totalEl = document.getElementById('cart-selected-total');
        if (!totalEl) return;

        let total = 0;

        const checkboxes = tbody.querySelectorAll('.cart-item-select-checkbox');
        checkboxes.forEach(cb => {
            const row = cb.closest('tr');
            const qtyInput = row ? row.querySelector('.cart-item-quantity-input-api') : null;
            const quantity = qtyInput ? parseInt(qtyInput.value, 10) || 0 : 0;
            const price = parseFloat(cb.dataset.price) || 0;

            if (cb.checked && quantity > 0) {
                total += price * quantity;
            }
        });

        totalEl.textContent = total.toLocaleString('vi-VN', {
            style: 'currency',
            currency: 'VND'
        });
    }

    // ===== SỰ KIỆN CHỌN TẤT CẢ =====
    const selectAllCheckbox = table.querySelector('#cart-select-all');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', () => {
            const itemCheckboxes = tbody.querySelectorAll('.cart-item-select-checkbox');
            itemCheckboxes.forEach(cb => {
                cb.checked = selectAllCheckbox.checked;
            });
            updateSelectedTotal();
        });
    }

    // ===== SỰ KIỆN TỪNG CHECKBOX =====
    tbody.querySelectorAll('.cart-item-select-checkbox').forEach(cb => {
        cb.addEventListener('change', () => {
            if (selectAllCheckbox) {
                const allChecked = [...tbody.querySelectorAll('.cart-item-select-checkbox')]
                    .every(x => x.checked);
                selectAllCheckbox.checked = allChecked;
            }
            updateSelectedTotal();
        });
    });

    // ===== SỰ KIỆN ĐỔI SỐ LƯỢNG (CẬP NHẬT THÀNH TIỀN + TỔNG) =====
    tbody.querySelectorAll('.cart-item-quantity-input-api').forEach(input => {
        input.addEventListener('change', () => {
            const row = input.closest('tr');
            const checkbox = row ? row.querySelector('.cart-item-select-checkbox') : null;
            const price = checkbox ? parseFloat(checkbox.dataset.price) || 0 : 0;
            const quantity = parseInt(input.value, 10) || 0;

            const subtotal = price * quantity;
            const subtotalCell = row ? row.querySelector('.cart-item-subtotal-cell') : null;
            if (subtotalCell) {
                subtotalCell.textContent = subtotal.toLocaleString('vi-VN', {
                    style: 'currency',
                    currency: 'VND'
                });
            }

            updateSelectedTotal();
        });
    });

    // Khởi tạo tổng ban đầu (nếu muốn lúc đầu chưa tick thì = 0)
    updateSelectedTotal();

    console.log("renderCartPageItemsAPI: Đã render xong bảng sản phẩm và tóm tắt giỏ hàng.");
}
async function updateCartItemQuantityAPI(productId, newQuantityStr) {
    if (!isLoggedIn() || !productId) {
        alert("Bạn cần đăng nhập hoặc ID sản phẩm không hợp lệ.");
        return;
    }
    const newQuantity = parseInt(newQuantityStr, 10);
    if (isNaN(newQuantity) || newQuantity < 0) {
        alert("Số lượng không hợp lệ.");
        await fetchCartData();
        return;
    }
    if (newQuantity === 0) {
        await removeCartItemAPI(productId, false);
        return;
    }
    const endpoint = `/my-cart/items/${productId}?quantity=${newQuantity}`; // Gửi quantity trong query parameter
    const result = await callApi(CART_API_BASE_URL, endpoint, 'PUT', null, true);
    if (result.ok) {
        await fetchCartData();
    } else {
        alert(`Lỗi cập nhật số lượng: ${result.data?.message || result.error}`);
        await fetchCartData();
    }
}
async function removeCartItemAPI(productId, confirmDelete = true) {
    if (!isLoggedIn() || !productId) {
        alert("Bạn cần đăng nhập hoặc ID sản phẩm không hợp lệ.");
        return;
    }
    if (confirmDelete && !confirm('Xóa sản phẩm này khỏi giỏ?')) {
        return;
    }
    const result = await callApi(CART_API_BASE_URL, `/my-cart/items/${productId}`, 'DELETE', null, true);
    if (result.ok) {
        await fetchCartData();
    } else {
        alert(`Lỗi xóa sản phẩm: ${result.data?.message || result.error}`);
    }
}

// ================== PRODUCT DETAIL PAGE ==================
// --- Product Detail Page ---
async function loadProductDetail() {
    const contentEl = document.getElementById("product-detail-content");
    if (!contentEl) return;

    const productIdFromUrl = new URLSearchParams(window.location.search).get("id");
    if (!productIdFromUrl) {
        contentEl.innerHTML = '<p class="error-message">ID sản phẩm không hợp lệ.</p>';
        return;
    }

    console.log("🔥 loadProductDetail START");
    console.log("👉 productId từ URL =", productIdFromUrl);

    contentEl.innerHTML = "<p>Đang tải...</p>";

    if (categoryMap.size === 0) {
        await loadCategoriesAndBuildMap();
    }

    const apiPath = `/products/${productIdFromUrl}`;
    const result = await callApi(PRODUCT_API_BASE_URL, apiPath, "GET", null, isLoggedIn());

    if (!result.ok || !result.data) {
        console.warn("❌ Lỗi khi load chi tiết sản phẩm:", result);
        contentEl.innerHTML = `<p class="error-message">
            Lỗi tải chi tiết sản phẩm: ${result.data?.message || result.error || "Không tìm thấy sản phẩm"}.
            <a href="products.html">Quay lại danh sách sản phẩm</a>.
        </p>`;
        return;
    }

    const p = result.data.result || result.data;
    console.log("🧩 Product object:", p);

    const realProductId = p.id || Number(productIdFromUrl);
    console.log("👉 realProductId =", realProductId);

    const currentUserRole = getUserRole();
    const currentUserId = localStorage.getItem("currentUserId");
    console.log("🔐 currentUserRole =", currentUserRole);
    console.log("🔐 currentUserId =", currentUserId);

    const sellerInfo = p.sellerInfo || {};
    console.log("🧑‍💼 sellerInfo.userId =", sellerInfo.userId);

    document.title = `${p.name || "Sản phẩm"} - HyperBuy`;

    // --------- Xử lý image URL ----------
    let imgUrl =
        p.imageUrl ||
        `https://placehold.co/400x300/EFEFEF/AAAAAA&text=${encodeURIComponent(
            p.name || "SP"
        )}`;

    if (imgUrl.startsWith("http://productservice")) {
        imgUrl = imgUrl.replace(/^http:\/\/productservice:\d+/, PRODUCT_IMAGE_BASE_URL);
    } else if (imgUrl.startsWith("http://localhost:8081")) {
        imgUrl = imgUrl.replace("http://localhost:8081", PRODUCT_IMAGE_BASE_URL);
    } else if (
        !imgUrl.startsWith("http://") &&
        !imgUrl.startsWith("https://") &&
        imgUrl.includes("/")
    ) {
        imgUrl = `${PRODUCT_IMAGE_BASE_URL}${imgUrl}`;
    } else if (
        !imgUrl.startsWith("http://") &&
        !imgUrl.startsWith("https://") &&
        !imgUrl.includes("/")
    ) {
        imgUrl = `${PRODUCT_IMAGE_BASE_URL}/product-images/${imgUrl}`;
    }

    const pName = p.name || "Tên SP không rõ";
    const pPriceNum = parseFloat(p.price) || 0;
    const productCategoryName =
        p.category?.name ||
        (p.categoryId && categoryMap.get(String(p.categoryId))) ||
        "Chưa phân loại";

    // --------- Mô tả & thông số kỹ thuật ----------
    let mainDesc = p.description || "Chưa có mô tả.";
    let techSpecsHtml = "";

    const descLines = mainDesc.split("\n");
    const specsArr = [];
    const generalDescLines = [];

    descLines.forEach((line) => {
        if (line.includes(":") && line.length < 100 && line.length > 3) {
            specsArr.push(line);
        } else {
            generalDescLines.push(line);
        }
    });

    mainDesc = generalDescLines.join("<br>");

    if (specsArr.length > 0) {
        techSpecsHtml =
            '<div class="tech-specs"><h3>Thông số kỹ thuật</h3><div class="tech-specs-columns">';
        specsArr.forEach((spec) => {
            const parts = spec.split(":");
            const label = parts[0]?.trim();
            const value =
                parts.slice(1).join(":")?.trim() || (label ? "" : spec);
            if (label) {
                techSpecsHtml += `<div class="spec-item"><strong>${label}:</strong> ${value}</div>`;
            } else if (value) {
                techSpecsHtml += `<div class="spec-item spec-value-only">${value}</div>`;
            }
        });
        techSpecsHtml += "</div></div>";
    }

    // --------- Thông tin seller + nút xem cửa hàng ----------
    let sellerInfoHtml = "";
    if (sellerInfo && sellerInfo.userId) {
        const sellerDisplayName = sellerInfo.username || "N/A";
        const sellerIdForButton = sellerInfo.userId;

        sellerInfoHtml = `
        <div class="seller-info-section" style="margin-top: 15px; padding: 10px; background-color: #f9f9f9; border-radius: 5px;">
        <p>Được bán bởi: <strong>${sellerDisplayName}</strong></p>

        <a href="store.html?sellerId=${encodeURIComponent(sellerIdForButton)}"
            class="btn btn-secondary btn-sm">
            🏪 Xem Chi Tiết Cửa Hàng
        </a>
        </div>`;
    }
    // --------- Seller được phép quản lý sản phẩm? ----------
    let productManagementControls = "";
    const isSameSeller =
        currentUserRole === "SELLER" &&
        sellerInfo &&
        sellerInfo.userId &&
        currentUserId &&
        sellerInfo.userId === currentUserId;

    if (isSameSeller) {
        const isActive = p.active !== false;
        const statusText = isActive
            ? `<span style="color:green;font-weight:bold;">Đang bán</span>`
            : `<span style="color:orange;font-weight:bold;">Ngưng bán</span>`;

        productManagementControls = `
        <div class="seller-product-controls"
             style="margin-top:20px;padding-top:15px;border-top:1px solid #ddd;">
             
            <h4>Quản lý sản phẩm của bạn</h4>
            <p>Trạng thái: ${statusText}</p>

            <a href="edit-product.html?id=${realProductId}" 
               class="btn btn-info btn-sm" style="margin-right:8px;">
                ✏️ Sửa sản phẩm
            </a>

            <button class="btn ${isActive ? "btn-warning" : "btn-success"} btn-sm seller-toggle-status"
                    data-product-id="${realProductId}"
                    data-store-id="${sellerInfo.storeId}">
                ${isActive ? "⛔ Ngưng bán" : "🛒 Bán lại"}
            </button>

            <button class="btn btn-danger btn-sm seller-delete-product"
                    data-product-id="${realProductId}"
                    data-store-id="${sellerInfo.storeId}">
                🗑️ Xóa sản phẩm
            </button>
        </div>
    `;
    }


    // --------- Nút Thêm vào giỏ / Mua ngay ----------
    const productActionsHtml = p.active
        ? `
            <button class="btn btn-primary btn-add-to-cart" data-product-id="${realProductId}">Thêm vào giỏ</button>
            <button class="btn btn-success btn-buy-now" data-product-id="${realProductId}" style="margin-left: 10px;">Mua ngay</button>
        `
        : '<p class="status-inactive" style="color: orange; font-weight: bold; margin-top:15px;">Sản phẩm này hiện đang ngưng bán.</p>';

    // --------- Render HTML ----------
    contentEl.innerHTML = `
        <div class="product-detail-container">
            <div class="product-detail-image">
                <img src="${imgUrl}" alt="${encodeURIComponent(pName)}">
            </div>
            <div class="product-detail-info">
                <h2>${pName}</h2>
                <p class="price">
                    ${pPriceNum.toLocaleString("vi-VN", {
        style: "currency",
        currency: "VND",
    })}
                </p>
                <p class="sku">SKU: ${p.sku || "N/A"}</p>
                <p class="category">Danh mục: ${productCategoryName}</p>

                ${sellerInfoHtml}

                <!-- ⭐ Tóm tắt đánh giá -->
                <div id="product-rating-summary" class="product-rating-summary"></div>

                <div class="product-actions">
                    ${productActionsHtml}
                </div>

                <div class="description">
                    <h3>Mô tả chung</h3>
                    <p>${mainDesc}</p>
                </div>

                ${techSpecsHtml}
                ${productManagementControls}
            </div>
        </div>
    `;
    requestAnimationFrame(() => {
        contentEl.classList.add("detail-show");
    });

    // --------- Gắn sự kiện seller (activate / deactivate / delete) ----------
    attachSellerProductEvents(realProductId);

    console.log("✅ Đã render xong product detail, init rating UI...");
    await initProductRatingUI(realProductId);
}
async function loadStorePage() {
    const storeId = new URLSearchParams(window.location.search).get("storeId");

    const infoEl = document.getElementById("store-info");
    const gridEl = document.getElementById("store-products-grid");

    if (!storeId) {
        if (infoEl) infoEl.innerHTML = `<div class="card"><p class="error-message">Thiếu storeId trên URL.</p></div>`;
        if (gridEl) gridEl.innerHTML = `<p>Không có dữ liệu.</p>`;
        return;
    }

    // 1) (Tuỳ chọn) Load thông tin store (nếu bạn có API)
    // Nếu bạn chưa có API store thì giữ try/catch này để không crash.
    if (infoEl) infoEl.innerHTML = `<div class="card"><p>Đang tải thông tin cửa hàng...</p></div>`;

    try {
        // 👉 Nếu bạn có endpoint store thật sự, sửa đúng baseURL/endpoint tại đây
        // Ví dụ (tự chỉnh): const storeRes = await callApi(USER_API_BASE_URL, `/stores/${storeId}`, "GET", null, false);
        // Tạm thời: bỏ qua nếu chưa có
        if (infoEl) infoEl.innerHTML = `<div class="card"><p><b>Cửa hàng</b>: ${storeId}</p></div>`;
    } catch (e) {
        if (infoEl) infoEl.innerHTML = `<div class="card"><p class="error-message">Lỗi tải thông tin cửa hàng.</p></div>`;
    }

    // 2) Load sản phẩm của cửa hàng (quan trọng) ✅
    if (gridEl) gridEl.innerHTML = `<p>Đang tải sản phẩm của cửa hàng...</p>`;

    // ✅ Đây là chỗ bạn đang bị sai URL => phải là /products/store/{storeId}
    const endpoint = `/products/store/${storeId}?page=0&size=12`;

    const res = await callApi(PRODUCT_API_BASE_URL, endpoint, "GET", null, false);

    if (!res.ok) {
        console.log("STORE PRODUCTS ERROR:", res);
        if (gridEl) gridEl.innerHTML = `<p class="error-message">Không tải được sản phẩm của cửa hàng (404 thường là sai endpoint).</p>`;
        return;
    }

    // Hỗ trợ cả 2 kiểu response: result là array hoặc paging
    const payload = res.data?.result ?? res.data;
    const items =
        Array.isArray(payload) ? payload :
            Array.isArray(payload?.content) ? payload.content :
                Array.isArray(payload?.items) ? payload.items :
                    [];

    if (!items.length) {
        if (gridEl) gridEl.innerHTML = `<p>Chưa có sản phẩm nào.</p>`;
        return;
    }

    // Render
    const toVND = (n) => (Number(n) || 0).toLocaleString("vi-VN", { style: "currency", currency: "VND" });

    gridEl.innerHTML = items.map(p => {
        const id = p.id;
        const name = p.name || "Sản phẩm";
        const price = toVND(p.price);

        let img = p.imageUrl || "";
        if (img.startsWith("http://productservice")) img = img.replace(/^http:\/\/productservice:\d+/, PRODUCT_IMAGE_BASE_URL);
        else if (img.startsWith("http://localhost:8081")) img = img.replace("http://localhost:8081", PRODUCT_IMAGE_BASE_URL);
        else if (img && !img.startsWith("http")) img = `${PRODUCT_IMAGE_BASE_URL}${img.startsWith("/") ? "" : "/"}${img}`;

        if (!img) img = `https://placehold.co/400x300?text=${encodeURIComponent(name)}`;

        return `
      <div class="product-card">
        <a href="product-detail.html?id=${id}" class="product-link">
          <img class="product-image" src="${img}" alt="${name}">
          <div class="product-info">
            <h4 class="product-name">${name}</h4>
            <div class="product-price">${price}</div>
          </div>
        </a>
      </div>
    `;
    }).join("");
}

function attachSellerProductEvents() {

    // Toggle active / deactivate
    document.querySelectorAll(".seller-toggle-status").forEach((btn) => {
        btn.addEventListener("click", async () => {

            const productId = btn.dataset.productId;
            const storeId = btn.dataset.storeId;

            if (!productId || !storeId) {
                alert("Thiếu productId hoặc storeId!");
                return;
            }

            const isDeactivate = btn.textContent.includes("Ngưng bán");
            const confirmMsg = isDeactivate
                ? "Bạn có chắc muốn NGƯNG BÁN sản phẩm này?"
                : "Bạn có muốn BÁN LẠI sản phẩm này?";

            if (!confirm(confirmMsg)) return;

            const endpoint = isDeactivate
                ? `/products/${productId}/deactivate`
                : `/products/${productId}/activate`;

            const storeIdHeader = localStorage.getItem("userStoreId");

            const res = await callApi(
                PRODUCT_API_BASE_URL,
                endpoint,
                "PATCH",
                null,
                true,
                false,
                { "X-Store-Id": storeId }
            );

            if (res.ok) {
                alert("Cập nhật trạng thái thành công!");
                loadProductDetail();
            } else {
                alert("Lỗi cập nhật: " + (res.data?.message || res.error));
            }
        });
    });

    // Xóa sản phẩm
    document.querySelectorAll(".seller-delete-product").forEach((btn) => {
        btn.addEventListener("click", async () => {
            if (!confirm("Bạn có chắc chắn muốn XÓA sản phẩm này không?")) return;

            const storeId = localStorage.getItem("userStoreId");

            const res = await callApi(
                PRODUCT_API_BASE_URL,
                `/products/${productId}`,
                "DELETE",
                null,
                true,
                false,
                { "X-Store-Id": storeId }
            );

            if (res.ok || res.status === 204) {
                alert("Xóa sản phẩm thành công!");
                window.location.href = "products.html";
            } else {
                alert("Lỗi xóa: " + (res.data?.message || res.error));
            }
        });
    });
}


// ================== RATING API & UI =======================

// Lấy tóm tắt rating cho 1 sản phẩm
async function fetchProductRatingSummary(productId) {
    if (!productId) {
        console.error("❌ fetchProductRatingSummary: productId bị thiếu!", productId);
        return null;
    }

    let result;

    // Nếu đang đăng nhập thì thử gọi kèm token trước
    if (isLoggedIn()) {
        result = await callApi(
            PRODUCT_API_BASE_URL,
            `/products/${productId}/rating-summary`,
            "GET",
            null,
            true // requiresAuth = true
        );

        // Nếu token sai / hết hạn / không được phép → thử lại không kèm token
        if (result.status === 401 || result.status === 403) {
            console.warn("⚠️ rating-summary 401/403 với token, thử lại không có token (guest)...");
            result = await callApi(
                PRODUCT_API_BASE_URL,
                `/products/${productId}/rating-summary`,
                "GET",
                null,
                false // guest
            );
        }
    } else {
        // Chưa đăng nhập → gọi kiểu guest
        result = await callApi(
            PRODUCT_API_BASE_URL,
            `/products/${productId}/rating-summary`,
            "GET",
            null,
            false
        );
    }

    if (!result.ok || !result.data) {
        console.error("❌ fetchProductRatingSummary lỗi:", result);
        return null;
    }

    const data = result.data.result || result.data;

    return {
        averageRating: Number(data.averageRating ?? data.avgRating ?? 0),
        totalRatings: Number(data.totalRatings ?? data.count ?? 0),
    };
}

// Lấy danh sách rating của 1 sản phẩm
async function fetchProductRatings(productId) {
    if (!productId) {
        console.error("❌ fetchProductRatings: productId bị thiếu!", productId);
        return [];
    }

    let result;

    if (isLoggedIn()) {
        result = await callApi(
            PRODUCT_API_BASE_URL,
            `/products/${productId}/ratings`,
            "GET",
            null,
            true
        );

        if (result.status === 401 || result.status === 403) {
            console.warn("⚠️ ratings 401/403 với token, thử lại không có token (guest)...");
            result = await callApi(
                PRODUCT_API_BASE_URL,
                `/products/${productId}/ratings`,
                "GET",
                null,
                false
            );
        }
    } else {
        result = await callApi(
            PRODUCT_API_BASE_URL,
            `/products/${productId}/ratings`,
            "GET",
            null,
            false
        );
    }

    if (!result.ok || !result.data) {
        console.error("❌ fetchProductRatings lỗi:", result);
        return [];
    }

    const d = result.data;
    if (Array.isArray(d)) return d;
    if (Array.isArray(d.result)) return d.result;
    if (Array.isArray(d.content)) return d.content;

    return [];
}

// Vẽ phần tóm tắt rating (số sao trung bình + tổng số đánh giá)
async function renderProductRatingSummaryUI(productId) {
    const el = document.getElementById("product-rating-summary");
    if (!el) return;

    const summary = await fetchProductRatingSummary(productId);

    if (!summary || isNaN(summary.averageRating) || summary.totalRatings === 0) {
        el.innerHTML =
            '<span class="rating-empty">Chưa có đánh giá nào cho sản phẩm này.</span>';
        return;
    }

    const avg = summary.averageRating;
    const total = summary.totalRatings;

    el.innerHTML = `
        <div class="rating-summary-box">
            <span class="rating-average">${avg.toFixed(1)}★</span>
            <span class="rating-count">(${total} đánh giá)</span>
        </div>
    `;
}

// Vẽ danh sách các đánh giá
async function renderProductRatingListUI(productId) {
    const container = document.getElementById("product-rating-list");
    container.innerHTML = "<p>Đang tải đánh giá...</p>";

    const res = await callApi(PRODUCT_API_BASE_URL, `/products/${productId}/ratings`, "GET", null, false);

    if (!res.ok) {
        container.innerHTML = "<p>Chưa có đánh giá nào.</p>";
        return;
    }

    const ratings = res.data.result;
    const currentUser = localStorage.getItem("username");

    let html = "";

    ratings.forEach(r => {
        const date = new Date(r.createdAt).toLocaleString("vi-VN");

        html += `
      <div class="rating-item">
          <div class="rating-header">
              <strong>${r.username}</strong>
              <span class="stars">${"★".repeat(r.ratingValue)}${"☆".repeat(5 - r.ratingValue)}</span>
          </div>

          <div class="rating-comment">${r.comment}</div>
          <div class="rating-date">${date}</div>
          
          ${r.username === currentUser ? `
          <button class="delete-rating-btn" onclick="deleteMyRating(${productId}, ${r.id})">
              🗑 Xóa đánh giá
          </button>`
                : ""}
      </div>
      `;
    });

    container.innerHTML = html;
}


// Gửi đánh giá mới (dùng chung cho form)

/** Helper: build HTML sao (★) dùng class .full / .half / .empty */
function buildStarsHtml(value) {
    const maxStars = 5;
    const v = Number(value) || 0;

    const full = Math.floor(v);
    const hasHalf = v - full >= 0.5 ? 1 : 0;

    let html = '';
    for (let i = 1; i <= maxStars; i++) {
        let cls = 'empty';
        if (i <= full) cls = 'full';
        else if (i === full + 1 && hasHalf) cls = 'half';
        html += `<span class="${cls}">★</span>`;
    }
    return html;
}

// Vẽ form đánh giá
function renderProductRatingFormUI(productId, productData) {
    const formContainer = document.getElementById('product-rating-form-container');
    if (!formContainer) return;

    if (!isLoggedIn()) {
        formContainer.innerHTML = `
            <p>Bạn cần <a href="login.html">đăng nhập</a> để đánh giá sản phẩm.</p>
        `;
        return;
    }

    formContainer.innerHTML = `
        <h4>Gửi đánh giá của bạn</h4>
        <p class="rating-note">
            Lưu ý: Hệ thống chỉ chấp nhận đánh giá nếu bạn đã mua sản phẩm này 
            (kiểm tra dựa trên đơn hàng DELIVERED).
        </p>
        <form id="product-rating-form">
            <label for="ratingValue">Số sao (1–5):</label>
            <select id="ratingValue" name="ratingValue" required>
                <option value="5">5 - Rất tốt</option>
                <option value="4">4 - Tốt</option>
                <option value="3">3 - Bình thường</option>
                <option value="2">2 - Tệ</option>
                <option value="1">1 - Rất tệ</option>
            </select>

            <label for="ratingComment">Nhận xét:</label>
            <textarea id="ratingComment" name="ratingComment"
                rows="3"
                placeholder="Chia sẻ cảm nhận của bạn về sản phẩm..."></textarea>

            <button type="submit" class="btn btn-primary btn-sm" style="margin-top:8px;">
                Gửi đánh giá
            </button>
        </form>
        <p id="product-rating-message" class="rating-message"></p>
    `;

    const form = document.getElementById('product-rating-form');
    const messageEl = document.getElementById('product-rating-message');

    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (messageEl) {
            messageEl.textContent = '';
            messageEl.className = 'rating-message';
        }

        const ratingValue = form.ratingValue.value;
        const comment = form.ratingComment.value;

        const result = await submitProductRating(productId, ratingValue, comment);

        if (result.ok) {
            if (messageEl) {
                messageEl.textContent = '✅ Đã gửi đánh giá thành công!';
                messageEl.classList.add('success-message');
            }
            form.reset();
            await renderProductRatingSummaryUI(productId);
            await renderProductRatingListUI(productId);
        } else {
            const errMsg =
                result.data?.message ||
                result.error ||
                'Gửi đánh giá thất bại. Có thể bạn chưa mua sản phẩm này.';
            if (messageEl) {
                messageEl.textContent = `❌ ${errMsg}`;
                messageEl.classList.add('error-message');
            } else {
                alert(errMsg);
            }
        }
    });
}

// Khởi tạo UI rating trên trang chi tiết sản phẩm


// --- Product Admin Forms (Add/Edit) ---
function setupCustomFileInput(inputFileId, chosenTextId) { /* ... Giữ nguyên ... */
    const input = document.getElementById(inputFileId); const textEl = document.getElementById(chosenTextId);
    if (input && textEl) input.addEventListener('change', function () { textEl.textContent = this.files[0] ? this.files[0].name : 'Không có tệp nào được chọn'; });
}
function setupImagePreview(inputFileId, previewImgId, fileChosenTextId = null) { /* ... Giữ nguyên ... */
    const inputEl = document.getElementById(inputFileId); const previewEl = document.getElementById(previewImgId);
    if (inputEl && previewEl) {
        inputEl.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
                if (file.size > 5 * 1024 * 1024) { alert("File quá lớn (max 5MB)."); this.value = ""; previewEl.src = '#'; previewEl.style.display = 'none'; return; }
                const reader = new FileReader();
                reader.onload = e => { previewEl.src = e.target.result; previewEl.style.display = 'block'; };
                reader.readAsDataURL(file);
            } else { previewEl.src = '#'; previewEl.style.display = 'none'; }
        });
    }
    if (fileChosenTextId) setupCustomFileInput(inputFileId, fileChosenTextId);
}
async function handleAddProduct(event) {
    event.preventDefault();
    const form = event.target;
    const msgEls = { success: document.getElementById('add-product-success-message'), error: document.getElementById('add-product-error-message') };
    Object.values(msgEls).forEach(el => { if (el) { el.style.display = 'none'; el.textContent = ''; } });

    // *** LẤY storeId TỪ localStorage ***
    const storeId = localStorage.getItem('userStoreId');
    console.log("handleAddProduct - storeId từ localStorage:", storeId); // THÊM DÒNG NÀY
    // *** KIỂM TRA storeId ***
    if (!storeId) {
        if (msgEls.error) {
            msgEls.error.textContent = 'Lỗi: Không tìm thấy thông tin cửa hàng. Đảm bảo bạn đã đăng nhập và có cửa hàng.';
            msgEls.error.style.display = 'block';
        }
        return; // Dừng lại nếu không có storeId
    }

    // *** THÊM storeId VÀO DỮ LIỆU GỬI ĐI ***
    const reqData = {
        name: form.name.value,
        sku: form.sku.value,
        price: parseFloat(form.price.value),
        description: form.description.value,
        stockQuantity: parseInt(form.stockQuantity.value, 10),
        categoryId: form.categoryId.value,
        storeId: storeId // <<<< THÊM VÀO ĐÂY
        // Backend có thể cần sellerId thay vì storeId, hãy kiểm tra lại
    };

    if (!reqData.name.trim() || !reqData.sku.trim() || isNaN(reqData.price) || reqData.price <= 0 || isNaN(reqData.stockQuantity) || reqData.stockQuantity < 0 || !reqData.categoryId) {
        if (msgEls.error) { msgEls.error.textContent = 'Lỗi: Vui lòng điền đúng và đủ thông tin.'; msgEls.error.style.display = 'block'; } return;
    }

    const formData = new FormData();
    formData.append('productRequest', new Blob([JSON.stringify(reqData)], { type: 'application/json' }));
    if (form.imageFile.files[0]) formData.append('imageFile', form.imageFile.files[0]);

    // Gọi API, đảm bảo có token (true, true)
    const result = await callApi(PRODUCT_API_BASE_URL, '/products', 'POST', formData, true, true);

    if (result.ok) {
        if (msgEls.success) { msgEls.success.textContent = 'Thêm SP thành công!'; msgEls.success.style.display = 'block'; }
        form.reset();
        // ... (reset preview ảnh, ...)
    } else if (msgEls.error) {
        msgEls.error.textContent = `Lỗi: ${result.data?.message || result.error || 'Thêm thất bại'}`;
        msgEls.error.style.display = 'block';
    }
}

async function loadProductForEdit(productIdParam) {
    const form = document.getElementById('editProductForm');
    const pageContainer = document.getElementById('edit-product-container'); // container chính

    if (!form || !pageContainer) {
        console.error("Form (#editProductForm) hoặc container chính (#edit-product-container) không tìm thấy.");
        if (pageContainer) {
            pageContainer.innerHTML = "<p class='error-message'>Lỗi giao diện: Không tìm thấy các thành phần trang cần thiết.</p>";
        }
        return false;
    }

    // Lấy id từ URL nếu chưa truyền vào
    const productId = productIdParam || new URLSearchParams(window.location.search).get('id');
    if (!productId) {
        pageContainer.innerHTML = "<p class='error-message'>ID sản phẩm không hợp lệ. <a href='products.html'>Quay lại danh sách</a>.</p>";
        form.style.display = 'none';
        return false;
    }

    form.style.display = 'none'; // ẩn form, để <p> "Đang tải..." hiển thị

    const result = await callApi(
        PRODUCT_API_BASE_URL,
        `/products/${productId}`,
        'GET',
        null,
        true
    );

    if (!result || !result.ok || !result.data) {
        let errorMessage = 'Không tìm thấy sản phẩm hoặc có lỗi xảy ra.';
        if (result && result.data?.message) {
            errorMessage = result.data.message;
        } else if (result && result.error) {
            errorMessage = result.error;
        }
        pageContainer.innerHTML = `<p class="error-message">Lỗi tải sản phẩm để sửa: ${errorMessage}. <a href="products.html">Quay lại danh sách</a>.</p>`;
        form.style.display = 'none';
        return false;
    }

    // ✅ UNWRAP ĐÚNG DỮ LIỆU
    const raw = result.data;
    const p = raw.result || raw;
    console.log("🧩 Product for edit:", p);

    const currentUserRole = getUserRole();
    const currentUserId = localStorage.getItem('currentUserId');

    let canEditThisProduct = false;

    // CHỈ SELLER ĐƯỢC SỬA SẢN PHẨM CỦA CHÍNH MÌNH
    if (currentUserRole === 'SELLER') {
        if (!p.sellerInfo || !p.sellerInfo.userId) {
            console.warn("Dữ liệu sản phẩm không chứa sellerInfo.userId:", p);
            pageContainer.innerHTML = `
                <h2>Chỉnh Sửa Sản Phẩm</h2>
                <p class="error-message">
                    Không thể xác định quyền chỉnh sửa do thiếu thông tin người bán trong dữ liệu sản phẩm.
                    <a href="products.html">Quay lại</a>.
                </p>`;
            form.style.display = 'none';
            return false;
        }
        if (String(p.sellerInfo.userId) === String(currentUserId)) {
            canEditThisProduct = true;
        }
    }

    // ADMIN không dùng giao diện này để sửa
    if (!canEditThisProduct) {
        let noPermissionMessage = 'Bạn không có quyền chỉnh sửa sản phẩm này.';
        if (currentUserRole === 'ADMIN') {
            noPermissionMessage = 'Quản trị viên không được phép chỉnh sửa chi tiết sản phẩm từ giao diện này.';
        }
        pageContainer.innerHTML = `
            <h2>Chỉnh Sửa Sản Phẩm</h2>
            <p class="error-message">${noPermissionMessage} <a href="products.html">Quay lại</a>.</p>`;
        form.style.display = 'none';
        return false;
    }

    // ---------- Đổi text "Đang tải..." rồi load categories ----------
    const loadingParagraphOriginal = pageContainer.querySelector('p:first-child');
    if (loadingParagraphOriginal && loadingParagraphOriginal.textContent.includes('Đang tải thông tin sản phẩm...')) {
        loadingParagraphOriginal.textContent = 'Đang tải danh mục...';
    }

    await loadCategoriesAndBuildMap('edit-product-category-id');

    if (loadingParagraphOriginal) {
        loadingParagraphOriginal.style.display = 'none';
    }

    // ---------- Điền dữ liệu vào form ----------
    form.style.display = 'block';
    document.title = `Chỉnh sửa: ${p.name || 'Sản phẩm'} - HyperBuy`;

    document.getElementById('product-id').value = p.id;
    form.elements['name'].value = p.name || '';
    form.elements['sku'].value = p.sku || '';
    form.elements['price'].value = p.price === undefined ? '' : p.price;
    form.elements['description'].value = p.description || '';
    form.elements['stockQuantity'].value = p.stockQuantity === undefined ? 0 : p.stockQuantity;

    const activeCheckbox = form.elements['active'];
    if (activeCheckbox) {
        activeCheckbox.checked = typeof p.active === 'boolean' ? p.active : true;
    }

    // ---------- storeId bắt buộc để update ----------
    if (!p.sellerInfo || !p.sellerInfo.storeId) {
        console.warn("Dữ liệu sản phẩm không chứa sellerInfo.storeId:", p);
        pageContainer.innerHTML = `
            <h2>Chỉnh Sửa Sản Phẩm</h2>
            <p class="error-message">
                Không thể xác định storeId của sản phẩm. Không thể tiến hành chỉnh sửa.
                <a href="products.html">Quay lại</a>.
            </p>`;
        form.style.display = 'none';
        return false;
    }
    form.storeId = p.sellerInfo.storeId;

    // ---------- Ảnh hiện tại ----------
    const currentImgPreview = document.getElementById('current-product-image-preview');
    const noImgText = document.getElementById('current-product-no-image');

    if (p.imageUrl) {
        let editImgUrl = p.imageUrl;
        if (editImgUrl.startsWith('http://productservice')) {
            editImgUrl = editImgUrl.replace(/^http:\/\/productservice:\d+/, PRODUCT_IMAGE_BASE_URL);
        } else if (editImgUrl.startsWith('http://localhost:8081')) {
            editImgUrl = editImgUrl.replace('http://localhost:8081', PRODUCT_IMAGE_BASE_URL);
        } else if (!editImgUrl.startsWith('http://') && !editImgUrl.startsWith('https://') && editImgUrl.includes('/')) {
            editImgUrl = `${PRODUCT_IMAGE_BASE_URL}${editImgUrl}`;
        } else if (!editImgUrl.startsWith('http://') && !editImgUrl.startsWith('https://') && !editImgUrl.includes('/')) {
            editImgUrl = `${PRODUCT_IMAGE_BASE_URL}/product-images/${editImgUrl}`;
        }

        if (currentImgPreview) {
            currentImgPreview.src = editImgUrl;
            currentImgPreview.style.display = 'block';
        }
        if (noImgText) noImgText.style.display = 'none';
    } else {
        if (currentImgPreview) {
            currentImgPreview.src = '#';
            currentImgPreview.style.display = 'none';
        }
        if (noImgText) noImgText.style.display = 'block';
    }

    // ---------- Reset preview ảnh mới ----------
    const newImagePreview = document.getElementById('new-product-image-preview');
    const newImagePreviewBox = document.getElementById('new-image-preview-box');
    if (newImagePreview) newImagePreview.src = '#';
    if (newImagePreviewBox) newImagePreviewBox.style.display = 'none';

    const fileChosenTextEl = document.getElementById('file-chosen-text');
    if (fileChosenTextEl) fileChosenTextEl.textContent = 'Không có tệp nào được chọn';
    const imageFileInputEl = document.getElementById('edit-product-image-file');
    if (imageFileInputEl) imageFileInputEl.value = "";

    // ---------- Chọn danh mục ----------
    const categorySelect = document.getElementById('edit-product-category-id');
    const categoryIdToSelect = String(p.category?.id || p.categoryId || "");
    if (categorySelect) {
        if (
            categoryIdToSelect &&
            Array.from(categorySelect.options).some(
                (opt) => String(opt.value) === categoryIdToSelect
            )
        ) {
            categorySelect.value = categoryIdToSelect;
        } else {
            if (categoryIdToSelect)
                console.warn(
                    `ID danh mục "${categoryIdToSelect}" từ sản phẩm "${p.name}" không tồn tại trong select.`
                );
            categorySelect.value = "";
        }
    }

    // ---------- Nút chat với seller (nếu cần) ----------
    const chatButton = document.getElementById('chat-with-seller-button');
    if (chatButton) {
        if (p.sellerInfo && p.sellerInfo.userId && currentUserRole !== 'SELLER') {
            chatButton.style.display = 'block';
            chatButton.onclick = () => initiateChatWithSeller(p.sellerInfo.userId);
        } else {
            chatButton.style.display = 'none';
        }
    }

    return true;
}

async function handleUpdateProduct(event) {
    event.preventDefault();
    const form = event.target;
    const productId = form.productId.value;
    const msgEls = {
        success: document.getElementById('edit-product-success-message'),
        error: document.getElementById('edit-product-error-message')
    };
    Object.values(msgEls).forEach(el => { if (el) { el.style.display = 'none'; el.textContent = ''; } });

    const name = form.elements['name'].value.trim();
    const sku = form.elements['sku'].value.trim();
    const priceStr = form.elements['price'].value;
    const description = form.elements['description'].value.trim();
    const stockQuantityStr = form.elements['stockQuantity'].value;
    const categoryId = form.elements['categoryId'].value;
    const activeCheckbox = form.elements['active'];

    // validate như cũ...

    const storeIdFromForm = form.storeId;
    if (!storeIdFromForm) {
        if (msgEls.error) {
            msgEls.error.textContent = 'Lỗi: Không tìm thấy thông tin Store ID của sản phẩm để cập nhật.';
            msgEls.error.style.display = 'block';
        }
        return;
    }

    const reqData = {
        name,
        sku,
        price: parseFloat(priceStr),
        description,
        stockQuantity: parseInt(stockQuantityStr, 10),
        categoryId,
        storeId: storeIdFromForm
    };
    if (activeCheckbox) {
        reqData.active = activeCheckbox.checked;
    }

    const formData = new FormData();
    formData.append('productRequest', new Blob([JSON.stringify(reqData)], { type: 'application/json' }));

    const imageFileInput = form.elements['imageFile'];
    if (imageFileInput && imageFileInput.files[0]) {
        formData.append('imageFile', imageFileInput.files[0]);
    }

    // >>> THÊM HEADER X-Store-Id Ở ĐÂY <<<
    const headers = { 'X-Store-Id': storeIdFromForm };

    const result = await callApi(
        PRODUCT_API_BASE_URL,
        `/products/${productId}`,
        'PUT',
        formData,
        true,
        true,
        headers
    );

    if (result.ok) {
        alert('Cập nhật sản phẩm thành công!');
        window.location.href = `product-detail.html?id=${productId}`;
    } else {
        if (msgEls.error) {
            let errorMessage = result.data?.message || result.error || 'Cập nhật thất bại. Vui lòng thử lại.';
            if (result.status === 500 && errorMessage.includes('Uncategorized error')) {
                errorMessage = `Server gặp lỗi 500 (Uncategorized error). Hãy kiểm tra lại dữ liệu sản phẩm hoặc log backend.`;
            }
            msgEls.error.textContent = `Lỗi (${result.status || 'không xác định'}): ${errorMessage}`;
            msgEls.error.style.display = 'block';
        }
    }
}
async function handleToggleProduct(btn) {
    const productId = btn.dataset.productId;
    const storeId = btn.dataset.storeId;
    const isDeactivate = btn.textContent.includes("Ngưng");

    const endpoint = isDeactivate
        ? `/products/${productId}/deactivate`
        : `/products/${productId}/activate`;

    const result = await callApi(
        PRODUCT_API_BASE_URL,
        endpoint,
        "PATCH",
        null,
        true,
        false,
        { "X-Store-Id": storeId }
    );

    if (result.ok) {
        alert("Cập nhật trạng thái thành công!");
        loadProductDetail();
    } else {
        alert("Thao tác thất bại!");
    }
}




// Hàm trợ giúp (đã có từ trước)
function getCurrentProductListContainerId() {
    if (document.getElementById('product-grid-home')) return 'product-grid-home';
    if (document.getElementById('product-grid-all')) return 'product-grid-all';
    return null;
}
async function handleActivateProduct(eventTarget) { // Nhận eventTarget
    const productId = eventTarget.dataset.productId;
    const storeId = eventTarget.dataset.storeId; // Lấy storeId từ data attribute

    const userRole = getUserRole();
    if (!isLoggedIn() || (userRole !== 'ADMIN' && userRole !== 'SELLER')) {
        alert("Bạn không có quyền thực hiện hành động này.");
        return;
    }

    if (!storeId) {
        alert("Lỗi: Không tìm thấy thông tin cửa hàng (Store ID) để thực hiện hành động này trên sản phẩm.");
        return;
    }

    if (!confirm('Bạn có chắc muốn KÍCH HOẠT LẠI sản phẩm này không? Sản phẩm sẽ được hiển thị lại cho khách hàng.')) {
        return;
    }

    const headers = { 'X-Store-Id': storeId }; // Chuẩn bị header
    const result = await callApi(PRODUCT_API_BASE_URL, `/products/${productId}/activate`, 'PATCH', '1', true, false, headers);

    if (result.ok) {
        alert('Đã kích hoạt lại sản phẩm thành công!');
        if (window.location.pathname.includes('product-detail.html')) {
            loadProductDetail();
        }
    } else {
        alert(`Lỗi khi kích hoạt sản phẩm: ${result.data?.message || result.error || `Lỗi server với status ${result.status}`}`);
    }
}
async function handleSellerDeleteProduct(eventTarget) {
    const productId = eventTarget.dataset.productId;
    const storeId = eventTarget.dataset.storeId;

    if (!productId || !storeId) {
        alert("Thiếu productId hoặc storeId.");
        return;
    }

    if (!confirm("XÓA VĨNH VIỄN sản phẩm này?")) return;

    const headers = {
        "X-Store-Id": storeId
    };

    const result = await callApi(
        PRODUCT_API_BASE_URL,
        `/products/${productId}`,
        "DELETE",
        null,      // ✅ DELETE không body
        true,
        false,
        headers
    );

    if (result.ok || result.status === 204) {
        alert("Xóa sản phẩm thành công!");
        window.location.href = "products.html";
    } else {
        alert(result.data?.message || "Xóa thất bại!");
    }
}


async function handleHardDeleteProduct(productId) {
    if (!isLoggedIn() || getUserRole() !== 'ADMIN') {
        alert("Bạn không có quyền thực hiện hành động này.");
        return;
    }
    if (!confirm(`XÁC NHẬN XÓA HOÀN TOÀN sản phẩm này (ID: ${productId}) khỏi cơ sở dữ liệu?\n\nHành động này KHÔNG THỂ hoàn tác!`)) {
        return;
    }
    // Body là '1' (raw text) theo Postman (image_4bef8d.png). Nếu API DELETE không cần body, truyền null.
    const bodyForDelete = '1'; // Hoặc null

    const result = await callApi(PRODUCT_API_BASE_URL, `/products/${productId}`, 'DELETE', bodyForDelete, true);

    if (result.ok || result.status === 204) { // 204 No Content cũng là thành công cho DELETE
        alert(`Sản phẩm (ID: ${productId}) đã được xóa hoàn toàn thành công!`);
        window.location.href = 'products.html'; // Chuyển về trang danh sách sản phẩm
    } else {
        alert(`Lỗi khi xóa hoàn toàn sản phẩm: ${result.data?.message || result.error || `Lỗi server với status ${result.status}`}`);
    }
}

// Hàm trợ giúp (nếu chưa có hoặc cần cập nhật)
function getCurrentProductListContainerId() {
    if (document.getElementById('product-grid-home')) return 'product-grid-home';
    if (document.getElementById('product-grid-all')) return 'product-grid-all';
    return null;
}
async function handleClearCart(confirmAction = true) {
    if (!isLoggedIn()) {
        alert("Bạn cần đăng nhập để thực hiện hành động này.");
        window.location.href = 'login.html'; // Thêm điều hướng nếu chưa đăng nhập
        return;
    }

    if (confirmAction && !confirm('Bạn có chắc chắn muốn xóa TOÀN BỘ sản phẩm khỏi giỏ hàng không? Hành động này không thể hoàn tác.')) {
        return;
    }

    console.log("handleClearCart: Token đang sử dụng:", getToken());
    console.log("handleClearCart: Đang gọi API DELETE /my-cart để xóa hết giỏ hàng...");

    const result = await callApi(CART_API_BASE_URL, '/my-cart', 'DELETE', null, true);

    if (result.ok || result.status === 204) {
        alert('Đã xóa toàn bộ sản phẩm khỏi giỏ hàng thành công!');
        console.log("handleClearCart: Xóa hết giỏ hàng thành công.");
        await fetchCartData();
    } else {
        let errorMessage = result.data?.message || result.error || `Lỗi server với status ${result.status}`;
        alert(`Lỗi khi xóa hết giỏ hàng: ${errorMessage}`);
        console.error("handleClearCart Error:", result);

        if (result.status === 401 || result.status === 403) {
            console.warn("Token không hợp lệ hoặc hết hạn khi xóa giỏ hàng. Đăng xuất người dùng.");
            handleLogout(); // Tự động đăng xuất nếu lỗi là 401/403
        }
    }
}
// TẠO ĐƠN HÀNG THEO API MỚI (GET /api/v1/carts/my-cart + POST /api/v1/orders)
// ====== (2) TẠO ĐƠN HÀNG THEO API MỚI (items: productId + quantity) ======
async function handleCreateOrderWithDetails(shippingData, billingData, paymentData) {
    const userId = localStorage.getItem('currentUserId');
    const cartData = await fetchCartData();

    if (!cartData || !cartData.items || cartData.items.length === 0) {
        alert("Giỏ hàng của bạn đang trống.");
        return;
    }

    // 1. LẤY DANH SÁCH ITEMS ĐỂ ĐẶT HÀNG
    let selectedItems = [];
    try {
        const raw = localStorage.getItem(CHECKOUT_ITEMS_KEY);
        if (raw) selectedItems = JSON.parse(raw);
    } catch (e) { }

    let itemsToOrder = cartData.items;

    if (selectedItems && selectedItems.length > 0) {
        const selectedMap = new Map(
            selectedItems.map(it => [String(it.productId), Number(it.quantity) || 1])
        );

        itemsToOrder = cartData.items
            .filter(item => selectedMap.has(String(item.productId)))
            .map(item => {
                const newQty = selectedMap.get(String(item.productId));
                const qty = Math.min(Number(item.quantity), newQty);
                const price = parseFloat(item.currentPrice || item.priceAtAddition || 0);

                return {
                    productId: item.productId,
                    quantity: qty,
                    price: price
                };
            });
    } else {
        // Nếu không có selectedItems → đặt tất cả giỏ
        itemsToOrder = cartData.items.map(item => ({
            productId: item.productId,
            quantity: item.quantity,
            price: parseFloat(item.currentPrice || item.priceAtAddition || 0)
        }));
    }

    // 2. TẠO BODY ORDER GỬI SANG ORDER-SERVICE
    const orderBody = {
        userId: userId,

        shippingAddress: shippingData,
        billingAddress: billingData || shippingData,

        paymentMethod: paymentData.paymentMethod || "COD",

        // 🎁 Voucher đã chọn
        voucherCode: paymentData.voucherCode || null,

        // 💸 Số tiền giảm đã apply trước đó từ voucher-service
        discountAmount: paymentData.discountAmount || 0,

        // Danh sách item đặt hàng
        items: itemsToOrder
    };

    console.log("📦 ORDER BODY GỬI SANG ORDER-SERVICE:", orderBody);

    // 3. GỌI ORDER-SERVICE TẠO ĐƠN HÀNG
    const result = await callApi(
        ORDER_API_BASE_URL,
        "/orders",
        "POST",
        orderBody,
        true
    );

    if (!result.ok) {
        alert("Tạo đơn hàng thất bại: " + (result.data?.message || result.error));
        return;
    }

    // 4. XÓA GIỎ HÀNG / XÓA selectedItems
    localStorage.removeItem(CHECKOUT_ITEMS_KEY);

    alert("Đặt hàng thành công!");
    window.location.href = "my-orders.html";
}


function renderOrderDetailsOnPage(order, contentEl, orderIdForTitleOverride = null) {
    if (!order || !order.id) {
        if (contentEl) {
            contentEl.innerHTML = `<p class="error-message">Dữ liệu đơn hàng không hợp lệ hoặc thiếu ID.</p>`;
        }
        console.error("renderOrderDetailsOnPage: Dữ liệu đơn hàng không hợp lệ hoặc thiếu ID.", order);
        return;
    }
    if (!contentEl) {
        console.error("renderOrderDetailsOnPage: contentEl không được cung cấp.");
        return;
    }

    const displayOrderId = orderIdForTitleOverride || order.id;
    const finalStatus = order.status || order.orderStatus || "N/A";

    document.title = `Đơn hàng #${displayOrderId} - HyperBuy`;

    const orderDate = order.createdAt
        ? new Date(order.createdAt).toLocaleDateString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        })
        : "N/A";

    // ====== TÍNH TIỀN THEO FE (KHÔNG ĐỤNG BE) ======
    const rawTotal = parseFloat(order.totalAmount) || 0;                       // tổng tiền gốc BE trả về
    const rawDiscount = parseFloat(order.discountAmount || 0) || 0;              // nếu BE có trường discountAmount
    const finalAmount = Math.max(rawTotal - rawDiscount, 0);                     // tiền sau giảm (FE tự tính)

    const totalStr = rawTotal.toLocaleString("vi-VN", { style: "currency", currency: "VND" });
    const discountStr = rawDiscount.toLocaleString("vi-VN", { style: "currency", currency: "VND" });
    const finalStr = finalAmount.toLocaleString("vi-VN", { style: "currency", currency: "VND" });

    // Khối HTML hiển thị tiền
    let pricingHtml = "";
    if (rawDiscount > 0) {
        pricingHtml = `
            <p><strong>Tổng tiền hàng:</strong> ${totalStr}</p>
            <p><strong>Giảm voucher${order.voucherCode ? " (" + order.voucherCode + ")" : ""}:</strong> -${discountStr}</p>
            <p><strong>Thành tiền phải trả:</strong> <span class="order-grand-total">${finalStr}</span></p>
        `;
    } else {
        // Không có giảm giá → hiện như cũ
        pricingHtml = `
            <p><strong>Tổng tiền:</strong> <span class="order-grand-total">${totalStr}</span></p>
        `;
    }

    // ================= SẢN PHẨM TRONG ĐƠN =================
    let itemsHtml = '<ul class="order-items-list-detail">';

    if (order.items && order.items.length > 0) {
        order.items.forEach(item => {
            const productName = item.productName || "Sản phẩm không rõ";

            // Ảnh
            let imageUrl = item.imageUrl;
            if (!imageUrl || imageUrl === "null" || imageUrl === "") {
                imageUrl = "https://placehold.co/80x80";
            } else {
                imageUrl = imageUrl.replace("http://localhost:8081", PRODUCT_IMAGE_BASE_URL);
            }

            // Review
            let reviewHtml = "";
            const delivered = (order.orderStatus === "DELIVERED" || order.status === "DELIVERED");
            if (delivered) {
                if (item.rating && item.rating > 0) {
                    reviewHtml = `
                        <div class="stars-display">
                            ${"★".repeat(item.rating)}${"☆".repeat(5 - item.rating)}
                        </div>
                        <p>${item.review || ""}</p>
                    `;
                } else {
                    reviewHtml = `
                        <button class="btn-review btn-review-item"
                                data-product-id="${item.productId}"
                                data-order-id="${order.id}">
                            ⭐ Đánh giá
                        </button>
                    `;
                }
            }

            itemsHtml += `
                <li class="order-item-detail-entry">
                    <img src="${imageUrl}" class="order-item-thumb">
                    <div class="order-item-info">
                        <span class="order-item-name">${productName}</span>
                        <span class="order-item-quantity">Số lượng: ${item.quantity || 1}</span>
                        <span class="order-item-price">Giá: ${(parseFloat(item.price) || 0).toLocaleString('vi-VN')} đ</span>
                    </div>
                    <div class="order-item-review">
                        ${reviewHtml}
                    </div>
                </li>
            `;
        });
    } else {
        itemsHtml += "<li>Không có sản phẩm nào trong đơn hàng này.</li>";
    }
    itemsHtml += "</ul>";

    const formatAddress = (addr) => {
        if (!addr) return "N/A";
        const parts = [];
        if (addr.addressLine1) parts.push(addr.addressLine1);
        if (addr.addressLine2) parts.push(addr.addressLine2);
        if (addr.city) parts.push(addr.city);
        if (addr.postalCode) parts.push(addr.postalCode);
        if (addr.country) parts.push(addr.country);
        const formatted = parts.join(", ").replace(/,\s*$/, "").replace(/^,\s*/, "");
        return formatted || "N/A";
    };

    contentEl.innerHTML = `
        <div class="order-detail-card">
            <div class="order-detail-header">
                <h3>Đơn hàng #${order.id}</h3>
                <p><strong>Ngày đặt:</strong> ${orderDate}</p>
                <p><strong>Trạng thái:</strong>
                    <span class="order-status-${finalStatus.toLowerCase()}">${finalStatus}</span>
                </p>
                <p><strong>Khách hàng:</strong> ${order.userId || "N/A"}</p>
                ${pricingHtml}
            </div>

            <div class="order-section">
                <h4>Thông tin thanh toán</h4>
                <p><strong>Phương thức:</strong> ${order.paymentMethod || "N/A"}</p>
                <p><strong>Mã giao dịch:</strong> ${order.paymentTransactionId || "Chưa có / COD"}</p>
            </div>

            <div class="order-section">
                <h4>Địa chỉ giao hàng</h4>
                <p>${formatAddress(order.shippingAddress)}</p>
            </div>

            <div class="order-section">
                <h4>Địa chỉ thanh toán</h4>
                <p>${formatAddress(order.billingAddress)}</p>
            </div>

            <div class="order-section">
                <h4>Các sản phẩm trong đơn</h4>
                ${itemsHtml}
            </div>

            <div style="margin-top: 16px;">
                <a href="my-orders.html" class="btn btn-primary">Quay lại danh sách đơn hàng</a>
            </div>
        </div>
    `;
}

document.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-review-item");
    if (!btn) return;

    reviewingProductId = btn.dataset.productId;
    reviewingOrderId = btn.dataset.orderId;

    document.getElementById("review-stars").value = 5;
    document.getElementById("review-comment").value = "";
    document.getElementById("review-modal").style.display = "flex";
});
async function loadMyOrders() {
    const container = document.getElementById("my-orders-container");
    if (!container) {
        console.warn("loadMyOrders: #my-orders-container not found");
        return;
    }

    if (!isLoggedIn()) {
        container.innerHTML =
            `<p class="my-orders-empty">
                Bạn cần <a href="login.html">đăng nhập</a> để xem lịch sử đơn hàng.
            </p>`;
        return;
    }

    container.innerHTML = `<p class="text-muted">Đang tải đơn hàng...</p>`;

    const res = await callApi(
        ORDER_API_BASE_URL,
        "/orders/my-orders",
        "GET",
        null,
        true
    );

    console.log("MY ORDERS response:", res);

    if (!res.ok || !Array.isArray(res.data)) {
        container.innerHTML =
            `<p class="my-orders-empty">
            Không thể tải danh sách đơn hàng. Vui lòng thử lại sau.
        </p>`;
        return;
    }

    // ============================
    // ✅ SẮP XẾP: Đơn hàng mới nhất lên trên cùng
    // ============================
    const orders = [...res.data].sort((a, b) => {
        const dateA = new Date(a.orderDate || a.createdAt || 0);
        const dateB = new Date(b.orderDate || b.createdAt || 0);

        // So sánh ngày trước
        if (!isNaN(dateB - dateA) && (dateB - dateA) !== 0) {
            return dateB - dateA; // mới hơn đứng trước
        }

        // Nếu ngày giống nhau -> so sánh ID giảm dần
        return (Number(b.id) || 0) - (Number(a.id) || 0);
    });
    // ============================

    if (orders.length === 0) {
        container.innerHTML =
            `<p class="my-orders-empty">
            Bạn chưa có đơn hàng nào. 
            <a href="products.html">Mua sắm ngay</a> nhé!
        </p>`;
        return;
    }

    // Hàm format ngày như cũ
    function formatDate(isoString) {
        if (!isoString) return "";
        const d = new Date(isoString);
        return d.toLocaleString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    }


    function getStatusBadge(status) {
        if (!status) {
            return { label: "Không rõ", cls: "order-status-badge" };
        }
        const upper = status.toUpperCase();
        if (upper === "DELIVERED") {
            return { label: "Đã giao", cls: "order-status-badge order-status-delivered" };
        }
        if (upper === "CONFIRMED") {
            return { label: "Đã xác nhận", cls: "order-status-badge order-status-confirmed" };
        }
        if (upper === "PENDING") {
            return { label: "Đang xử lý", cls: "order-status-badge order-status-pending" };
        }
        if (upper === "CANCELLED" || upper === "CANCELED") {
            return { label: "Đã hủy", cls: "order-status-badge order-status-cancelled" };
        }
        return { label: upper, cls: "order-status-badge" };
    }

    let html = "";

    // Sắp xếp đơn hàng mới nhất lên đầu bằng orderDate
    orders.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate));

    orders.forEach(order => {
        const id = order.id;
        const orderDate = formatDate(order.orderDate || order.createdAt);

        // 🔥 TÍNH TIỀN SAU KHI GIẢM – CHỈ FE
        const rawTotal = parseFloat(order.totalAmount) || 0;                 // tổng tiền gốc BE trả
        const rawDiscount = parseFloat(order.discountAmount || 0) || 0;        // tiền giảm (nếu BE có)
        const finalAmount = Math.max(rawTotal - rawDiscount, 0);               // tiền phải trả sau giảm
        const totalText = finalAmount.toLocaleString("vi-VN") + " đ";

        let discountLine = "";
        if (rawDiscount > 0) {
            discountLine = `
                <div class="order-card-discount">
                    Đã giảm: -${rawDiscount.toLocaleString("vi-VN")} đ
                </div>`;
        }

        const status = order.status || order.orderStatus;
        const badge = getStatusBadge(status);

        html += `
        <article class="order-card">
            <div class="order-card-main">
                <div class="order-card-title">
                    Đơn hàng #${id}
                </div>
                <div class="order-card-meta">
                    <span><i class="fa-regular fa-clipboard"></i> Mã: #${id}</span>
                    <span><i class="fa-regular fa-clock"></i> Ngày đặt: ${orderDate}</span>
                </div>
                <div class="order-card-total">
                    Tổng tiền: <strong>${totalText}</strong>
                </div>
                ${discountLine}
            </div>
            <div class="order-card-side">
                <span class="${badge.cls}">${badge.label}</span>
                <div class="order-card-date-label">Ngày đặt</div>
                <div class="order-card-date-value">${orderDate}</div>
                <a href="order-detail.html?id=${id}" 
                   class="btn-view-order">
                    Xem chi tiết
                    <i class="fa-solid fa-arrow-right"></i>
                </a>
            </div>
        </article>
        `;
    });

    container.innerHTML = html;
}

async function loadOrderDetail(orderId) {
    const contentEl = document.getElementById('order-detail-content');
    if (!contentEl) {
        console.warn("loadOrderDetail: Element #order-detail-content không tìm thấy.");
        return;
    }

    if (!isLoggedIn()) {
        contentEl.innerHTML = '<p>Bạn cần <a href="login.html">đăng nhập</a> để xem chi tiết đơn hàng.</p>';
        return;
    }

    contentEl.innerHTML = `<p>Đang tải chi tiết đơn hàng #${orderId}...</p>`;
    const result = await callApi(ORDER_API_BASE_URL, `/orders/${orderId}`, 'GET', null, true);

    console.log(`loadOrderDetail (API Fetch for ${orderId}): API response:`, JSON.stringify(result, null, 2));

    // Sửa: Kiểm tra result.data thay vì result.data.result
    if (result.ok && result.data) {
        const orderFromApi = result.data;
        renderOrderDetailsOnPage(orderFromApi, contentEl, orderId);
    } else {
        contentEl.innerHTML = `<p class="error-message">Lỗi tải chi tiết đơn hàng: ${result.data?.message || result.error || `Không tìm thấy đơn hàng #${orderId}`}</p>`;
    }
}
// --- Notifications ---

/**
 * Cập nhật giao diện người dùng cho chuông thông báo và danh sách dropdown.
 * @param {Array | null} notifications - Mảng các đối tượng thông báo hoặc null.
 */
function updateNotificationUI(notifications) {
    const bellContainer = document.getElementById('nav-notification-bell');
    const countEl = document.getElementById('notification-count');
    const listEl = document.getElementById('notification-list-items');
    const dropdown = document.getElementById('notification-dropdown'); // Lấy dropdown

    if (!bellContainer || !countEl || !listEl || !dropdown) {
        console.warn("Các phần tử UI thông báo không tìm thấy.");
        return;
    }

    if (!isLoggedIn()) {
        bellContainer.style.display = 'none';
        return;
    }

    bellContainer.style.display = 'inline-block'; // Or 'flex' or 'block'

    if (!notifications || notifications.length === 0) {
        countEl.textContent = '';
        countEl.style.display = 'none';
        listEl.innerHTML = '<li><a class="dropdown-item-notif" href="#">Không có thông báo nào.</a></li>';
        return;
    }
    // Sắp xếp: chưa đọc lên trước, sau đó theo thời gian mới nhất
    notifications.sort((a, b) => {
        const readA = a.read || a.isRead;
        const readB = b.read || b.isRead;
        if (readA !== readB) {
            return readA ? 1 : -1;
        }
        return new Date(b.createdAt) - new Date(a.createdAt);
    });


    const unreadNotifications = notifications.filter(n => !(n.read || n.isRead));
    const unreadCount = unreadNotifications.length;

    if (unreadCount > 0) {
        countEl.textContent = unreadCount > 9 ? '9+' : unreadCount;
        countEl.style.display = 'inline-block';
    } else {
        countEl.textContent = '';
        countEl.style.display = 'none';
    }

    listEl.innerHTML = ''; // Xóa danh sách cũ

    notifications.slice(0, 5).forEach(notif => { // Chỉ hiển thị 5 thông báo gần nhất trong dropdown
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.className = 'dropdown-item-notif';
        a.href = notif.link || '#';
        a.dataset.id = notif.id; // Quan trọng để đánh dấu đã đọc

        const isRead = notif.read || notif.isRead;
        if (!isRead) {
            // a.classList.add('unread'); // Không cần class unread nữa nếu dùng fw-bold
        }

        const message = notif.message || 'Nội dung thông báo không có.';
        // Định dạng thời gian ngắn gọn hơn cho dropdown
        const time = notif.createdAt ? new Date(notif.createdAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }) : '';

        a.innerHTML = `
            <div class="notif-item-content">
                <span class="notif-message ${isRead ? '' : 'fw-bold'}">${message.substring(0, 45)}${message.length > 45 ? '...' : ''}</span>
                <small class="notif-time">${time}</small>
            </div>
            ${!isRead ? `<button class="btn btn-sm mark-as-read-btn" data-id="${notif.id}" title="Đánh dấu đã đọc" style="/* CSS cho nút này đã ở trên */">✓</button>` : ''}
        `;
        li.appendChild(a);
        listEl.appendChild(li);
    });
}
/**
 * Lấy danh sách thông báo cho người dùng hiện tại.
 * **Lưu ý:** API endpoint '/notifications' được giả định. Bạn có thể cần thay đổi nó
 * thành '/notifications/my-notifications' hoặc endpoint chính xác từ backend của bạn.
 */
async function fetchMyNotifications() {
    if (!isLoggedIn()) {
        updateNotificationUI(null);
        return null;
    }

    const myUserId = localStorage.getItem('currentUserId'); // Lấy userId đã lưu

    if (!myUserId) {
        console.error("fetchMyNotifications: Không tìm thấy User ID của người dùng hiện tại trong localStorage.");
        updateNotificationUI(null);
        return null;
    }

    const endpoint = `/notifications/${myUserId}`; // Endpoint với userId cụ thể
    console.log("fetchMyNotifications: Calling API: " + NOTIFICATION_API_BASE_URL + endpoint);

    const result = await callApi(NOTIFICATION_API_BASE_URL, endpoint, 'GET', null, true);
    console.log(`fetchMyNotifications response for user ${myUserId}:`, JSON.stringify(result, null, 2));


    let notificationsArray = null;
    if (result.ok && result.data) {
        if (Array.isArray(result.data.result)) { // API trả về {"result": [array]}
            notificationsArray = result.data.result;
        } else if (Array.isArray(result.data.content)) { // Dự phòng cho cấu trúc phân trang
            notificationsArray = result.data.content;
        } else if (Array.isArray(result.data)) { // Dự phòng cho mảng trực tiếp
            notificationsArray = result.data;
        }
    }

    if (notificationsArray) {
        updateNotificationUI(notificationsArray);
        return notificationsArray;
    } else {
        if (result.status !== 404 && result.ok === false) {
            console.error("Lỗi khi tải thông báo:", result.data?.message || result.error || `Status: ${result.status}`);
        }
        updateNotificationUI(null);
        return null;
    }
}


async function markNotificationAsRead(notificationId) {
    if (!isLoggedIn() || !notificationId) return;

    // Dựa trên image_d99857.png: PATCH /notifications/{id}/read
    const result = await callApi(NOTIFICATION_API_BASE_URL, `/notifications/${notificationId}/read`, 'PATCH', null, true);

    if (result.ok) {
        console.log(`Thông báo ${notificationId} đã được đánh dấu là đã đọc.`);
        await fetchMyNotifications(); // Tải lại để cập nhật UI
    } else {
        alert(`Lỗi đánh dấu đã đọc: ${result.data?.message || result.error}`);
    }
}


async function fetchNotificationById(notificationId) {
    if (!isLoggedIn() || !notificationId) return;
    const result = await callApi(NOTIFICATION_API_BASE_URL, `/notifications/${notificationId}`, 'GET', null, true);
    if (result.ok) {
        return result.data;
    } else {
        console.error(`Lỗi tải thông báo ${notificationId}:`, result.data?.message || result.error);
        return null;
    }
}
async function sendAdminNotification(event) {
    event.preventDefault();
    const form = event.target;
    const msgEls = { success: document.getElementById('send-notification-success-message'), error: document.getElementById('send-notification-error-message') };
    Object.values(msgEls).forEach(el => { if (el) { el.style.display = 'none'; el.textContent = ''; } });

    const userId = form.userId.value.trim();
    const message = form.message.value.trim();

    if (!userId || !message) {
        if (msgEls.error) { msgEls.error.textContent = 'Lỗi: Vui lòng điền đầy đủ User ID và nội dung thông báo.'; msgEls.error.style.display = 'block'; }
        return;
    }


    const notificationData = {
        userId: userId,
        message: message
    };

    const result = await callApi(NOTIFICATION_API_BASE_URL, '/notifications/admin/send', 'POST', notificationData, true);

    if (result.ok) {
        if (msgEls.success) { msgEls.success.textContent = 'Gửi thông báo thành công!'; msgEls.success.style.display = 'block'; }
        form.reset();
    } else {
        if (msgEls.error) { msgEls.error.textContent = `Lỗi: ${result.data?.message || result.error || 'Gửi thất bại'}`; msgEls.error.style.display = 'block'; }
    }
}

async function loadPendingSellerRequests() {
    const listEl = document.getElementById('seller-requests-list');
    const loadingEl = document.getElementById('admin-loading-requests');
    const errorEl = document.getElementById('admin-error-requests');

    if (!listEl || !loadingEl || !errorEl) {
        console.error("Các phần tử admin không tìm thấy.");
        return;
    }

    if (getUserRole() !== 'ADMIN') {
        loadingEl.textContent = 'Bạn không có quyền truy cập chức năng này.';
        errorEl.style.display = 'none';
        listEl.style.display = 'none';
        return;
    }

    loadingEl.style.display = 'block';
    errorEl.style.display = 'none';
    listEl.innerHTML = '';

    // SỬ DỤNG ENDPOINT THỰC TẾ: /users/seller-requests
    const result = await callApi(USER_API_BASE_URL, '/users/seller-requests', 'GET', null, true);

    loadingEl.style.display = 'none';

    if (result.ok && Array.isArray(result.data)) {
        const requests = result.data;
        if (requests.length === 0) {
            listEl.innerHTML = '<li>Không có yêu cầu nào đang chờ duyệt.</li>';
        } else {
            requests.forEach(req => {

                const userId = req.userId; // Cần có userId để duyệt
                const li = document.createElement('li');
                li.className = 'admin-request-item';
                li.innerHTML = `
                    <div class="request-info">
                        <strong>User:</strong> ${req.username || 'N/A'} (ID: ${userId}) <br>
                        <strong>Tên Cửa Hàng:</strong> ${req.storeName || 'N/A'} <br>
                        <strong>GPKD:</strong> ${req.businessLicense || 'N/A'} <br>
                        <strong>Ngày Yêu Cầu:</strong> ${req.requestDate ? new Date(req.requestDate).toLocaleDateString('vi-VN') : 'N/A'}
                    </div>
                    <div class="request-actions">
                        <button class="btn btn-success btn-sm btn-approve-request" data-user-id="${userId}">Chấp Thuận</button>
                        <button class="btn btn-danger btn-sm btn-reject-request" data-user-id="${userId}">Từ Chối</button>
                    </div>
                `;
                listEl.appendChild(li);
            });
        }
    } else {
        errorEl.textContent = `Lỗi tải yêu cầu: ${result.data?.message || result.error || 'Lỗi không xác định.'}`;
        errorEl.style.display = 'block';
    }
}

async function handleApproveRequest(userId) {
    if (!confirm(`Bạn có chắc muốn CHẤP THUẬN yêu cầu cho User ID: ${userId}?`)) return;

    const result = await callApi(USER_API_BASE_URL, `/users/approve-seller/${userId}`, 'POST', null, true); // Không cần body theo hình

    if (result.ok) {
        alert('Đã chấp thuận yêu cầu thành công!');
        await loadPendingSellerRequests(); // Tải lại danh sách
    } else {
        alert(`Lỗi chấp thuận: ${result.data?.message || result.error}`);
    }
}
async function openSellerModal(userId) { // <<< THAY ĐỔI TÊN THAM SỐ Ở ĐÂY
    const modal = document.getElementById('sellerInfoModal');
    if (!modal) {
        console.error("Modal #sellerInfoModal không tìm thấy!");
        alert("Không thể mở thông tin cửa hàng.");
        return;
    }

    // Lấy các element trong modal (đảm bảo các ID này tồn tại trong HTML của bạn)
    const storeNameEl = document.getElementById('modal-seller-store-name');
    const ownerNameEl = document.getElementById('modal-seller-owner-name');
    const usernameEl = document.getElementById('modal-seller-username');
    const emailEl = document.getElementById('modal-seller-email');
    const licenseEl = document.getElementById('modal-seller-license');
    const productsEl = document.getElementById('modal-seller-products');

    // Hiển thị modal và trạng thái đang tải
    if (storeNameEl) storeNameEl.textContent = 'Đang tải...';
    if (ownerNameEl) ownerNameEl.textContent = 'Đang tải...';
    if (usernameEl) usernameEl.textContent = 'Đang tải...';
    if (emailEl) emailEl.textContent = 'Đang tải...';
    if (licenseEl) licenseEl.textContent = 'Đang tải...';
    if (productsEl) productsEl.innerHTML = '<p>Đang tải sản phẩm...</p>';
    modal.style.display = 'block';

    // Gọi API lấy thông tin cửa hàng của người bán có userId này
    const result = await callApi(USER_API_BASE_URL, `/users/${userId}/store`, 'GET', null, true);

    if (result.ok && result.data && result.data.result) {
        const storeData = result.data.result; // storeData.userId sẽ là userId của người bán

        // Điền thông tin cửa hàng
        if (storeNameEl) storeNameEl.textContent = storeData.storeName || 'N/A';
        if (ownerNameEl) ownerNameEl.textContent = storeData.name || 'N/A'; // Giả sử API trả về tên người bán trong storeData.name
        if (usernameEl) usernameEl.textContent = storeData.username || 'N/A';
        if (emailEl) emailEl.textContent = storeData.email || 'N/A';
        if (licenseEl) licenseEl.textContent = storeData.businessLicense || 'N/A';

        // Xử lý và hiển thị danh sách sản phẩm của cửa hàng
        if (productsEl) {
            productsEl.innerHTML = '';
            const productList = storeData.products;
            if (productList && Array.isArray(productList) && productList.length > 0) {
                productList.forEach(p => {
                    let imgUrl = p.imageUrl || `https://placehold.co/150x100/EFEFEF/AAAAAA&text=Ảnh`;
                    // Xử lý imgUrl (thay thế localhost, productservice bằng PRODUCT_IMAGE_BASE_URL)
                    if (imgUrl.startsWith('http://productservice')) { // Kiểm tra cụ thể cho productservice
                        imgUrl = imgUrl.replace(/^http:\/\/productservice:\d+/, PRODUCT_IMAGE_BASE_URL);
                    } else if (imgUrl.startsWith('http://localhost:8081')) {
                        imgUrl = imgUrl.replace('http://localhost:8081', PRODUCT_IMAGE_BASE_URL);
                    } else if (!imgUrl.startsWith('http://') && !imgUrl.startsWith('https://') && imgUrl.includes('/')) {
                        // Nếu là đường dẫn tương đối như "/product-images/..."
                        imgUrl = `${PRODUCT_IMAGE_BASE_URL}${imgUrl}`;
                    } else if (!imgUrl.startsWith('http://') && !imgUrl.startsWith('https://') && !imgUrl.includes('/')) {
                        // Nếu chỉ là tên file (cần làm rõ đường dẫn chuẩn từ backend)
                        imgUrl = `${PRODUCT_IMAGE_BASE_URL}/product-images/${imgUrl}`; // Giả định
                    }


                    const productItemDiv = document.createElement('div');
                    productItemDiv.className = 'modal-product-item';
                    // ... (style và innerHTML cho productItemDiv như bạn đã có) ...
                    productItemDiv.innerHTML = `
                        <a href="product-detail.html?id=${p.id}" target="_blank" title="${p.name || ''}">
                           <img src="${imgUrl}" alt="${p.name || 'Sản phẩm'}" style="max-width: 100%; height: 100px; object-fit: cover; margin-bottom: 5px;"
                                onerror="this.onerror=null; this.src='https://placehold.co/150x100/EFEFEF/AAAAAA&text=Ảnh lỗi';">
                           <p style="font-size: 0.9em; margin-bottom: 5px; font-weight: bold; height: 3em; overflow: hidden; text-overflow: ellipsis;">${p.name || 'N/A'}</p>
                        </a>
                        <p style="font-size: 0.85em; color: #e44d26; font-weight: 500;">${(parseFloat(p.price) || 0).toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })}</p>
                    `;
                    productsEl.appendChild(productItemDiv);
                });
            } else {
                productsEl.innerHTML = '<p>Cửa hàng này chưa có sản phẩm nào.</p>';
            }
        }
    } else {
        // Xử lý lỗi tải thông tin cửa hàng
        if (storeNameEl) storeNameEl.textContent = 'Lỗi tải dữ liệu';
        if (ownerNameEl) ownerNameEl.textContent = '';
        if (usernameEl) usernameEl.textContent = '';
        if (emailEl) emailEl.textContent = '';
        if (licenseEl) licenseEl.textContent = '';
        if (productsEl) productsEl.innerHTML = `<p class="error-message">Lỗi tải thông tin cửa hàng: ${result.data?.message || result.error || 'Không thể kết nối'}</p>`;
    }
}
function closeSellerModal() {
    const modal = document.getElementById('sellerInfoModal');
    if (modal) modal.style.display = 'none';
}
/**
 * Lấy thông tin người dùng hiện tại, sau đó lấy storeId và lưu vào localStorage.
 */
async function fetchAndStoreMyStoreId() {
    if (!isLoggedIn()) {
        localStorage.removeItem('userStoreId');
        return null;
    }
    const currentRole = getUserRole();
    const userId = localStorage.getItem('currentUserId');

    if (currentRole !== 'SELLER' || !userId) {
        localStorage.removeItem('userStoreId');
        return null;
    }

    const storeResult = await callApi(USER_API_BASE_URL, `/users/${userId}/store`, 'GET', null, true);
    if (storeResult.ok && storeResult.data?.result?.storeId) {
        const storeId = storeResult.data.result.storeId;
        localStorage.setItem('userStoreId', storeId);
        return storeId;
    }
    localStorage.removeItem('userStoreId'); // Xóa nếu không lấy được
    return null;
}
// ① Admin tạo voucher
// Helper: chuẩn format datetime-local -> yyyy-MM-ddTHH:mm:ss
function normalizeDateTimeInput(value) {
    if (!value) return null;
    // datetime-local thường là '2025-11-01T00:00'
    if (value.length === 16) {
        return value + ':00';
    }
    return value;
}

// ① Admin tạo voucher
// Chuẩn lại datetime-local -> yyyy-MM-ddTHH:mm:ss
function normalizeDateTimeInput(value) {
    if (!value) return null;
    if (value.length === 16) {
        // '2025-11-05T00:00' -> thêm giây
        return value + ':00';
    }
    return value;
}

// Map giá trị trong select loại giảm sang kiểu BE dùng
function mapDiscountType(raw) {
    if (!raw) return null;
    if (raw === '%' || raw === 'PERCENT') return 'PERCENT';
    if (raw === 'AMOUNT' || raw === 'VND' || raw === '₫') return 'AMOUNT';
    return raw;
}

async function handleCreateVoucher(event) {
    event.preventDefault();
    const form = event.target;

    const msgEls = {
        success: document.getElementById('voucher-success-message'),
        error: document.getElementById('voucher-error-message'),
    };
    Object.values(msgEls).forEach((el) => {
        if (el) {
            el.style.display = 'none';
            el.textContent = '';
        }
    });

    // Kiểm tra quyền ở FE (chỉ admin mới thấy trang này)
    if (getUserRole() !== 'ADMIN') {
        if (msgEls.error) {
            msgEls.error.textContent = 'Bạn không có quyền tạo voucher.';
            msgEls.error.style.display = 'block';
        }
        return;
    }

    // Lấy pointCost an toàn
    const pointCostInput = form.pointCost;
    let pointCost = 0;
    if (pointCostInput && pointCostInput.value !== '') {
        const parsed = parseInt(pointCostInput.value, 10);
        pointCost = Number.isNaN(parsed) ? 0 : parsed;
    }

    const data = {
        code: form.code.value.trim(),
        discountValue: parseFloat(form.discountValue.value),
        discountType: mapDiscountType(form.discountType.value),
        quantity: parseInt(form.quantity.value, 10),
        used: 0,
        startDate: normalizeDateTimeInput(form.startDate.value),
        endDate: normalizeDateTimeInput(form.endDate.value),
        status: form.status.value,
        pointCost: pointCost,
    };

    // ❗ Quan trọng: KHÔNG gửi token (requiresAuth = false)
    const result = await callApi(
        VOUCHER_API_BASE_URL,
        '/vouchers',
        'POST',
        data,
        false // <-- không gửi Authorization: Bearer ...
    );

    if (result.ok) {
        if (msgEls.success) {
            msgEls.success.textContent = '✅ Tạo voucher thành công!';
            msgEls.success.style.display = 'block';
        }
        form.reset();

        if (typeof loadVoucherList === 'function') {
            await loadVoucherList();
        }
    } else {
        let message =
            (result.data && result.data.message) ||
            result.error ||
            'Lỗi không xác định.';

        if (msgEls.error) {
            msgEls.error.textContent = '❌ Lỗi: ' + message;
            msgEls.error.style.display = 'block';
        }
    }
}




// ② Xem danh sách voucher (ai cũng xem được)
async function loadAllVouchers() {
    const container = document.getElementById('voucher-list-container');
    if (!container) return;

    container.innerHTML = '<p>Đang tải danh sách voucher...</p>';

    // GET /vouchers – public ⇒ KHÔNG cần token
    const result = await callApi(
        VOUCHER_API_BASE_URL,
        '/vouchers',
        'GET',
        null,
        false         // không gửi Authorization
    );

    if (result.ok && Array.isArray(result.data)) {
        if (result.data.length === 0) {
            container.innerHTML = '<p>Hiện chưa có voucher nào.</p>';
            return;
        }
        let html = `
          <table class="voucher-table">
            <tr>
              <th>Mã</th>
              <th>Giảm</th>
              <th>Loại</th>
              <th>Số lượng</th>
              <th>Trạng thái</th>
            </tr>`;
        result.data.forEach(v => {
            html += `
              <tr>
                <td>${v.code}</td>
                <td>${v.discountValue}${v.discountType === 'PERCENT' ? '%' : '₫'}</td>
                <td>${v.discountType}</td>
                <td>${v.quantity ?? '-'}</td>
                <td>${v.status}</td>
              </tr>`;
        });
        html += '</table>';
        container.innerHTML = html;
    } else {
        container.innerHTML =
            `<p class="error-message">Lỗi tải voucher: ${result.data?.message || result.error}</p>`;
    }
}

// ③ Gửi voucher cho user (Admin)
async function handleIssueVoucher(event) {
    event.preventDefault();
    const form = event.target;

    const msgEls = {
        success: document.getElementById('issue-success-message'),
        error: document.getElementById('issue-error-message')
    };
    Object.values(msgEls).forEach(el => {
        if (el) { el.style.display = 'none'; el.textContent = ''; }
    });

    if (getUserRole() !== 'ADMIN') {
        if (msgEls.error) {
            msgEls.error.textContent = 'Bạn không có quyền gửi voucher.';
            msgEls.error.style.display = 'block';
        }
        return;
    }

    const userId = form.userId.value.trim();
    const voucherCode = form.voucherCode.value.trim();
    const endpoint = `/vouchers/issue/${userId}?code=${encodeURIComponent(voucherCode)}`;

    // POST /vouchers/issue/{userId}?code=... – cần token
    const result = await callApi(
        VOUCHER_API_BASE_URL,
        endpoint,
        'POST',
        null,
        true
    );

    if (result.ok) {
        msgEls.success.textContent = '🎁 Gửi voucher thành công!';
        msgEls.success.style.display = 'block';
        form.reset();
    } else {
        msgEls.error.textContent = `❌ Lỗi: ${result.data?.message || result.error}`;
        msgEls.error.style.display = 'block';
    }
}

// ④ Xem danh sách voucher của user hiện tại
async function loadMyVouchers() {
    const container = document.getElementById('my-voucher-list');
    if (!container) return;

    if (!isLoggedIn()) {
        container.innerHTML = '<p>Bạn cần đăng nhập để xem voucher của mình.</p>';
        return;
    }

    const userId = localStorage.getItem('currentUserId');

    const result = await callApi(
        VOUCHER_API_BASE_URL,
        `/vouchers/user/${userId}`,
        'GET',
        null,
        true
    );

    if (!(result.ok && Array.isArray(result.data))) {
        container.innerHTML = `<p class="error-message">Không thể tải voucher.</p>`;
        return;
    }

    const list = result.data;

    if (list.length === 0) {
        container.innerHTML = '<p>Bạn chưa có voucher nào.</p>';
        return;
    }

    let html = "";

    list.forEach(item => {
        const v = item.voucher;

        // Xử lý giảm giá
        const discountText =
            v.discountType === "PERCENT"
                ? `${v.discountValue}%`
                : `${v.discountValue.toLocaleString('vi-VN')}₫`;

        // Xử lý hạn sử dụng
        const endDate = new Date(v.endDate).toLocaleDateString('vi-VN');

        // Xử lý trạng thái thực tế
        let statusLabel = "Chưa dùng";
        if (item.used === true) {
            statusLabel = "Đã dùng";
        } else {
            const now = new Date();
            const end = new Date(v.endDate);
            if (now > end) statusLabel = "Hết hạn";
        }

        html += `
            <div class="voucher-card">
                <h3>${v.code}</h3>
                <p><strong>Giảm:</strong> ${discountText}</p>
                <p><strong>HSD:</strong> ${endDate}</p>
                <p><strong>Trạng thái:</strong> ${statusLabel}</p>
            </div>
        `;
    });

    container.innerHTML = html;
}


// =============== MINI GAME ===============

// Lấy userId hiện tại từ localStorage
function getCurrentUserIdForMinigame() {
    const token = localStorage.getItem("authToken");
    const userId = localStorage.getItem("currentUserId");

    if (!token || !userId) {
        alert("Vui lòng đăng nhập để chơi MiniGame.");
        return null;
    }
    return userId;
}
function showMinigameResult(title, result) {
    if (!result) {
        alert(`${title}\nKhông nhận được dữ liệu từ server.`);
        return;
    }

    // Backend bạn đang trả về dạng { code, message, result / data }
    const message =
        (typeof result === "string" && result) ||
        result.description ||
        result.message ||
        result.result ||
        JSON.stringify(result);

    alert(`${title}\n\n${message}`);
}
// ----- Điểm danh hằng ngày -----
async function playDailyReward() {
    const userId = getCurrentUserIdForMinigame();
    if (!userId) return;

    try {
        const res = await callApi(
            MINIGAME_API_BASE_URL,
            `/daily-reward/${encodeURIComponent(userId)}`,
            "POST",
            null,
            true
        );
        console.log("Daily reward result:", res);

        if (!res.ok) {
            const msg = res.data?.message || res.error || "Lỗi nhận điểm đăng nhập.";
            alert(`⚠ Lỗi điểm danh: ${msg}`);
            return;
        }

        // res.data là { code, message, result }
        showMinigameResult("🎁 Điểm danh hàng ngày", res.data);
        await loadMinigameHistory(); // reload bảng lịch sử
    } catch (err) {
        console.error("❌ playDailyReward error:", err);
        alert("Có lỗi mạng khi gọi API điểm danh.");
    }
}

// ----- Vòng quay may mắn -----
async function playSpinWheel() {
    const userId = getCurrentUserIdForMinigame();
    if (!userId) return;

    try {
        const res = await callApi(
            MINIGAME_API_BASE_URL,
            `/spin/${encodeURIComponent(userId)}`,
            "POST",
            null,
            true
        );
        console.log("Spin result:", res);

        if (!res.ok) {
            const msg = res.data?.message || res.error || "Lỗi quay vòng may mắn.";
            alert(`⚠ Lỗi vòng quay: ${msg}`);
            return;
        }

        showMinigameResult("🎡 Kết quả vòng quay may mắn", res.data);
        await loadMinigameHistory();
    } catch (err) {
        console.error("❌ playSpinWheel error:", err);
        alert("Có lỗi mạng khi gọi API vòng quay.");
    }
}


// ----- Lật thẻ nhận quà (6 thẻ) -----
async function playCardFlip(choice) {
    const userId = getCurrentUserIdForMinigame();
    if (!userId) return;

    try {
        const res = await callApi(
            MINIGAME_API_BASE_URL,
            `/card-flip-advanced/${encodeURIComponent(userId)}?choice=${encodeURIComponent(choice)}`,
            "POST",
            null,
            true
        );
        console.log("Card flip result:", res);

        if (!res.ok) {
            const msg = res.data?.message || res.error || "Lỗi lật thẻ.";
            alert(`⚠ Lỗi lật thẻ: ${msg}`);
            return;
        }

        showMinigameResult(`🃏 Bạn đã chọn thẻ số ${choice}`, res.data);
        await loadMinigameHistory();
    } catch (err) {
        console.error("❌ playCardFlip error:", err);
        alert("Có lỗi mạng khi gọi API lật thẻ.");
    }
}
// ----- Lịch sử nhận quà -----
async function loadMinigameHistory() {
    const tbody = document.getElementById("minigame-history-body");
    if (!tbody) {
        console.warn("Không tìm thấy tbody lịch sử minigame (#minigame-history-body)");
        return;
    }

    const userId = getCurrentUserIdForMinigame();
    if (!userId) {
        tbody.innerHTML = `<tr><td colspan="4">Bạn cần đăng nhập để xem lịch sử.</td></tr>`;
        return;
    }

    tbody.innerHTML = `<tr><td colspan="4">Đang tải...</td></tr>`;

    try {
        const res = await callApi(
            MINIGAME_API_BASE_URL,
            `/history/${encodeURIComponent(userId)}`,
            "GET",
            null,
            true
        );

        // API của bạn trả về { code, message, result: [...] }
        const list =
            (Array.isArray(res.data?.result) && res.data.result) ||
            (Array.isArray(res.data) && res.data) ||
            [];

        console.log("Minigame history:", { ok: res.ok, status: res.status, data: list });

        if (!res.ok) {
            const msg = res.data?.message || res.error || "Lỗi tải lịch sử.";
            tbody.innerHTML = `<tr><td colspan="4">${msg}</td></tr>`;
            return;
        }

        if (!list.length) {
            tbody.innerHTML = `<tr><td colspan="4">Chưa có lịch sử nhận quà.</td></tr>`;
            return;
        }

        tbody.innerHTML = "";
        list.forEach((item, index) => {
            const tr = document.createElement("tr");
            const created = item.createdAt ? new Date(item.createdAt) : null;

            let typeLabel = "";
            switch (item.type) {
                case "DAILY_REWARD":
                    typeLabel = "Điểm danh hằng ngày";
                    break;
                case "SPIN_WHEEL":
                    typeLabel = "Vòng quay may mắn";
                    break;
                case "CARD_FLIP_ADVANCED":
                case "CARD_FLIP":
                    typeLabel = "Lật thẻ nhận quà";
                    break;
                default:
                    typeLabel = item.type || "Khác";
            }

            tr.innerHTML = `
                <td>${index + 1}</td>
                <td>${typeLabel}</td>
                <td>${item.description || ""}</td>
                <td>${created ? created.toLocaleString("vi-VN") : ""}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("❌ loadMinigameHistory error:", err);
        tbody.innerHTML = `<tr><td colspan="4">Không thể tải lịch sử (lỗi mạng).</td></tr>`;
    }
}
// --------- 5. Khởi tạo trang MiniGame ---------
function initMinigamePage() {
    const page = document.getElementById("minigame-page");
    if (!page) return; // Không phải minigame.html

    console.log("Init MiniGame page...");

    const btnDailyReward = document.getElementById("btn-daily-reward");
    const btnSpinWheel = document.getElementById("btn-spin-wheel");
    const btnReloadHistory = document.getElementById("btn-reload-history");
    const cardButtons = document.querySelectorAll(".btn-card-choice");

    if (btnDailyReward) {
        btnDailyReward.addEventListener("click", (e) => {
            e.preventDefault();
            playDailyReward();
        });
    }

    if (btnSpinWheel) {
        btnSpinWheel.addEventListener("click", (e) => {
            e.preventDefault();
            playSpinWheel();
        });
    }

    cardButtons.forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            const choice = parseInt(btn.dataset.choice, 10);
            if (!isNaN(choice)) {
                playCardFlip(choice);
            }
        });
    });

    if (btnReloadHistory) {
        btnReloadHistory.addEventListener("click", (e) => {
            e.preventDefault();
            loadMinigameHistory();
        });
    }

    // Load lịch sử ngay khi vào trang
    loadMinigameHistory();
}

// =====================================================
// MINI GAME - ĐIỂM & ĐỔI VOUCHER
// APIs:
//  - POST  /minigame/api/v1/rewards/{userId}/add-points?points=200
//  - GET   /minigame/api/v1/rewards/{userId}/summary
//  - POST  /minigame/api/v1/rewards/{userId}/redeem?code=SALE10
// =====================================================

// --- ADMIN cộng điểm cho user ---
async function adminAddPointsForUser(userId, points) {
    if (!userId || !points) {
        alert("Vui lòng nhập userId và số điểm.");
        return;
    }

    const endpoint = `/api/v1/rewards/${encodeURIComponent(userId)}/add-points?points=${encodeURIComponent(points)}`;

    const res = await callApi(
        MINIGAME_API_BASE_URL,
        endpoint,
        "POST",
        null,
        true // ADMIN token
    );

    if (res.ok) {
        alert(`Đã cộng ${points} điểm cho user ${userId}`);
    } else {
        alert(res.data?.message || res.error || "Cộng điểm thất bại.");
    }
}

// --- Lấy tổng điểm của user ---
async function fetchMyMinigameSummary() {
    let userId = localStorage.getItem("currentUserId");
    if (!userId) {
        const user = await loadProfileData();
        if (!user || !user.id) {
            throw new Error("Không xác định được user.");
        }
        userId = user.id;
        localStorage.setItem("currentUserId", userId);
    }

    const endpoint = `/api/v1/rewards/${encodeURIComponent(userId)}/summary`;

    const res = await callApi(
        MINIGAME_API_BASE_URL,
        endpoint,
        "GET",
        null,
        true
    );

    if (!res.ok) throw new Error(res.data?.message || res.error);

    return res.data;
}

// --- Đổi điểm lấy voucher ---
async function redeemVoucherByPoints(code) {
    let userId = localStorage.getItem("currentUserId");
    if (!userId) {
        alert("Bạn cần đăng nhập lại.");
        return;
    }
    if (!code) {
        alert("Vui lòng nhập mã voucher.");
        return;
    }

    const endpoint = `/api/v1/rewards/${encodeURIComponent(userId)}/redeem?code=${encodeURIComponent(code)}`;

    const res = await callApi(
        MINIGAME_API_BASE_URL,
        endpoint,
        "POST",
        null,
        true
    );

    if (res.ok) {
        alert("Đổi voucher thành công!");
        await updateMinigameSummaryUI();
    } else {
        alert(res.data?.message || res.error || "Đổi voucher thất bại.");
    }
}

// --- Cập nhật giao diện điểm ---
async function updateMinigameSummaryUI() {
    const spanPoints = document.getElementById("minigame-current-points");
    const spanUsed = document.getElementById("minigame-used-points");
    const spanVch = document.getElementById("minigame-total-vouchers");

    if (!spanPoints) return;

    try {
        const data = await fetchMyMinigameSummary();

        spanPoints.textContent = data.availablePoints ?? data.totalPoints ?? 0;
        if (spanUsed) spanUsed.textContent = data.usedPoints ?? 0;
        if (spanVch) spanVch.textContent = data.totalVouchers ?? 0;
    } catch (e) {
        spanPoints.textContent = "0";
    }
}

// --- Form đổi voucher ---
async function initMinigameVoucherSection() {
    // Cập nhật lại điểm hiện tại trước
    await updateMinigameSummaryUI();

    const listEl = document.getElementById("minigame-voucher-list");
    const spanPoints = document.getElementById("minigame-current-points");
    if (!listEl || !spanPoints) return;

    // Lấy điểm hiện tại trên UI
    const currentPoints = Number(
        (spanPoints.textContent || "0").replace(/[^\d]/g, "")
    ) || 0;

    listEl.innerHTML = "<p>Đang tải danh sách voucher có thể đổi...</p>";

    try {
        // Dùng API đã có: GET /vouchers (public)
        const result = await callApi(
            VOUCHER_API_BASE_URL,
            "/vouchers",
            "GET",
            null,
            false        // không cần token
        );

        if (!result.ok || !Array.isArray(result.data)) {
            listEl.innerHTML =
                `<p class="error-message">Không thể tải voucher: ${result.data?.message || result.error || "Lỗi không xác định"
                }</p>`;
            return;
        }

        // Lọc các voucher ACTIVE và có pointCost > 0
        const vouchers = result.data.filter(v => {
            const pointCost = Number(v.pointCost ?? v.point_cost ?? 0);
            return v.status === "ACTIVE" && pointCost > 0;
        });

        if (!vouchers.length) {
            listEl.innerHTML =
                "<p>Hiện chưa có voucher nào có thể đổi bằng điểm.</p>";
            return;
        }

        // Render danh sách
        let html = "";
        vouchers.forEach(v => {
            const pointCost = Number(v.pointCost ?? v.point_cost ?? 0);
            const canRedeem = currentPoints >= pointCost;

            const discountText =
                v.discountType === "PERCENT"
                    ? `Giảm ${v.discountValue}%`
                    : `Giảm ${(v.discountValue || 0).toLocaleString("vi-VN")}₫`;

            html += `
              <div class="minigame-voucher-row"
                   style="display:flex; justify-content:space-between; align-items:center;
                          padding:8px 0; border-bottom:1px solid #eee;">
                <div class="minigame-voucher-info">
                  <div><strong>${v.code}</strong> - ${discountText}</div>
                  <div>Yêu cầu: <b>${pointCost}</b> điểm</div>
                </div>
                <button
                  class="btn btn-sm ${canRedeem ? "btn-success" : "btn-secondary"} btn-minigame-redeem"
                  data-code="${v.code}"
                  ${canRedeem ? "" : "disabled"}
                >
                  Đổi
                </button>
              </div>
            `;
        });

        listEl.innerHTML = html;

        // Gắn sự kiện click cho từng nút Đổi
        listEl.querySelectorAll(".btn-minigame-redeem").forEach(btn => {
            btn.addEventListener("click", async () => {
                const code = btn.dataset.code;
                if (!code || btn.disabled) return;

                const ok = confirm(`Bạn muốn đổi voucher ${code}?`);
                if (!ok) return;

                await redeemVoucherByPoints(code); // gọi API minigame /redeem
                // Sau khi đổi xong, load lại điểm + danh sách voucher
                await initMinigameVoucherSection();
            });
        });

    } catch (err) {
        console.error("initMinigameVoucherSection error:", err);
        listEl.innerHTML =
            '<p class="error-message">Không thể tải danh sách voucher (lỗi mạng).</p>';
    }
}
// Seller cập nhật trạng thái đơn hàng
async function sellerUpdateOrderStatus(orderId, newStatus) {
    if (!isLoggedIn() || getUserRole() !== 'SELLER') {
        alert('Chỉ tài khoản Seller mới được phép cập nhật trạng thái đơn hàng.');
        return;
    }

    const endpoint = `/orders/${orderId}/status?status=${encodeURIComponent(newStatus)}`;

    const result = await callApi(
        ORDER_API_BASE_URL,
        endpoint,
        'PATCH', // dùng PATCH cho endpoint của seller
        null,    // không có body
        true     // gửi kèm token
    );

    if (!result.ok) {
        console.error('sellerUpdateOrderStatus error:', result);
        alert(result.data?.message || result.error || 'Cập nhật trạng thái thất bại');
        return;
    }

    alert('Cập nhật trạng thái đơn hàng thành công!');
    // Sau khi thành công, reload lại danh sách đơn của seller nếu cần:
    if (typeof loadSellerOrders === 'function') {
        await loadSellerOrders(currentSellerFilterStatus || null);
    } else {
        location.reload();
    }
}
function renderSellerOrders(orders) {
    const tbody = document.querySelector('#seller-orders-table tbody');
    if (!tbody) return;

    tbody.innerHTML = '';
    orders.forEach(order => {
        const tr = document.createElement('tr');

        const canConfirm = order.status === 'PENDING';
        const canDeliver = order.status === 'CONFIRMED';

        tr.innerHTML = `
            <td>${order.id}</td>
            <td>${order.userId}</td>
            <td>${(order.totalAmount || 0).toLocaleString('vi-VN')} đ</td>
            <td>${order.status}</td>
            <td>
                ${canConfirm ? `
                    <button class="btn btn-sm btn-primary seller-btn-update-status"
                            data-order-id="${order.id}"
                            data-status="CONFIRMED">
                        Xác nhận đơn
                    </button>` : ''}
                ${canDeliver ? `
                    <button class="btn btn-sm btn-success seller-btn-update-status mt-1"
                            data-order-id="${order.id}"
                            data-status="DELIVERED">
                        Đánh dấu đã giao
                    </button>` : ''}
            </td>
        `;

        tbody.appendChild(tr);
    });
}
// ================== SELLER XEM DOANH THU CỬA HÀNG ==================
async function loadSellerRevenue() {
    const warningEl = document.getElementById('seller-revenue-warning');
    const msgEl = document.getElementById('seller-revenue-message');
    const totalEl = document.getElementById('seller-revenue-total');
    const totalOrdersEl = document.getElementById('seller-revenue-total-orders');
    const table = document.getElementById('seller-revenue-table');
    const tbody = document.getElementById('seller-revenue-table-body');
    const startInput = document.getElementById('seller-revenue-start');
    const endInput = document.getElementById('seller-revenue-end');

    if (!warningEl || !msgEl || !totalEl || !totalOrdersEl || !table || !tbody || !startInput || !endInput) {
        console.error('Thiếu element trên trang seller-revenue.html');
        return;
    }

    warningEl.style.display = 'none';
    warningEl.textContent = '';
    msgEl.textContent = 'Đang tải dữ liệu...';
    tbody.innerHTML = '';
    table.style.display = 'none';

    if (!isLoggedIn() || getUserRole() !== 'SELLER') {
        warningEl.textContent = 'Bạn cần đăng nhập với tài khoản Seller để xem doanh thu.';
        warningEl.style.display = 'block';
        msgEl.textContent = '';
        return;
    }

    const start = startInput.value;
    const end = endInput.value;

    if (!start || !end) {
        warningEl.textContent = 'Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc.';
        warningEl.style.display = 'block';
        msgEl.textContent = '';
        return;
    }

    // Lấy storeId của seller từ localStorage (đã lưu lúc login/profile)
    let storeId = localStorage.getItem('userStoreId');
    if (!storeId) {
        // fallback: nếu chưa có thì gọi lại hàm fetch store id (nếu em đã viết)
        try {
            storeId = await fetchAndStoreMyStoreId();
        } catch (e) {
            console.error('fetchAndStoreMyStoreId error', e);
        }
    }

    if (!storeId) {
        warningEl.textContent = 'Không tìm thấy thông tin cửa hàng của bạn. Vui lòng đăng nhập lại.';
        warningEl.style.display = 'block';
        msgEl.textContent = '';
        return;
    }

    const params = new URLSearchParams({
        storeId: storeId,
        start: start,
        end: end
    });

    const endpoint = `/orders/seller/revenue/daily?${params.toString()}`;

    const res = await callApi(
        ORDER_API_BASE_URL,
        endpoint,
        'GET',
        null,
        true // gửi token
    );

    if (!res.ok || !Array.isArray(res.data)) {
        console.error('loadSellerRevenue error:', res);
        warningEl.textContent = res.data?.message || res.error || 'Không thể tải dữ liệu doanh thu.';
        warningEl.style.display = 'block';
        msgEl.textContent = '';
        return;
    }

    const rows = res.data;

    if (rows.length === 0) {
        msgEl.textContent = 'Không có đơn hàng nào trong khoảng thời gian đã chọn.';
        totalEl.textContent = '0 đ';
        totalOrdersEl.textContent = '0';
        tbody.innerHTML = '';
        table.style.display = 'none';
        return;
    }

    // ===== CÓ DỮ LIỆU → RENDER BẢNG + TÍNH TỔNG =====
    let totalRevenue = 0;
    let totalOrders = 0;

    tbody.innerHTML = '';
    table.style.display = '';

    // Debug 1 dòng nếu cần
    // console.log('Sample revenue row:', rows[0]);

    rows.forEach(r => {
        // ----- TÌM FIELD NGÀY -----
        let dateValue = '';
        try {
            const keys = Object.keys(r || {});
            const dateKey = keys.find(k =>
                k.toLowerCase().includes('date') ||   // date, orderDate, revenueDate,...
                k.toLowerCase().includes('day')       // day, orderDay,...
            );

            if (dateKey) {
                dateValue = r[dateKey];
            }

            // Nếu là chuỗi date ISO → format dd/MM/yyyy
            if (dateValue) {
                const d = new Date(dateValue);
                if (!isNaN(d.getTime())) {
                    dateValue = d.toLocaleDateString('vi-VN');
                }
            }
        } catch (e) {
            console.warn('Không đọc được ngày từ record:', r, e);
            dateValue = '';
        }

        // ----- SỐ ĐƠN & DOANH THU -----
        const ordersCount = Number(
            r.totalOrders || r.orderCount || r.ordersCount || r.count || 0
        );
        const revenueVal = Number(
            r.totalRevenue || r.revenue || r.totalAmount || r.amount || 0
        );

        totalOrders += ordersCount;
        totalRevenue += revenueVal;

        // ----- TẠO DÒNG BẢNG -----
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${dateValue || '-'}</td>
            <td>${ordersCount}</td>
            <td>${revenueVal.toLocaleString('vi-VN')} đ</td>
        `;
        tbody.appendChild(tr);
    });

    // Cập nhật 2 ô tổng trên đầu
    totalEl.textContent = totalRevenue.toLocaleString('vi-VN') + ' đ';
    totalOrdersEl.textContent = String(totalOrders);

    msgEl.textContent = `Đã tải ${rows.length} dòng dữ liệu doanh thu.`;
}

// Khởi tạo trang seller-revenue.html
async function initSellerRevenuePage() {
    if (!isLoggedIn() || getUserRole() !== 'SELLER') {
        alert('Bạn cần đăng nhập với tài khoản Seller để xem doanh thu.');
        window.location.href = 'login.html';
        return;
    }

    // set default range: 1 tháng gần nhất
    const startInput = document.getElementById('seller-revenue-start');
    const endInput = document.getElementById('seller-revenue-end');
    const btnLoad = document.getElementById('btn-load-seller-revenue');
    const btnBack = document.getElementById('btn-back-profile');

    if (startInput && endInput) {
        const today = new Date();
        const past = new Date();
        past.setDate(today.getDate() - 30);

        const toIsoDate = (d) => d.toISOString().slice(0, 10);
        if (!startInput.value) startInput.value = toIsoDate(past);
        if (!endInput.value) endInput.value = toIsoDate(today);
    }

    if (btnLoad) {
        btnLoad.addEventListener('click', async (e) => {
            e.preventDefault();
            await loadSellerRevenue();
        });
    }

    if (btnBack) {
        btnBack.addEventListener('click', (e) => {
            e.preventDefault();
            window.location.href = 'profile.html';
        });
    }

    // tự load doanh thu lần đầu
    await loadSellerRevenue();
}
// ===================== VOUCHER SELECTION FOR CHECKOUT =====================

function openVoucherSelectionModal() {
    const modal = document.getElementById('voucherSelectionModal');
    const listEl = document.getElementById('voucher-selection-list');
    const userId = localStorage.getItem("currentUserId");

    if (!modal || !listEl) return;

    if (!isLoggedIn()) {
        alert("Vui lòng đăng nhập để chọn voucher.");
        return;
    }

    modal.style.display = "block";
    listEl.innerHTML = "<p>Đang tải voucher...</p>";

    loadAvailableVouchersForCheckout(userId);
}

function closeVoucherSelectionModal() {
    const modal = document.getElementById('voucherSelectionModal');
    if (modal) modal.style.display = "none";
}

// GỌI API LẤY VOUCHER CÒN DÙNG ĐƯỢC
// GỌI API LẤY VOUCHER CÒN DÙNG ĐƯỢC (checkout)
// GỌI API LẤY VOUCHER CÒN DÙNG ĐƯỢC (trong checkout)
// GỌI API LẤY VOUCHER CÒN DÙNG ĐƯỢC (trong checkout)
async function loadAvailableVouchersForCheckout(userId) {
    const listEl = document.getElementById("voucher-selection-list");
    if (!listEl) return;

    if (!userId) {
        listEl.innerHTML = "<p>Không xác định được user hiện tại.</p>";
        console.warn("loadAvailableVouchersForCheckout: userId = null");
        return;
    }

    const result = await callApi(
        VOUCHER_API_BASE_URL,
        `/vouchers/user/${userId}/available`,
        "GET",
        null,
        true
    );

    console.log("Available vouchers result = ", result);

    if (!(result.ok && Array.isArray(result.data))) {
        listEl.innerHTML = `<p>Không tải được danh sách voucher.</p>`;
        return;
    }

    if (result.data.length === 0) {
        listEl.innerHTML = `<p>Bạn không có voucher khả dụng.</p>`;
        return;
    }

    let html = "";
    result.data.forEach(item => {
        // /available trả về trực tiếp Voucher, còn /user/{id} trả UserVoucher
        const v = item.voucher || item;
        if (!v) return;

        const discountText =
            v.discountType === "PERCENT"
                ? `${v.discountValue}%`
                : `${Number(v.discountValue || 0).toLocaleString("vi-VN")}₫`;

        const endDate = v.endDate
            ? new Date(v.endDate).toLocaleDateString("vi-VN")
            : "-";

        html += `
          <div class="voucher-card">
            <h3>${v.code}</h3>
            <p><strong>Giảm:</strong> ${discountText}</p>
            <p><strong>HSD:</strong> ${endDate}</p>
            <button type="button" class="btn btn-primary"
                    onclick="selectVoucherForCheckout('${v.code}', ${Number(v.discountValue || 0)}, '${v.discountType}')">
              Dùng voucher này
            </button>
          </div>`;
    });

    // Nếu vì lý do gì đó vẫn không render được card
    listEl.innerHTML = html || "<p>Không có voucher phù hợp.</p>";
}



// CHỌN VOUCHER XONG → TỰ ĐIỀN VÀO Ô INPUT
// CHỌN VOUCHER XONG → TỰ ĐIỀN VÀO Ô INPUT + TÍNH GIẢM GIÁ
async function selectVoucherForCheckout(code) {
    const input = document.getElementById("checkout-voucher-code");
    const msgEl = document.getElementById("selected-voucher-message");

    // Gán code vào ô input + text “Đã chọn voucher…”
    if (input) input.value = code;
    if (msgEl) msgEl.textContent = `Đã chọn voucher: ${code}`;

    // Đóng modal chọn voucher (nếu có)
    const modal = document.getElementById("voucher-selection-modal");
    if (modal) modal.style.display = "none";

    const userId = localStorage.getItem("currentUserId");
    if (!userId) {
        window.appliedVoucherDiscount = 0;
        await displayCheckoutSummary();
        alert("Không tìm thấy thông tin người dùng, không thể áp dụng voucher.");
        return;
    }

    // Tổng tiền hiện tại của đơn (chưa giảm)
    const orderAmount = Number(window.checkoutTotalAmount || 0);

    try {
        const applyResult = await callApi(
            VOUCHER_API_BASE_URL,
            `/vouchers/apply?userId=${encodeURIComponent(userId)}&code=${encodeURIComponent(code)}&orderAmount=${orderAmount}`,
            "GET",
            null,
            true
        );

        console.log("APPLY VOUCHER result =", applyResult);

        if (applyResult.ok && applyResult.data) {
            const discountAmount = Number(applyResult.data.discountAmount || 0);
            window.appliedVoucherDiscount = discountAmount;
        } else {
            window.appliedVoucherDiscount = 0;
            alert(applyResult.data?.message || "Voucher không hợp lệ hoặc đã dùng!");
        }
    } catch (err) {
        console.error("Lỗi khi apply voucher:", err);
        window.appliedVoucherDiscount = 0;
        alert("Không áp dụng được voucher, vui lòng thử lại.");
    }

    // Cập nhật lại phần tóm tắt đơn hàng
    await displayCheckoutSummary();
}

// để HTML onclick="selectVoucherForCheckout('SALE10')" dùng được
window.selectVoucherForCheckout = selectVoucherForCheckout;



// --- Auto init ---
document.addEventListener("DOMContentLoaded", async () => {
    const page = window.location.pathname.split("/").pop();

    // Trang minigame
    if (page === "minigame.html") {
        await initMinigameVoucherSection();
    }

    // Form admin cộng điểm
    const adminForm = document.getElementById("admin-add-points-form");
    if (adminForm) {
        adminForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const userId = document.getElementById("admin-points-userId").value;
            const points = Number(document.getElementById("admin-points-value").value);
            await adminAddPointsForUser(userId, points);
        });
    }
});
document.addEventListener('click', async (e) => {
    const btn = e.target.closest('.seller-btn-update-status');
    if (!btn) return;

    const orderId = btn.dataset.orderId;
    const newStatus = btn.dataset.status;
    if (!orderId || !newStatus) return;

    const ok = confirm(`Bạn có chắc muốn chuyển đơn #${orderId} sang trạng thái ${newStatus}?`);
    if (!ok) return;

    await sellerUpdateOrderStatus(orderId, newStatus);
});
// ====================== HIỂN THỊ ĐÁNH GIÁ SẢN PHẨM ========================
async function loadProductRatings(productId) {
    const res = await callApi(PRODUCT_API_BASE_URL, `/products/${productId}/ratings`, "GET", null, true);

    const ratingListBox = document.getElementById("rating-list");
    const summaryBox = document.getElementById("rating-summary");

    if (!ratingListBox || !summaryBox) return;

    if (!res.ok) {
        summaryBox.innerHTML = "<p class='error'>Không thể tải đánh giá.</p>";
        return;
    }

    const list = res.data.result || [];
    const currentUser = localStorage.getItem("username");

    // ====== Không có đánh giá ======
    if (list.length === 0) {
        summaryBox.innerHTML = `<p>Chưa có đánh giá nào.</p>`;
        ratingListBox.innerHTML = "";
        return;
    }

    // ====== Summary rating ======
    const avg = (list.reduce((sum, r) => sum + r.ratingValue, 0) / list.length).toFixed(1);
    summaryBox.innerHTML = `
        <strong>${avg} / 5 ⭐</strong> • ${list.length} lượt đánh giá
    `;

    // ====== Render từng đánh giá ======
    let html = "";
    list.forEach(r => {
        const stars = "★".repeat(r.ratingValue) + "☆".repeat(5 - r.ratingValue);
        const isOwner = r.username === currentUser;

        html += `
            <div class="rating-card">
                <div class="rating-info">
                    <div class="rating-username">${r.username}</div>
                    <div class="rating-stars">${stars}</div>
                    <div class="rating-comment">${r.comment}</div>
                    <div class="rating-date">${new Date(r.createdAt).toLocaleString("vi-VN")}</div>
                </div>

                ${isOwner ? `
                    <button 
                        class="btn-delete-rating" 
                        data-rating-id="${r.id}"
                        data-product-id="${productId}"
                    >
                        Xóa
                    </button>`
                : ""}
            </div>
        `;
    });

    ratingListBox.innerHTML = html;

    // ====== Gắn sự kiện xoá ======
    document.querySelectorAll(".btn-delete-rating").forEach(btn => {
        btn.addEventListener("click", handleDeleteRating);
    });
}

//tóm tắt số sao trung bình
async function loadRatingSummary(productId) {
    const summaryBox = document.getElementById("rating-summary");

    const res = await callApi(PRODUCT_API_BASE_URL, `/products/${productId}/rating-summary`, "GET", null, true);

    if (!res.ok) {
        summaryBox.textContent = "Không thể tải đánh giá.";
        return;
    }

    const s = res.data.result;

    summaryBox.innerHTML = `
        ⭐ <strong>${s.averageRating.toFixed(1)}</strong> / 5  
        • ${s.totalRatings} lượt đánh giá
    `;
}
// ====================== XÓA ĐÁNH GIÁ ========================
async function handleDeleteRating(e) {
    const ratingId = e.target.dataset.ratingId;
    const productId = e.target.dataset.productId;

    if (!confirm("Bạn chắc chắn muốn xóa đánh giá này?")) return;

    const res = await callApi(
        PRODUCT_API_BASE_URL,
        `/products/${productId}/my/${ratingId}`,
        "DELETE",
        null,
        true
    );

    if (res.ok) {
        alert("Đã xóa đánh giá!");

        // Reload UI tại my-ratings.html
        if (window.location.pathname.includes("my-ratings.html")) {
            loadMyRatings();
        }

        // Reload UI tại product-detail
        if (window.location.pathname.includes("product-detail.html")) {
            loadProductRatings(productId);
            loadRatingSummary(productId);
        }

    } else {
        alert(res.data?.message || "Không thể xóa đánh giá!");
    }
}
//danh sách đánh giá 
// ====================== LẤY DANH SÁCH ĐÁNH GIÁ CỦA TÔI ======================
async function loadMyRatings() {
    const container = document.getElementById("my-ratings-box");
    container.innerHTML = "Đang tải...";

    const res = await callApi(
        PRODUCT_API_BASE_URL,
        "/products/my-ratings",
        "GET",
        null,
        true
    );

    if (!res.ok) {
        container.innerHTML = "<p>Không thể tải dữ liệu.</p>";
        return;
    }

    const list = res.data.result;
    if (list.length === 0) {
        container.innerHTML = "<p>Bạn chưa đánh giá sản phẩm nào.</p>";
        return;
    }

    const prodDetails = {};
    const prodPromises = list.map(r => {
        if (!prodDetails[r.productId]) {
            prodDetails[r.productId] = { name: "Sản phẩm #" + r.productId, img: "images/default-avatar.png" };
            return callApi(PRODUCT_API_BASE_URL, `/products/${r.productId}`, "GET", null, false)
                .then(pRes => {
                    if (pRes.ok && pRes.data) {
                        const product = pRes.data.result || pRes.data;
                        prodDetails[r.productId] = {
                            name: product.name || "Sản phẩm #" + r.productId,
                            img: product.imageUrl || "images/default-avatar.png"
                        };
                    }
                }).catch(e => console.error("Err fetch product", e));
        }
        return Promise.resolve();
    });

    await Promise.all(prodPromises);

    let html = "";
    list.forEach(r => {
        const prod = prodDetails[r.productId];
        let pName = prod.name;
        let pImg = prod.img;

        if (pImg && pImg.startsWith("/api")) {
            pImg = PRODUCT_IMAGE_BASE_URL.replace("/images", "") + pImg;
        } else if (pImg && !pImg.startsWith("http")) {
            pImg = `${PRODUCT_IMAGE_BASE_URL}/${pImg}`;
        }

        const stars = "★".repeat(r.ratingValue) + "☆".repeat(5 - r.ratingValue);

        html += `
            <div class="rating-item" style="display:flex; gap:16px; align-items:flex-start; border-bottom:1px solid #eee; padding-bottom:16px; margin-bottom:16px;">
                <img src="${pImg}" style="width: 70px; height: 70px; object-fit: cover; border-radius: 8px; border: 1px solid #ddd;" onerror="this.src='images/default-avatar.png'">
                <div style="flex:1;">
                    <strong style="display:block; margin-bottom: 6px; font-size: 1.05em; color: #333;">${pName}</strong>
                    <div class="rating-stars" style="margin-bottom: 6px;">${stars}</div>
                <p>${r.comment}</p>
                <small style="color:#777">${new Date(r.createdAt).toLocaleString()}</small>
                <div style="margin-top: 8px;">
                    <button class="edit-btn" 
                            data-product-id="${r.productId}" 
                            data-rating-value="${r.ratingValue}"
                            data-rating-comment="${(r.comment || '').replace(/"/g, '&quot;')}">
                        Sửa đánh giá
                    </button>
                    <button class="delete-btn" 
                            data-product-id="${r.productId}" 
                            data-rating-id="${r.id}">
                        Xóa đánh giá
                    </button>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;

    // GẮN SỰ KIỆN XÓA
    document.querySelectorAll(".delete-btn").forEach(btn => {
        btn.addEventListener("click", handleDeleteRating);
    });

    // GẮN SỰ KIỆN SỬA
    document.querySelectorAll(".edit-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            const btnEl = e.target.closest(".edit-btn");
            const prodId = btnEl.dataset.productId;
            reviewingProductId = prodId; // Global variable

            document.getElementById("review-stars").value = btnEl.dataset.ratingValue;
            document.getElementById("review-comment").value = btnEl.dataset.ratingComment;

            // LƯU LUÔN VÀO MODAL GÓC ĐỂ FIX BUG CHẮC CHẮN 100%
            const modal = document.getElementById("review-modal");
            modal.dataset.productId = prodId;
            modal.dataset.isEdit = "true";
            modal.style.display = "flex";
        });
    });
}
function initProductRatingUI(productId) {
    console.log("Init Rating UI cho sản phẩm:", productId);

    // Tải tổng quan đánh giá
    loadRatingSummary(productId);

    // Tải danh sách đánh giá
    loadProductRatings(productId);
}
// Helper: hiển thị datetime ra tiếng Việt
function formatDateTimeVi(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    return d.toLocaleString('vi-VN');
}

// Danh sách voucher cho trang Admin (bảng có ID/Mã/Số lượng/Đã dùng/Ngày...)
async function loadVoucherList() {
    const tbody = document.getElementById('voucher-table-body');
    if (!tbody) return;

    tbody.innerHTML = `
      <tr><td colspan="9">⏳ Đang tải danh sách voucher...</td></tr>
    `;

    const result = await callApi(
        VOUCHER_API_BASE_URL,
        '/vouchers',
        'GET',
        null,
        false // nếu endpoint public; nếu BE bắt buộc auth thì đổi thành true
    );

    if (result.ok && Array.isArray(result.data)) {
        if (result.data.length === 0) {
            tbody.innerHTML = `
              <tr><td colspan="9">Chưa có voucher nào.</td></tr>
            `;
            return;
        }

        tbody.innerHTML = '';
        result.data.forEach(v => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
              <td>${v.id ?? ''}</td>
              <td>${v.code ?? ''}</td>
              <td>${v.discountType ?? ''}</td>
              <td>${v.discountValue ?? ''}</td>
              <td>${v.quantity ?? 0}</td>
              <td>${v.used ?? 0}</td>
              <td>${formatDateTimeVi(v.startDate)}</td>
              <td>${formatDateTimeVi(v.endDate)}</td>
              <td>${v.status ?? ''}</td>
            `;
            tbody.appendChild(tr);
        });
    } else {
        tbody.innerHTML = `
          <tr>
            <td colspan="9" class="error-message">
              Lỗi tải voucher: ${result.data?.message || result.error || 'Không thể kết nối server.'}
            </td>
          </tr>
        `;
    }
}
async function initEditAccountPage() {
    const form = document.getElementById('edit-account-form');
    const msgEl = document.getElementById('edit-account-message');

    if (!form) return;

    // Reset message
    if (msgEl) {
        msgEl.textContent = '';
        msgEl.style.display = 'none';
    }

    if (!isLoggedIn()) {
        if (msgEl) {
            msgEl.textContent = 'Bạn cần đăng nhập để chỉnh sửa tài khoản.';
            msgEl.className = 'error-message';
            msgEl.style.display = 'block';
        }
        return;
    }

    // Lấy thông tin hiện tại để fill form
    const user = await loadProfileData(); // đã có sẵn trong dự án
    if (user) {
        if (form.elements['name']) {
            form.elements['name'].value = user.name || '';
        }
        if (form.elements['email']) {
            form.elements['email'].value = user.email || '';
        }
        if (form.elements['dob']) {
            // user.dob dạng ISO -> yyyy-MM-dd
            const dobStr = user.dob ? String(user.dob).substring(0, 10) : '';
            form.elements['dob'].value = dobStr;
        }
        // Nếu sau này BE có phone thì fill tương tự
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (msgEl) {
            msgEl.textContent = '';
            msgEl.style.display = 'none';
        }

        const userId = localStorage.getItem('currentUserId');
        if (!userId) {
            if (msgEl) {
                msgEl.textContent = 'Không tìm thấy ID người dùng. Vui lòng đăng nhập lại.';
                msgEl.className = 'error-message';
                msgEl.style.display = 'block';
            }
            return;
        }

        const name = form.elements['name'].value.trim();
        const email = form.elements['email'].value.trim();
        const dob = form.elements['dob'].value; // yyyy-MM-dd
        const newPassword = form.elements['password'].value.trim();

        // Tạo payload đúng format Postman
        const payload = {
            name: name || null,
            email: email || null,
            dob: dob || null
        };

        if (newPassword) {
            payload.password = newPassword;
        }

        const result = await callApi(
            USER_API_BASE_URL,
            `/users/${userId}`,
            'PUT',
            payload,
            true // cần token
        );

        if (result.ok) {
            if (msgEl) {
                msgEl.textContent = '✅ Cập nhật tài khoản thành công!';
                msgEl.className = 'success-message';
                msgEl.style.display = 'block';
            }
            // cập nhật lại profile trong localStorage
            await loadProfileData();
        } else {
            if (msgEl) {
                msgEl.textContent =
                    '❌ Lỗi: ' +
                    (result.data?.message || result.error || 'Không thể cập nhật tài khoản.');
                msgEl.className = 'error-message';
                msgEl.style.display = 'block';
            }
        }
    });
}

// =======================================================================================
// START OF UPDATED DOMContentLoaded LISTENER
// =======================================================================================
document.addEventListener('DOMContentLoaded', async () => {
    // Phần khởi tạo chung ở đầu DOMContentLoaded
    const currentYearEl = document.getElementById('current-year');
    if (currentYearEl) currentYearEl.textContent = new Date().getFullYear();

    await loadCategoriesAndBuildMap(); // Tải danh mục trước

    // Các hàm này cần chạy sau loadProfileData và fetchAndStoreMyStoreId nếu có logic phụ thuộc
    // vào userRole hoặc userStoreId (ví dụ: updateNav)
    if (isLoggedIn()) {
        await loadProfileData();
        await fetchAndStoreMyStoreId(); // Lấy storeId nếu là Seller
    }

    populateCategorySidebar(); // Điền sidebar sau khi có categoryMap
    updateNav(); // Cập nhật nav dựa trên trạng thái login và role
    setActiveNavLink(); // Đặt active link cho navigation

    // Tải dữ liệu giỏ hàng và thông báo sau khi các thông tin cơ bản đã sẵn sàng
    if (isLoggedIn()) {
        await fetchCartData();
        await fetchMyNotifications();
    } else {
        updateCartUI(null); // Đảm bảo UI giỏ hàng trống nếu chưa login
        updateNotificationUI(null); // Đảm bảo UI thông báo trống nếu chưa login
    }

    const page = window.location.pathname.split("/").pop() || "index.html";

    // --- LOGIC CHO CATEGORY SIDEBAR (Tách riêng ra để dễ quản lý) ---
    const hamburgerButtonCategory = document.getElementById('hamburger-menu-button');
    const categorySidebar = document.getElementById('category-sidebar');
    const closeSidebarButtonCategory = document.getElementById('close-sidebar-button');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    function openCategorySidebar() {
        if (categorySidebar) categorySidebar.classList.add('sidebar-visible');
        if (sidebarOverlay) {
            sidebarOverlay.style.display = 'block';
            requestAnimationFrame(() => {
                sidebarOverlay.classList.add('active');
            });
        }
        document.body.classList.add('sidebar-open');
    }

    function closeCategorySidebar() {
        if (categorySidebar) categorySidebar.classList.remove('sidebar-visible');
        if (sidebarOverlay) {
            sidebarOverlay.classList.remove('active');
            setTimeout(() => {
                if (!sidebarOverlay.classList.contains('active')) {
                    sidebarOverlay.style.display = 'none';
                }
            }, 300);
        }
        document.body.classList.remove('sidebar-open');
    }

    if (hamburgerButtonCategory) {
        hamburgerButtonCategory.addEventListener('click', (e) => {
            if (categorySidebar && categorySidebar.classList.contains('sidebar-visible')) {
                closeCategorySidebar();
            } else {
                openCategorySidebar();
            }
        });
    }

    if (closeSidebarButtonCategory) {
        closeSidebarButtonCategory.addEventListener('click', closeCategorySidebar);
    }

    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', closeCategorySidebar);
    }

    if (categorySidebar) {
        const sidebarLinks = categorySidebar.querySelectorAll('#category-sidebar-list a');
        sidebarLinks.forEach(link => {
            link.addEventListener('click', closeCategorySidebar);
        });
    }
    // --- KẾT THÚC LOGIC CATEGORY SIDEBAR ---


    // Event listeners chung (luôn chạy)
    const loginForm = document.getElementById('loginForm');
    if (loginForm) loginForm.addEventListener('submit', handleLogin);
    const registerForm = document.getElementById('registerForm');
    if (registerForm) registerForm.addEventListener('submit', handleRegister);
    const addProductForm = document.getElementById('addProductForm');
    if (addProductForm) addProductForm.addEventListener('submit', handleAddProduct);
    const sendNotificationForm = document.getElementById('sendNotificationForm');
    if (sendNotificationForm) sendNotificationForm.addEventListener('submit', sendAdminNotification);
    const logoutBtn = document.getElementById('nav-logout');
    const logoutLink = document.querySelector('#nav-logout a');
    if (logoutBtn) logoutBtn.addEventListener('click', (e) => { e.preventDefault(); handleLogout(); });
    if (logoutLink) { logoutLink.addEventListener('click', (e) => { e.preventDefault(); handleLogout(); }); }


    // --- Logic cụ thể cho từng trang (Bắt đầu bằng IF) ---
    if ((page === 'index.html' || page === '') && document.getElementById('product-grid-home')) {
        loadProducts('product-grid-home', { size: 8 });
    }
    else if (page === 'admin.html') {
        if (!isLoggedIn() || getUserRole() !== 'ADMIN') {
            const mainContent = document.getElementById('admin-main-content');
            if (mainContent) {
                mainContent.innerHTML = `<p class="error-message" style="text-align:center; padding: 50px;">Bạn cần đăng nhập với quyền Admin để vào trang này.</p>`;
                const adminNav = document.getElementById('admin-sub-nav');
                if (adminNav) adminNav.style.display = 'none';
            } else {
                const container = document.getElementById('seller-requests-section') || document.body;
                container.innerHTML = `<h2>Yêu Cầu Seller</h2><p class="error-message">Bạn cần đăng nhập với quyền Admin để vào trang này.</p>`;
            }
        } else {
            // Code admin.js sẽ tự xử lý việc load request nếu admin.js được include và chạy.
            // Nếu loadPendingSellerRequests được gọi từ admin.js, không cần gọi lại ở đây.
            // Nếu bạn muốn nó chạy từ đây, bạn có thể gọi: await loadPendingSellerRequests();
            console.log("Đang ở trang admin và đã đăng nhập.");
        }
    }
    else if (page === 'profile.html') {
        await displayProfileOnPage();

        const btnViewMyStore = document.getElementById('btn-view-my-store');
        if (btnViewMyStore) {
            btnViewMyStore.addEventListener('click', handleViewMyStore);
        }
        const btnAddProductMyStore = document.getElementById('btn-add-product-my-store');
        if (btnAddProductMyStore) {
            btnAddProductMyStore.addEventListener('click', () => {
                window.location.href = 'add-product.html';
            });
        }
        // Listener cho modal đăng ký bán hàng (nếu có)
        const sellerModal = document.getElementById('sellerRequestModal');
        const openBtn = document.getElementById('openSellerRequestModalBtn');
        const closeBtn = document.getElementById('closeSellerRequestModalBtn'); // Lấy nút đóng bằng ID mới
        const sellerForm = document.getElementById('sellerRequestForm');

        if (openBtn && sellerModal) {
            openBtn.addEventListener('click', () => sellerModal.style.display = 'block');
        }
        if (closeBtn && sellerModal) { // Sử dụng ID 'closeSellerRequestModalBtn'
            closeBtn.addEventListener('click', () => sellerModal.style.display = 'none');
        }
        if (sellerForm) {
            sellerForm.addEventListener('submit', handleSellerRegistrationRequest);
        }
        // Listener đóng modal khi click ra ngoài đã được xử lý ở global event listeners bên dưới.
    }
    else if (page === 'edit-account.html') {
        await initEditAccountPage();
    }
    else if (page === 'products.html' && document.getElementById('product-grid-all')) {
        const filterForm = document.getElementById('filterForm');

        // 1) Luôn load danh mục vào select
        await loadCategoriesAndBuildMap('filter-categoryId');

        let currentPage = 0;

        if (filterForm) {
            const urlParams = new URLSearchParams(window.location.search);

            // 2) Đổ các query param vào form (tên input trùng name)
            urlParams.forEach((value, key) => {
                const input = filterForm.elements[key];
                if (input) {
                    input.value = value;
                    if (key === 'page') {
                        currentPage = parseInt(value, 10) || 0;
                    }
                }
            });

            // 3) Gọi load sản phẩm với filter hiện tại
            applyFiltersAndLoad(currentPage);

            // 4) Submit form để lọc
            filterForm.addEventListener('submit', (e) => {
                e.preventDefault();
                applyFiltersAndLoad(0);
            });

            // 5) Nút reset bộ lọc
            const resetBtn = document.getElementById('resetFiltersBtn');
            if (resetBtn) {
                resetBtn.addEventListener('click', () => {
                    filterForm.reset();
                    // Xoá query trên URL
                    window.history.replaceState({}, '', window.location.pathname);
                    applyFiltersAndLoad(0);
                });
            }
        } else {
            // Fallback: chỉ load trang 1 nếu không có form
            loadProducts('product-grid-all', { page: 0 });
        }
    }
    else if (page === 'seller-revenue.html') {
        await initSellerRevenuePage();
    }
    else if (page === 'my-ratings.html') {
        console.log("📌 Loading MY RATINGS page...");
        loadMyRatings();
    }

    else if (page === 'product-detail.html') {
        console.log("📌 Loading PRODUCT DETAIL page...");

        const productId = getParam("id");
        if (productId) {
            // Thứ tự phải đúng:
            await loadProductDetail();        // 1. tải chi tiết sản phẩm
            loadRatingSummary(productId);     // 2. tải tổng sao
            loadProductRatings(productId);    // 3. tải danh sách đánh giá
        } else {
            console.error("❌ Không tìm thấy productId trong URL!");
        }
    }
    else if (page === 'store.html') {
        await loadStorePage();
    }
    else if (page === 'add-product.html') {
        if (isLoggedIn() && (getUserRole() === 'ADMIN' || getUserRole() === 'SELLER')) {
            // Đảm bảo loadCategoriesAndBuildMap đã chạy ở đầu
            const productCategorySelect = document.getElementById('product-category-id');
            if (!productCategorySelect.options.length <= 1) {
                await loadCategoriesAndBuildMap('product-category-id');
            }
            setupImagePreview('product-image-file', 'product-image-preview', 'add-file-chosen-text');
        } else {
            const formContainer = document.querySelector('section.form-container.add-product-form-container') || document.querySelector('main.container section.form-container');
            if (formContainer) formContainer.innerHTML = `<h2>Thêm Sản Phẩm</h2><p class="error-message">Bạn cần <a href="login.html">đăng nhập</a> với quyền Admin hoặc Seller để thực hiện chức năng này. <a href="index.html">Quay lại trang chủ</a>.</p>`;
        }
    }
    else if (page === 'edit-product.html') {
        const pageContainer = document.getElementById('edit-product-container');
        const editFormElement = document.getElementById('editProductForm');

        if (!pageContainer || !editFormElement) {
            console.error("Thiếu container hoặc form cho trang edit-product.");
            if (pageContainer) pageContainer.innerHTML = "<p class='error-message'>Lỗi cấu trúc trang.</p>";
            return;
        }

        editFormElement.style.display = 'none';
        pageContainer.style.display = 'block';

        if (isLoggedIn() && (getUserRole() === 'ADMIN' || getUserRole() === 'SELLER')) {
            const productId = new URLSearchParams(window.location.search).get('id');
            if (productId) {
                // loadProductForEdit sẽ tải categories nếu cần và xử lý hiển thị form/lỗi
                const canEdit = await loadProductForEdit(productId);

                if (canEdit) {
                    setupImagePreview('edit-product-image-file', 'new-product-image-preview', 'file-chosen-text');
                    if (!editFormElement.dataset.listenerAttached) {
                        editFormElement.addEventListener('submit', handleUpdateProduct);
                        editFormElement.dataset.listenerAttached = 'true';
                    }
                }
            } else {
                pageContainer.innerHTML = `<h2>Chỉnh Sửa Sản Phẩm</h2><p class="error-message">ID sản phẩm không hợp lệ. <a href="products.html">Quay lại</a>.</p>`;
            }
        } else {
            pageContainer.innerHTML = `<h2>Chỉnh Sửa Sản Phẩm</h2><p class="error-message">Bạn cần <a href="login.html">đăng nhập</a> với quyền ${getUserRole() === 'ADMIN' ? 'Admin' : 'Seller'} để thực hiện chức năng này. <a href="products.html">Quay lại</a>.</p>`;
        }
    }
    else if (page === 'cart.html') {
        const cartGrid = document.getElementById('cart-grid');
        if (cartGrid) {
            cartGrid.addEventListener('change', async e => {
                if (e.target.classList.contains('cart-item-quantity-input-api')) {
                    await updateCartItemQuantityAPI(e.target.dataset.productId, e.target.value);
                }
            });

            cartGrid.addEventListener('click', async e => {
                const target = e.target;

                if (target.classList.contains('btn-remove-from-cart-api')) {
                    await removeCartItemAPI(target.dataset.productId);
                    return;
                }

                // 🔹 Thanh toán 1 món
                if (target.classList.contains('btn-checkout-single')) {
                    if (!isLoggedIn()) {
                        alert("Bạn cần đăng nhập để thanh toán.");
                        window.location.href = 'login.html';
                        return;
                    }

                    const productId = target.dataset.productId;
                    if (!productId) {
                        alert("ID sản phẩm không hợp lệ.");
                        return;
                    }

                    // Lấy số lượng hiện tại trong ô input
                    const row = target.closest('tr');
                    let quantity = 1;
                    const qtyInput = row ? row.querySelector('.cart-item-quantity-input-api') : null;
                    if (qtyInput) {
                        quantity = parseInt(qtyInput.value, 10) || 1;
                    }

                    const checkoutItemsPayload = [{
                        productId: Number(productId),
                        quantity: quantity
                    }];
                    localStorage.setItem(CHECKOUT_ITEMS_KEY, JSON.stringify(checkoutItemsPayload));

                    window.location.href = 'checkout.html';
                }

            });
        }

        const cartSummaryEl = document.getElementById('cart-summary');
        if (cartSummaryEl) {
            cartSummaryEl.addEventListener('click', async (e) => {
                const target = e.target;

                // 🟢 Thanh toán các sản phẩm đã tích chọn
                if (target.classList.contains('btn-checkout-selected')) {
                    if (!isLoggedIn()) {
                        alert("Bạn cần đăng nhập để thanh toán.");
                        window.location.href = 'login.html';
                        return;
                    }

                    // Lấy tất cả checkbox đã tích
                    const checked = document.querySelectorAll('.cart-item-select-checkbox:checked');
                    if (checked.length === 0) {
                        alert("Vui lòng chọn ít nhất một sản phẩm để thanh toán.");
                        return;
                    }

                    const selectedItems = [];
                    checked.forEach(cb => {
                        const productId = cb.dataset.productId;
                        const row = cb.closest('tr');
                        const qtyInput = row ? row.querySelector('.cart-item-quantity-input-api') : null;
                        const quantity = qtyInput ? parseInt(qtyInput.value, 10) || 1 : 1;

                        if (productId) {
                            selectedItems.push({
                                productId: Number(productId),
                                quantity: quantity
                            });
                        }
                    });

                    if (selectedItems.length === 0) {
                        alert("Không có sản phẩm hợp lệ được chọn.");
                        return;
                    }

                    // Lưu vào localStorage để checkout.html đọc
                    localStorage.setItem(CHECKOUT_ITEMS_KEY, JSON.stringify(selectedItems));
                    window.location.href = 'checkout.html';
                    return; // ⬅️ không chạy xuống phần "thanh toán tất cả"
                }

                // 🔵 Thanh toán tất cả (giữ logic cũ)
                if (target.classList.contains('btn-checkout')) {
                    if (!isLoggedIn()) {
                        alert("Bạn cần đăng nhập để thanh toán.");
                        window.location.href = 'login.html';
                        return;
                    }

                    const currentCart = await fetchCartData();
                    if (!currentCart || !currentCart.items || currentCart.items.length === 0) {
                        alert("Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm.");
                        return;
                    }

                    // Thanh toán tất cả: xoá selection → checkout hiểu là toàn bộ giỏ
                    localStorage.removeItem(CHECKOUT_ITEMS_KEY);

                    window.location.href = 'checkout.html';
                }
            });
        }

        const btnClearCart = document.getElementById('btn-clear-cart');
        if (btnClearCart) {
            btnClearCart.addEventListener('click', () => handleClearCart(true));
        }
    }
    else if (page === 'checkout.html') {
        if (!isLoggedIn()) {
            alert("Bạn cần đăng nhập để tiến hành thanh toán.");
            window.location.href = 'login.html';
            return;
        }

        const checkoutForm = document.getElementById('checkoutForm');
        const sameAsShippingCheckbox = document.getElementById('sameAsShipping');
        const billingAddressFieldset = document.getElementById('billingAddressFieldset');
        const checkoutErrorMessage = document.getElementById('checkout-error-message');
        const checkoutCartSummaryEl = document.getElementById('checkout-cart-summary');

        async function displayCheckoutSummary() {
            if (!checkoutCartSummaryEl) return;

            const cartData = await fetchCartData();
            if (!cartData || !cartData.items || cartData.items.length === 0) {
                checkoutCartSummaryEl.innerHTML =
                    '<p>Giỏ hàng của bạn đang trống. <a href="products.html">Tiếp tục mua sắm</a>.</p>';
                const submitButton = checkoutForm?.querySelector('button[type="submit"]');
                if (submitButton) submitButton.disabled = true;
                return;
            }

            // Đọc danh sách item được chọn từ localStorage (nếu có)
            let selectedItems = [];
            try {
                const raw = localStorage.getItem(CHECKOUT_ITEMS_KEY);
                if (raw) selectedItems = JSON.parse(raw);
            } catch (e) {
                console.warn("Không parse được CHECKOUT_ITEMS_KEY:", e);
            }

            let itemsToDisplay = cartData.items;
            let total = parseFloat(cartData.grandTotal) || 0;

            // Nếu có selectedItems → chỉ hiển thị & tính tiền cho subset đó
            if (selectedItems && Array.isArray(selectedItems) && selectedItems.length > 0) {
                const selectedMap = new Map(
                    selectedItems.map(it => [String(it.productId), Number(it.quantity) || 1])
                );

                itemsToDisplay = cartData.items
                    .filter(item => selectedMap.has(String(item.productId)))
                    .map(item => {
                        const selectedQty = selectedMap.get(String(item.productId));
                        const qty = Math.min(Number(item.quantity) || 1, selectedQty);
                        const price = parseFloat(item.currentPrice || item.priceAtAddition || 0);
                        return {
                            ...item,
                            quantity: qty,
                            lineItemTotal: price * qty
                        };
                    });

                total = itemsToDisplay.reduce((sum, it) => {
                    return sum + (parseFloat(it.lineItemTotal) || 0);
                }, 0);
            }

            if (!itemsToDisplay || itemsToDisplay.length === 0) {
                checkoutCartSummaryEl.innerHTML = '<p>Không tìm thấy sản phẩm để thanh toán.</p>';
                const submitButton = checkoutForm?.querySelector('button[type="submit"]');
                if (submitButton) submitButton.disabled = true;
                return;
            }

            // ⭐ LƯU TỔNG TIỀN CHƯA GIẢM ĐỂ /apply DÙNG
            window.checkoutTotalAmount = total;

            // ⭐ SỐ TIỀN GIẢM LẤY TỪ /vouchers/apply (nếu có)
            const discount = Number(window.appliedVoucherDiscount || 0);
            const finalAmount = Math.max(total - discount, 0);

            const formatVND = (n) =>
                (Number(n) || 0).toLocaleString("vi-VN", { style: "currency", currency: "VND" });

            let itemsHtml = "";
            itemsToDisplay.forEach(item => {
                itemsHtml += `
      <li>
        ${item.productName || 'Sản phẩm'} x ${item.quantity}
        <span>${formatVND(item.lineItemTotal)}</span>
      </li>`;
            });

            checkoutCartSummaryEl.innerHTML = `
    <div class="checkout-summary-card">
      <h4 class="checkout-summary-title">Tóm Tắt Đơn Hàng</h4>
      <ul class="checkout-summary-list">
        ${itemsHtml}
      </ul>
      <hr>
      <div class="checkout-summary-row">
        <span>Tạm tính</span>
        <span id="checkout-subtotal-amount">${formatVND(total)}</span>
      </div>
      <div class="checkout-summary-row">
        <span>Giảm giá voucher</span>
        <span id="checkout-discount-amount">
          ${discount > 0 ? "-" + formatVND(discount) : "-"}
        </span>
      </div>
      <div class="checkout-summary-row checkout-summary-row-total">
        <span>Thành tiền</span>
        <span id="checkout-final-amount">${formatVND(finalAmount)}</span>
      </div>
      <p class="checkout-summary-note">
        Bằng việc đặt hàng, bạn đồng ý với các điều khoản mua sắm của HyperBuy.
      </p>
    </div>
  `;
        }

        // để dùng được trong selectVoucherForCheckout gọi từ HTML
        window.displayCheckoutSummary = displayCheckoutSummary;




        async function handleCheckoutSubmit(event) {
            event.preventDefault();

            const shippingData = {
                addressLine1: document.getElementById('shipping-address1').value.trim(),
                addressLine2: document.getElementById('shipping-address2').value.trim(),
                city: document.getElementById('shipping-city').value.trim(),
                postalCode: document.getElementById('shipping-postalcode').value.trim(),
                country: document.getElementById('shipping-country').value.trim() || "Vietnam"
            };

            let billingData = null;
            if (!sameAsShippingCheckbox.checked) {
                billingData = {
                    addressLine1: document.getElementById('billing-address1').value.trim(),
                    addressLine2: document.getElementById('billing-address2').value.trim(),
                    city: document.getElementById('billing-city').value.trim(),
                    postalCode: document.getElementById('billing-postalcode').value.trim(),
                    country: document.getElementById('billing-country').value.trim() || "Vietnam"
                };
            }

            const paymentData = {
                paymentMethod: document.getElementById('payment-method').value || "COD",
                voucherCode: null,
                discountAmount: 0
            };

            // ⭐⭐⭐ LẤY MÃ VOUCHER TỪ INPUT
            const voucherCode = document.getElementById("checkout-voucher-code").value.trim();

            if (voucherCode !== "") {

                const orderAmount = window.checkoutTotalAmount || 0;

                const applyResult = await callApi(
                    VOUCHER_API_BASE_URL,
                    `/vouchers/apply?userId=${localStorage.getItem("currentUserId")}&code=${voucherCode}&orderAmount=${orderAmount}`,
                    "GET",
                    null,
                    true
                );

                console.log("APPLY VOUCHER RESULT = ", applyResult);

                if (applyResult.ok) {
                    const discountAmount = applyResult.data.discountAmount;

                    // 🔥 Lưu để hiển thị trong FE
                    window.appliedVoucherDiscount = discountAmount || 0;

                    // Nhét vào paymentData để gửi sang Order-service
                    paymentData.voucherCode = voucherCode;
                    paymentData.discountAmount = discountAmount;

                    // Cập nhật UI giá trị giảm
                    await displayCheckoutSummary();

                } else {
                    window.appliedVoucherDiscount = 0;
                    await displayCheckoutSummary();
                    alert("Voucher không hợp lệ hoặc đã dùng!");
                }
            }

            // ⭐⭐⭐ TIẾP TỤC TẠO ĐƠN HÀNG
            await handleCreateOrderWithDetails(shippingData, billingData, paymentData);
        }


        // toggle địa chỉ thanh toán
        if (sameAsShippingCheckbox && billingAddressFieldset) {
            sameAsShippingCheckbox.addEventListener('change', () => {
                billingAddressFieldset.style.display = sameAsShippingCheckbox.checked ? 'none' : 'block';
                billingAddressFieldset.querySelectorAll('input')
                    .forEach(input => input.required = !sameAsShippingCheckbox.checked);
            });
            sameAsShippingCheckbox.dispatchEvent(new Event('change'));
        }

        if (checkoutForm) {
            checkoutForm.addEventListener('submit', handleCheckoutSubmit);
        }

        await displayCheckoutSummary();
    }
    else if (page === 'my-orders.html') {
        await loadMyOrders();
    }
    else if (page === 'order-detail.html') {
        const orderId = new URLSearchParams(window.location.search).get('id');
        if (orderId) {
            await loadOrderDetail(orderId);
        } else {
            const contentEl = document.getElementById('order-detail-content');
            if (contentEl) contentEl.innerHTML = '<p class="error-message">Không tìm thấy ID đơn hàng trong URL.</p>';
        }
    }
    else if (page === 'admin-send-notification.html') {
        if (!isLoggedIn() || getUserRole() !== 'ADMIN') {
            const formContainer = document.querySelector('section.form-container');
            if (formContainer) formContainer.innerHTML = `<h2>Gửi Thông Báo</h2><p class="error-message">Bạn cần đăng nhập với quyền Admin để vào trang này.</p>`;
        }
    }


    // --- Listener cho link giỏ hàng (luôn chạy) ---
    const navCartLink = document.getElementById('nav-cart-link');
    if (navCartLink) {
        navCartLink.addEventListener('click', async function (event) {
            event.preventDefault();
            if (!isLoggedIn()) {
                if (confirm("Bạn cần đăng nhập để xem giỏ hàng. Bạn có muốn chuyển đến trang đăng nhập không?")) { window.location.href = 'login.html'; } return;
            }
            const initSuccess = await initializeCart();
            if (initSuccess) { window.location.href = this.href; }
            else { alert("Đã có lỗi xảy ra khi chuẩn bị giỏ hàng của bạn. Vui lòng thử lại."); }
        });
    }

    // --- Listener cho thông báo (luôn chạy) ---
    const notificationBellLink = document.getElementById('notification-bell-link');
    const notificationDropdown = document.getElementById('notification-dropdown');
    const navNotificationBell = document.getElementById('nav-notification-bell');
    if (notificationBellLink && notificationDropdown && navNotificationBell) {
        notificationBellLink.addEventListener('click', (e) => {
            e.preventDefault(); e.stopPropagation();
            notificationDropdown.classList.toggle('show');
        });
        document.addEventListener('click', (e) => {
            if (notificationDropdown.classList.contains('show') && !navNotificationBell.contains(e.target)) {
                notificationDropdown.classList.remove('show');
            }
        });
        notificationDropdown.addEventListener('click', async (e) => {
            if (e.target.classList.contains('mark-as-read-btn')) {
                e.preventDefault(); e.stopPropagation();
                const notificationId = e.target.dataset.id;
                if (notificationId) { await markNotificationAsRead(notificationId); }
            } else if (e.target.closest('a.dropdown-item-notif.unread')) {
                e.stopPropagation();
                const notificationItem = e.target.closest('a.dropdown-item-notif');
                const notificationId = notificationItem.dataset.id;
                if (notificationId) { await markNotificationAsRead(notificationId); }
            }
        });
    }

    // --- GLOBAL EVENT LISTENERS (luôn chạy) ---
    // =========================
    // GLOBAL CLICK HANDLER
    // =========================
    // ================== ĐÁNH GIÁ SẢN PHẨM TRONG CHI TIẾT ĐƠN HÀNG ==================

    let reviewingProductId = null;
    let reviewingOrderId = null;

    // Mở popup khi bấm nút "Đánh giá" ở từng sản phẩm trong đơn
    document.addEventListener("click", (e) => {
        const btn = e.target.closest(".btn-review-item");
        if (!btn) return;

        reviewingProductId = btn.dataset.productId;
        reviewingOrderId = btn.dataset.orderId;

        const modal = document.getElementById("review-modal");
        const starsEl = document.getElementById("review-stars");
        const commentEl = document.getElementById("review-comment");

        if (starsEl) starsEl.value = "5";
        if (commentEl) commentEl.value = "";
        if (modal) modal.style.display = "flex";
    });

    // Đóng popup
    function closeReviewModal() {
        const modal = document.getElementById("review-modal");
        if (modal) modal.style.display = "none";
    }

    // Gửi đánh giá
    async function submitReview() {
        const starsEl = document.getElementById("review-stars");
        const commentEl = document.getElementById("review-comment");

        if (!starsEl || !commentEl) {
            alert("Không tìm thấy form đánh giá.");
            return;
        }

        const ratingValue = Number(starsEl.value);
        const comment = commentEl.value.trim();

        if (!ratingValue) {
            alert("Vui lòng chọn số sao.");
            return;
        }

        if (!comment) {
            alert("Vui lòng nhập nội dung đánh giá.");
            return;
        }

        // CHECK BOTH GLOBAL VAR AND MODAL DATASET
        const modal = document.getElementById("review-modal");
        let actualProductId = reviewingProductId;
        if (!actualProductId && modal && modal.dataset.productId) {
            actualProductId = modal.dataset.productId;
        }

        if (!actualProductId || actualProductId === "undefined" || actualProductId === "null") {
            alert("Không xác định được sản phẩm để đánh giá.");
            return;
        }

        // Đảm bảo không bấm nhiều lần
        const btnSubmit = document.getElementById("btn-submit-review");
        if (btnSubmit) btnSubmit.disabled = true;

        const body = {
            // map đúng với backend mà bạn test bằng Postman
            ratingValue: ratingValue,
            comment: comment
        };

        console.log("Sending review body:", body, "productId:", actualProductId);

        const res = await callApi(
            PRODUCT_API_BASE_URL,
            `/products/${actualProductId}/ratings`,
            "POST",
            body,
            true   // cần token
        );

        if (btnSubmit) btnSubmit.disabled = false;

        if (res.ok) {
            alert("Đánh giá thành công!");
            closeReviewModal();
            // reload để cập nhật danh sách đánh giá / trạng thái
            location.reload();
        } else {
            const msg = res.data?.message || res.error || "Không thể gửi đánh giá.";
            alert(msg);
        }
    }

    // Gắn sự kiện trực tiếp cho 2 nút trong popup (đề phòng đoạn trên có lỗi)
    document.addEventListener("DOMContentLoaded", () => {
        const btnSubmit = document.getElementById("btn-submit-review");
        const btnClose = document.getElementById("btn-close-review");

        if (btnSubmit) {
            btnSubmit.addEventListener("click", (e) => {
                e.preventDefault();
                submitReview();
            });
        }

        if (btnClose) {
            btnClose.addEventListener("click", (e) => {
                e.preventDefault();
                closeReviewModal();
            });
        }
    });
    // Gắn sự kiện cho 2 nút trong popup đánh giá
    (function initReviewButtons() {
        const submitBtn = document.getElementById("btn-submit-review");
        const closeBtn = document.getElementById("btn-close-review");

        if (submitBtn) {
            submitBtn.addEventListener("click", (e) => {
                e.preventDefault();
                e.stopPropagation();
                submitReview();
            });
        }

        if (closeBtn) {
            closeBtn.addEventListener("click", (e) => {
                e.preventDefault();
                e.stopPropagation();
                closeReviewModal();
            });
        }

        // (tuỳ chọn) click ra ngoài overlay thì đóng popup
        const modal = document.getElementById("review-modal");
        if (modal) {
            modal.addEventListener("click", (e) => {
                if (e.target === modal) {
                    closeReviewModal();
                }
            });
        }
    })();
    // ================== HẾT PHẦN ĐÁNH GIÁ ==================

    document.body.addEventListener('click', async function (event) {
        const target = event.target;

        // --- CHỨC NĂNG GIỎ HÀNG VÀ MUA NGAY ---
        if (target.classList.contains('btn-add-to-cart')) {
            if (target.disabled) return;
            const { productId } = target.dataset;
            if (productId) await addToCartAPI(String(productId), 1, true);
            else console.error("Thêm vào giỏ: Thiếu data-product-id.");
        }
        else if (target.classList.contains('btn-buy-now')) {
            if (target.disabled) return;
            const { productId } = target.dataset;
            if (productId) {
                const success = await addToCartAPI(String(productId), 1, false);
                if (success) window.location.href = 'cart.html';
            } else console.error("Mua ngay: Thiếu data-product-id.");
        }

        // --- SELLER CONTROLS ---
        else if (target.classList.contains('btn-deactivate-product')) {
            await handleDeactivateProduct(target);
        }
        else if (target.classList.contains('btn-activate-product')) {
            await handleActivateProduct(target);
        }
        else if (target.classList.contains('btn-seller-delete-product')) {
            await handleSellerDeleteProduct(target);
        }

        // --- ADMIN DELETE ---
        else if (target.classList.contains('btn-hard-delete-product')) {
            const { productId } = target.dataset;
            if (productId) await handleHardDeleteProduct(productId);
        }

        // --- VIEW SELLER INFO ---
        else if (target.classList.contains('btn-view-seller')) {
            const sellerUserId = target.dataset.sellerId;
            if (sellerUserId) {
                await openSellerModal(sellerUserId);
            } else {
                console.error("View Store: Thiếu data-seller-id");
                alert("Không thể lấy thông tin cửa hàng.");
            }
        }

        // --- CLOSE MODALS ---
        else if (target.matches('.close-button')) {
            const modalToClose = target.closest('.modal');
            if (modalToClose) {
                if (modalToClose.id === 'sellerInfoModal') closeSellerModal();
                else if (modalToClose.id === 'sellerRequestModal') modalToClose.style.display = 'none';
            }
        }

        // --- ADMIN SELLER REQUEST ---
        else if (target.classList.contains('btn-approve-request')) {
            const userId = target.dataset.userId;
            if (userId) await handleApproveRequest(userId);
        }
        else if (target.classList.contains('btn-reject-request')) {
            const userId = target.dataset.userId;
            alert("Chức năng từ chối chưa được cài đặt.");
        }
    });

    // =========================
    // CLOSE MODAL WHEN CLICK OUTSIDE
    // =========================
    window.addEventListener('click', (event) => {
        const sellerInfoModal = document.getElementById('sellerInfoModal');
        if (sellerInfoModal && event.target === sellerInfoModal) {
            closeSellerModal();
        }

        const sellerRequestModal = document.getElementById('sellerRequestModal');
        if (sellerRequestModal && event.target === sellerRequestModal) {
            sellerRequestModal.style.display = 'none';
        }
    });
    // =========================
    // AUTO-INIT CHO MINIGAME PAGE
    // =========================
    const path = window.location.pathname;
    if (path.endsWith("minigame.html")) {
        console.log("🎮 Minigame page detected → initMinigamePage()");
        initMinigamePage();
    }
});
(function pageTransitionInit() {
    // fade-in khi vào trang
    document.addEventListener("DOMContentLoaded", () => {
        document.body.classList.add("page-enter");
        requestAnimationFrame(() => {
            requestAnimationFrame(() => document.body.classList.remove("page-enter"));
        });
    });

    // fade-out khi click link chuyển trang
    document.addEventListener("click", (e) => {
        const a = e.target.closest("a");
        if (!a) return;

        const href = a.getAttribute("href");
        if (!href || href.startsWith("#")) return;
        if (a.target === "_blank") return;
        if (href.startsWith("http")) return;

        // cho phép ctrl / cmd / shift mở tab mới
        if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;

        e.preventDefault();
        document.body.classList.add("page-exit");

        setTimeout(() => {
            window.location.href = href;
        }, 180);
    });
})();