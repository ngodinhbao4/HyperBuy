document.addEventListener('DOMContentLoaded', async () => {
    // === KIỂM TRA QUYỀN TRUY CẬP ===
if (
    typeof isLoggedIn !== 'function' ||
    typeof getUserRole !== 'function' ||
    typeof callApi !== 'function' ||
    typeof USER_API_BASE_URL === 'undefined' ||
    typeof PRODUCT_API_BASE_URL === 'undefined' ||
    typeof NOTIFICATION_API_BASE_URL === 'undefined' ||
    typeof ORDER_API_BASE_URL === 'undefined'      // ✅ NEW
) {
    document.body.innerHTML =
        '<h1 style="color: red; text-align: center; padding: 50px;">Lỗi: Không thể tải các hàm/biến cần thiết từ main.js. Vui lòng kiểm tra lại.</h1>';
    return;
}

    if (!isLoggedIn() || getUserRole() !== 'ADMIN') {
        const mainContent = document.getElementById('admin-main-content');
        if (mainContent) {
            mainContent.innerHTML = `
                <div id="admin-access-denied" style="text-align:center; padding: 50px; background: white; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                    <h2>Truy cập bị từ chối</h2>
                    <p>Bạn không có quyền truy cập trang này.</p>
                    <p>Vui lòng <a href="login.html" style="font-weight: bold; color: #007bff;">đăng nhập</a> với tài khoản Quản trị viên hoặc <a href="index.html" style="font-weight: bold; color: #007bff;">quay lại trang chủ</a>.</p>
                </div>`;
            const adminLogoutBtn = document.getElementById('admin-logout-btn');
            if (adminLogoutBtn) adminLogoutBtn.style.display = 'none';
            const adminSubNav = document.getElementById('admin-sub-nav');
            if (adminSubNav) adminSubNav.style.display = 'none';
            const adminH1 = document.querySelector('main.container h1');
            if (adminH1) adminH1.style.display = 'none';
        }
        return;
    }

    // === TẢI DỮ LIỆU BAN ĐẦU ===
    console.log("Admin Dashboard: Bắt đầu tải dữ liệu...");
    await loadSellerRequests();
    await loadAllUsers();
    await loadBannedUsers();
    await loadAdminCategories();
    await loadAdminVouchers();
    await loadAdminRoles();
    console.log("Admin Dashboard: Tải dữ liệu hoàn tất.");

    // Update stat cards
    updateStatCards();


    // === LOGIC ĐIỀU HƯỚNG CON CHO ADMIN ===
    const adminSections = document.querySelectorAll('.admin-section');
    const adminNavLinks = document.querySelectorAll('#admin-sub-nav .admin-nav-link');

    function setActiveAdminSection(targetSectionId) {
        let sectionFound = false;
        adminSections.forEach(section => {
            section.classList.toggle('active', section.id === targetSectionId);
            if (section.id === targetSectionId) sectionFound = true;
        });
        adminNavLinks.forEach(link => {
            link.classList.toggle('active', link.dataset.sectionId === targetSectionId);
        });
        if (sectionFound) {
            history.replaceState(null, '', '#' + targetSectionId);
        } else if (adminSections.length > 0 && adminNavLinks.length > 0) { // Fallback nếu hash không hợp lệ
            adminSections[0].classList.add('active');
            adminNavLinks[0].classList.add('active');
            history.replaceState(null, '', '#' + adminSections[0].id);
        }
    }

    adminNavLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            setActiveAdminSection(link.dataset.sectionId);
        });
    });
    const currentHash = window.location.hash.substring(1);
    setActiveAdminSection(currentHash && document.getElementById(currentHash) ? currentHash : 'user-management');

    // === EVENT LISTENERS ===
    const adminLogoutBtn = document.getElementById('admin-logout-btn');
    if (adminLogoutBtn && typeof handleLogout === 'function') {
        adminLogoutBtn.addEventListener('click', (e) => { e.preventDefault(); handleLogout(); });
    }
    document.getElementById('seller-requests-table')?.addEventListener('click', handleUserTableClick);
    document.getElementById('all-users-table')?.addEventListener('click', handleUserTableClick); // Listener cho bảng mới
    document.getElementById('banned-users-table')?.addEventListener('click', handleUserTableClick);
    document.getElementById('admin-orders-table')?.addEventListener('click', handleAdminOrdersTableClick);
    // document.getElementById('ban-user-button')?.addEventListener('click', handleBanUserClick); // Đã XÓA

    document.getElementById('show-add-category-form')?.addEventListener('click', () => showCategoryForm());
    document.getElementById('save-category-button')?.addEventListener('click', handleSaveCategoryClick);
    document.getElementById('cancel-category-button')?.addEventListener('click', hideCategoryForm);
    document.getElementById('categories-table')?.addEventListener('click', handleCategoryTableClick);
    document.getElementById('adminSendNotificationForm')?.addEventListener('submit', handleAdminSendNotificationSubmit);
    document.getElementById('createVoucherForm')?.addEventListener('submit', handleCreateVoucher);
    document.getElementById('issueVoucherForm')?.addEventListener('submit', handleIssueVoucher);
    document.getElementById('btn-reload-vouchers')?.addEventListener('click', () => loadAdminVouchers());
    document.getElementById('btn-create-role')?.addEventListener('click', handleCreateRole);
    document.getElementById('btn-reload-roles')?.addEventListener('click', () => loadAdminRoles());
    document.getElementById('roles-table')?.addEventListener('click', async (e) => {
        const btn = e.target.closest('.btn-delete-role');
        if (btn) {
            const roleName = btn.dataset.role;
            if (roleName && confirm(`Bạn có chắc muốn XÓA role "${roleName}" không?`)) {
                await handleDeleteRole(roleName);
            }
        }
    });
    
    const currentYearEl = document.getElementById('current-year');
    if (currentYearEl) currentYearEl.textContent = new Date().getFullYear();
});

// === HÀM HỖ TRỢ HIỂN THỊ ===
function displayTableMessage(tableId, message, colspan = 5, isError = false) {
    const tableBody = document.getElementById(tableId)?.querySelector('tbody');
    if (tableBody) {
        tableBody.innerHTML = `<tr><td colspan="${colspan}" style="text-align: center; color: ${isError ? 'red' : 'inherit'};">${message}</td></tr>`;
    }
}
// === CẬP NHẬT STAT CARDS ===
function updateStatCards() {
    const allUserCount = document.getElementById('all-user-count')?.textContent;
    const bannedCount = document.getElementById('banned-user-count')?.textContent;
    const catCount = document.querySelectorAll('#categories-table tbody tr')?.length;

    if (document.getElementById('stat-total-users') && allUserCount)
        document.getElementById('stat-total-users').textContent = allUserCount || '0';

    if (document.getElementById('stat-total-banned') && bannedCount)
        document.getElementById('stat-total-banned').textContent = bannedCount || '0';

    // Count sellers from user table
    const allRows = document.querySelectorAll('#all-users-table tbody tr');
    let sellerCount = 0;
    allRows.forEach(row => {
        if (row.textContent.includes('SELLER')) sellerCount++;
    });
    const statSellers = document.getElementById('stat-total-sellers');
    if (statSellers) statSellers.textContent = sellerCount;

    const statCat = document.getElementById('stat-total-categories');
    if (statCat) {
        const rows = document.querySelectorAll('#categories-table tbody tr');
        statCat.textContent = rows.length > 0 && rows[0].children.length > 1 ? rows.length : '0';
    }
}

function showMessage(elementId, message, isSuccess = true) {
    const el = document.getElementById(elementId);
    if (el) {
        el.textContent = message;
        el.className = isSuccess ? 'success-message' : 'error-message';
        el.style.display = 'block';
        setTimeout(() => { el.style.display = 'none'; }, 5000);
    }
}

// === QUẢN LÝ NGƯỜI DÙNG ===
async function loadSellerRequests() {
    const tableBody = document.getElementById('seller-requests-table')?.querySelector('tbody');
    const countSpan = document.getElementById('seller-request-count');
    if (!tableBody || !countSpan) return;
    displayTableMessage('seller-requests-table', 'Đang tải...', 4);

    const result = await callApi(USER_API_BASE_URL, '/users/seller-requests', 'GET', null, true);

    if (result.ok && result.data && Array.isArray(result.data.result)) {
        const requests = result.data.result;
        countSpan.textContent = requests.length;
        if (requests.length === 0) {
            displayTableMessage('seller-requests-table', 'Không có yêu cầu nào.', 4);
            return;
        }
        tableBody.innerHTML = '';
        requests.forEach(req => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${req.id || 'N/A'}</td>
                <td>${req.username || 'N/A'}</td>
                <td>${req.storeName || 'N/A'}</td>
                <td class="action-buttons">
                    <button class="btn btn-success btn-sm approve-seller" data-id="${req.id}">Phê duyệt</button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    } else {
        displayTableMessage('seller-requests-table', `Lỗi tải yêu cầu: ${result.error || result.data?.message || 'Failed to fetch'}`, 4, true);
    }
}

async function loadAllUsers() {
    const tableBody = document.getElementById('all-users-table')?.querySelector('tbody');
    const countSpan = document.getElementById('all-user-count');
    if (!tableBody || !countSpan) return;
    displayTableMessage('all-users-table', 'Đang tải...', 5);

    const result = await callApi(USER_API_BASE_URL, '/users', 'GET', null, true);

    if (result.ok && result.data && Array.isArray(result.data.result)) {
        const users = result.data.result;
        countSpan.textContent = users.length;
        if (users.length === 0) {
            displayTableMessage('all-users-table', 'Không có người dùng nào.', 5);
            return;
        }
        tableBody.innerHTML = '';
        users.forEach(user => {
            const isAdmin = user.role?.some(r => r.name === 'ADMIN');
            const isSeller = user.role?.some(r => r.name === 'SELLER');
            const roleNames = user.role?.map(r => r.name) || [];
            const roleBadges = roleNames.map(r =>
                `<span class="role-badge ${r}">${r}</span>`
            ).join(' ') || '<span class="role-badge USER">USER</span>';
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="font-family:monospace;font-size:0.78rem;max-width:140px;overflow:hidden;text-overflow:ellipsis;">${user.id || 'N/A'}</td>
                <td><strong>${user.username || user.name || 'N/A'}</strong></td>
                <td>${user.email || 'N/A'}</td>
                <td>${roleBadges}</td>
                <td class="action-buttons">
                   ${!isAdmin ? `<button class="btn btn-danger btn-sm ban-user" data-id="${user.id}"><i class="fas fa-ban"></i> Cấm</button>` : '<span style="color:var(--admin-text-muted)">Quản trị viên</span>'}
                </td>
            `;
            tableBody.appendChild(tr);
        });

    } else {
        displayTableMessage('all-users-table', `Lỗi tải danh sách người dùng: ${result.error || result.data?.message || 'Failed to fetch'}`, 5, true);
    }
}

async function loadBannedUsers() {
    const tableBody = document.getElementById('banned-users-table')?.querySelector('tbody');
    const countSpan = document.getElementById('banned-user-count');
    if (!tableBody || !countSpan) return;
    displayTableMessage('banned-users-table', 'Đang tải...', 4);

    const result = await callApi(USER_API_BASE_URL, '/users/banned', 'GET', null, true);

    if (result.ok && result.data && Array.isArray(result.data.result)) {
        const users = result.data.result;
        countSpan.textContent = users.length;
        if (users.length === 0) {
            displayTableMessage('banned-users-table', 'Không có người dùng nào bị cấm.', 4);
            return;
        }
        tableBody.innerHTML = '';
        users.forEach(user => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${user.id || 'N/A'}</td>
                <td>${user.name || user.username || 'N/A'}</td>
                <td>${user.email || 'N/A'}</td>
                <td class="action-buttons">
                   <button class="btn btn-warning btn-sm unban-user" data-id="${user.id}">Bỏ Cấm</button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    } else {
        displayTableMessage('banned-users-table', `Lỗi tải danh sách cấm: ${result.error || result.data?.message || 'Failed to fetch'}`, 4, true);
    }
}

async function handleUserTableClick(event) {
    const target = event.target;
    const id = target.dataset.id; // ID này có thể là request ID hoặc user ID tùy theo nút

    if (!id) return;

    if (target.classList.contains('approve-seller')) {
        if (confirm(`Bạn có chắc muốn PHÊ DUYỆT yêu cầu bán hàng ${id} không?`)) {
            const result = await callApi(USER_API_BASE_URL, `/users/approve-seller/${id}`, 'POST', null, true);
            if (result.ok) {
                alert('Phê duyệt thành công!');
                await loadSellerRequests();
                await loadAllUsers(); 
            } else {
                alert(`Lỗi phê duyệt: ${result.data?.message || result.error}`);
            }
        }
    } else if (target.classList.contains('reject-seller')) {
        if (confirm(`Bạn có chắc muốn TỪ CHỐI yêu cầu bán hàng ${id} không?`)) {
            // QUAN TRỌNG: Đây là endpoint giả định, bạn cần xác nhận với backend
            const result = await callApi(USER_API_BASE_URL, `/users/reject-seller/${id}`, 'POST', null, true); 
            if (result.ok) {
                alert('Từ chối yêu cầu thành công!');
                await loadSellerRequests();
            } else {
                alert(`Lỗi từ chối yêu cầu: ${result.data?.message || result.error}`);
            }
        }
    } else if (target.classList.contains('unban-user')) {
        if (confirm(`Bạn có chắc muốn BỎ CẤM tài khoản người dùng ${id} không?`)) {
            const result = await callApi(USER_API_BASE_URL, `/users/unban/${id}`, 'POST', null, true);
            if (result.ok) {
                alert('Bỏ cấm tài khoản thành công!');
                await loadBannedUsers();
                await loadAllUsers();
            } else {
                alert(`Lỗi bỏ cấm: ${result.data?.message || result.error}`);
            }
        }
    } else if (target.classList.contains('ban-user')) {
        if (confirm(`Bạn có chắc muốn CẤM tài khoản người dùng ${id} không?`)) {
            const result = await callApi(USER_API_BASE_URL, `/users/ban/${id}`, 'POST', null, true);
            if (result.ok) {
                alert(`Người dùng ${id} đã bị cấm thành công!`);
                await loadBannedUsers();
                await loadAllUsers();
            } else {
                alert(`Lỗi cấm người dùng: ${result.data?.message || result.error}`);
            }
        }
    }
}

// Không còn form cấm riêng nữa nên hàm này không cần thiết
// async function handleBanUserClick() { ... } 
// === QUẢN LÝ ĐƠN HÀNG (ADMIN) ===
// === QUẢN LÝ ĐƠN HÀNG (ADMIN) ===
// ================== ADMIN – QUẢN LÝ ĐƠN HÀNG ==================

console.log("admin.js loaded");

// Lấy danh sách đơn hàng theo username (dùng API: GET /orders/user/{username})
async function loadAdminOrders() {
    const tbody = document.querySelector('#admin-orders-table tbody');
    const countSpan = document.getElementById('admin-order-count');
    const errorBox = document.getElementById('admin-orders-error');
    const searchInput = document.getElementById('admin-order-search-input');

    if (!tbody) return;

    // Reset UI
    tbody.innerHTML = '<tr><td colspan="6">Đang tải đơn hàng...</td></tr>';
    if (errorBox) {
        errorBox.classList.add('d-none');
        errorBox.textContent = '';
    }
    if (countSpan) countSpan.textContent = '0';

    const username = (searchInput?.value || '').trim();
    if (!username) {
        // Không gọi API nếu chưa nhập username để tránh lỗi
        tbody.innerHTML = '<tr><td colspan="6">Hãy nhập username để tìm đơn hàng.</td></tr>';
        return;
    }

    try {
        const endpoint = `/orders/user/${encodeURIComponent(username)}`;
        console.log("Admin: gọi API", ORDER_API_BASE_URL + endpoint);

        const result = await callApi(
            ORDER_API_BASE_URL,
            endpoint,
            'GET',
            null,
            true   // có token admin
        );

        if (!result.ok) {
            console.error("Admin load orders error:", result);
            const msg = result.data?.message || result.error || `Lỗi server (status ${result.status})`;
            tbody.innerHTML = `<tr><td colspan="6" class="text-danger">Lỗi tải đơn hàng: ${msg}</td></tr>`;
            if (errorBox) {
                errorBox.classList.remove('d-none');
                errorBox.textContent = `Lỗi tải đơn hàng: ${msg}`;
            }
            return;
        }

        const orders = result.data?.result || result.data || [];
        if (!Array.isArray(orders) || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">Không có đơn hàng nào cho user này.</td></tr>';
            if (countSpan) countSpan.textContent = '0';
            return;
        }

        // Render bảng
        tbody.innerHTML = '';
        orders.forEach(order => {
            const tr = document.createElement('tr');

            const createdAt = order.createdAt
                ? new Date(order.createdAt).toLocaleString('vi-VN')
                : '';

            const statusClass =
                order.status === 'DELIVERED' ? 'role-badge SELLER' :
                order.status === 'CONFIRMED' ? 'role-badge ADMIN' :
                order.status === 'CANCELLED' ? 'role-badge BANNED' :
                'role-badge USER';


            tr.innerHTML = `
    <td>${order.id ?? ''}</td>
    <td>${order.username || order.userId || ''}</td>
    <td>${(order.totalAmount || order.grandTotal || 0).toLocaleString('vi-VN')} đ</td>
    <td><span class="${statusClass}">${statusBadge}</span></td>
    <td>${createdAt}</td>
    <td>
        <button 
            class="btn btn-sm btn-outline-primary admin-btn-view-order" 
            data-order-id="${order.id}">
            Xem
        </button>
    </td>
`;

            tbody.appendChild(tr);
        });

        if (countSpan) countSpan.textContent = String(orders.length);
    } catch (err) {
        console.error("Admin load orders exception:", err);
        tbody.innerHTML =
            '<tr><td colspan="6" class="text-danger">Lỗi không xác định khi tải đơn hàng.</td></tr>';
        if (errorBox) {
            errorBox.classList.remove('d-none');
            errorBox.textContent = 'Lỗi không xác định khi tải đơn hàng.';
        }
    }
}

// Xử lý click các nút trong bảng
async function handleAdminOrdersTableClick(e) {
    const viewBtn = e.target.closest('.admin-btn-view-order');
    if (!viewBtn) return;

    const orderId = viewBtn.dataset.orderId;
    if (!orderId) return;

    // Admin chỉ được xem chi tiết đơn
    window.location.href = `my-orders.html?orderId=${orderId}`;
}


// Gọi API cập nhật trạng thái đơn hàng (API: PUT /orders/{id}/status)


// Khởi tạo sự kiện cho trang admin
document.addEventListener('DOMContentLoaded', () => {
    if (!window.location.pathname.endsWith('admin.html')) return;

    console.log("Admin page detected, init order management...");

    const table = document.getElementById('admin-orders-table');
    if (table) {
        table.addEventListener('click', handleAdminOrdersTableClick);
    }

    const searchBtn = document.getElementById('admin-order-search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', (e) => {
            e.preventDefault();
            loadAdminOrders();
        });
    }

    // Nếu đang ở tab #order-management thì có thể tự load luôn
    if (location.hash === '#order-management') {
        loadAdminOrders();
    }
});

// Gọi API cập nhật trạng thái đơn hàng (Admin)

// Bắt sự kiện click trong bảng đơn hàng admin


// === QUẢN LÝ DANH MỤC === 
async function loadAdminCategories() {
    const tableBody = document.getElementById('categories-table')?.querySelector('tbody');
    if (!tableBody) return;
    displayTableMessage('categories-table', 'Đang tải...', 4);
    const result = await callApi(PRODUCT_API_BASE_URL, '/categories', 'GET', null, true);
    const categories = Array.isArray(result.data) ? result.data : (result.data?.content || []);
    if (result.ok && categories) {
        if (categories.length === 0) { displayTableMessage('categories-table', 'Chưa có danh mục nào.', 4); return; }
        tableBody.innerHTML = '';
        categories.forEach(cat => {
            const tr = document.createElement('tr');
            tr.dataset.id = cat.id; tr.dataset.name = cat.name; tr.dataset.description = cat.description || '';
            tr.innerHTML = `
                <td>${cat.id}</td> <td>${cat.name}</td> <td>${cat.description || ''}</td>
                <td class="action-buttons">
                    <button class="btn btn-info btn-sm edit-category" data-id="${cat.id}">Sửa</button>
                    <button class="btn btn-danger btn-sm delete-category" data-id="${cat.id}">Xóa</button>
                </td>`;
            tableBody.appendChild(tr);
        });
    } else { displayTableMessage('categories-table', `Lỗi tải danh mục: ${result.error || result.data?.message || 'Failed to fetch'}`, 4, true); }
}
function showCategoryForm(isEdit = false, id = '', name = '', description = '') {
    const formDiv = document.getElementById('add-edit-category-form');
    document.getElementById('category-form-title').textContent = isEdit ? 'Sửa Danh mục' : 'Thêm Danh mục Mới';
    document.getElementById('category-id').value = id;
    document.getElementById('category-name').value = name;
    document.getElementById('category-description').value = description;
    document.getElementById('category-message').textContent = '';
    formDiv.style.display = 'block';
    document.getElementById('show-add-category-form').style.display = 'none';
    document.getElementById('category-name').focus();
}
function hideCategoryForm() {
    const formDiv = document.getElementById('add-edit-category-form');
    formDiv.style.display = 'none';
    document.getElementById('show-add-category-form').style.display = 'inline-block';
    document.getElementById('category-id').value = '';
    document.getElementById('category-name').value = '';
    document.getElementById('category-description').value = '';
    document.getElementById('category-message').textContent = '';
}
async function handleSaveCategoryClick() {
    const id = document.getElementById('category-id').value;
    const name = document.getElementById('category-name').value.trim();
    const description = document.getElementById('category-description').value.trim();
    if (!name) { showMessage('category-message', 'Tên danh mục là bắt buộc.', false); return; }
    const data = { name, description };
    const method = id ? 'PUT' : 'POST';
    const endpoint = id ? `/categories/${id}` : '/categories';
    const result = await callApi(PRODUCT_API_BASE_URL, endpoint, method, data, true);
    if (result.ok) {
        alert(id ? 'Cập nhật danh mục thành công!' : 'Thêm danh mục thành công!');
        hideCategoryForm();
        await loadAdminCategories();
    } else { showMessage('category-message', `Lỗi: ${result.data?.message || result.error}`, false); }
}
async function handleCategoryTableClick(event) {
    const target = event.target;
    const categoryId = target.dataset.id;
    if (target.classList.contains('edit-category')) {
        const row = target.closest('tr');
        if (row) showCategoryForm(true, categoryId, row.dataset.name, row.dataset.description);
    } else if (target.classList.contains('delete-category')) {
        if (confirm(`Bạn có chắc muốn XÓA danh mục ID: ${categoryId} không?`)) {
            const result = await callApi(PRODUCT_API_BASE_URL, `/categories/${categoryId}`, 'DELETE', null, true);
            if (result.ok || result.status === 204) { alert('Xóa danh mục thành công!'); await loadAdminCategories(); }
            else { alert(`Lỗi xóa: ${result.data?.message || result.error}`); }
        }
    }
}

// === QUẢN LÝ THÔNG BÁO === 
async function handleAdminSendNotificationSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const userId = form.userId.value.trim(); 
    const message = form.message.value.trim();
    const successMsgEl = document.getElementById('send-notification-admin-success-message');
    const errorMsgEl = document.getElementById('send-notification-admin-error-message');
    if (successMsgEl) successMsgEl.style.display = 'none'; 
    if (errorMsgEl) errorMsgEl.style.display = 'none';
    if (!message) { 
        if (errorMsgEl) { errorMsgEl.textContent = 'Vui lòng nhập nội dung thông báo.'; errorMsgEl.style.display = 'block'; }
        return; 
    }
    const notificationData = { message };
    if (userId) { notificationData.userId = userId; }
    const result = await callApi(NOTIFICATION_API_BASE_URL, '/notifications/admin/send', 'POST', notificationData, true);
    if (result.ok) {
        if (successMsgEl) { successMsgEl.textContent = 'Gửi thông báo thành công!'; successMsgEl.style.display = 'block';}
        form.reset();
        setTimeout(() => { if (successMsgEl) successMsgEl.style.display = 'none'; }, 5000);
    } else { 
        if (errorMsgEl) { errorMsgEl.textContent = `Lỗi gửi thông báo: ${result.data?.message || result.error}`; errorMsgEl.style.display = 'block'; }
    }
}

// ===== QUẢN LÝ VOUCHER =====
async function loadAdminVouchers() {
    const tbody = document.getElementById('admin-vouchers-table')?.querySelector('tbody');
    const countEl = document.getElementById('admin-voucher-count');
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;color:var(--admin-text-muted);">Đang tải...</td></tr>`;

    const result = await callApi(VOUCHER_API_BASE_URL, '/vouchers', 'GET', null, true);
    const vouchers = Array.isArray(result.data) ? result.data
        : Array.isArray(result.data?.result) ? result.data.result
        : Array.isArray(result.data?.content) ? result.data.content : [];

    if (!result.ok && !vouchers.length) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:red;">Lỗi tải voucher: ${result.data?.message || result.error || 'Không kết nối được'}</td></tr>`;
        return;
    }
    if (countEl) countEl.textContent = vouchers.length;
    // Update stat card
    const statVoucher = document.getElementById('stat-total-vouchers');
    if (statVoucher) statVoucher.textContent = vouchers.length;

    if (!vouchers.length) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;color:var(--admin-text-muted);">Chưa có voucher nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = '';
    vouchers.forEach(v => {
        const discount = v.discountType === 'PERCENT'
            ? `${v.discountValue}%`
            : `${(v.discountValue || 0).toLocaleString('vi-VN')}₫`;
        const endDate = v.endDate ? new Date(v.endDate).toLocaleDateString('vi-VN') : '—';
        const statusClass = v.status === 'ACTIVE' ? 'role-badge SELLER' : 'role-badge BANNED';
        const statusLabel = v.status === 'ACTIVE' ? 'ACTIVE' : (v.status || 'INACTIVE');
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${v.code || '—'}</strong></td>
            <td>${v.discountType === 'PERCENT' ? 'Phần trăm' : 'Tiền mặt'}</td>
            <td>${discount}</td>
            <td>${v.pointCost ?? 0}</td>
            <td>${v.quantity ?? '—'}</td>
            <td>${endDate}</td>
            <td><span class="${statusClass}">${statusLabel}</span></td>
            <td class="action-buttons">
                <button class="btn btn-danger btn-sm delete-voucher" data-code="${v.code}"><i class="fas fa-trash"></i> Xóa</button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    // Bind delete
    tbody.querySelectorAll('.delete-voucher').forEach(btn => {
        btn.addEventListener('click', () => handleDeleteVoucher(btn.dataset.code));
    });
}

async function handleDeleteVoucher(code) {
    if (!code || !confirm(`Xóa voucher "${code}"?`)) return;
    const result = await callApi(VOUCHER_API_BASE_URL, `/vouchers/${encodeURIComponent(code)}`, 'DELETE', null, true);
    if (result.ok) {
        showMessage('voucher-list-message', `Đã xóa voucher ${code}!`, true);
        await loadAdminVouchers();
    } else {
        showMessage('voucher-list-message', `Lỗi xóa voucher: ${result.data?.message || result.error}`, false);
    }
}

async function handleCreateVoucher(event) {
    event.preventDefault();
    const form = event.target;
    const data = {
        code:          form.code.value.trim(),
        discountValue: parseFloat(form.discountValue.value),
        discountType:  form.discountType.value,
        quantity:      parseInt(form.quantity.value, 10) || 1,
        startDate:     form.startDate.value ? form.startDate.value.replace('T', ' ') + ':00' : null,
        endDate:       form.endDate.value   ? form.endDate.value.replace('T', ' ')   + ':00' : null,
        pointCost:     parseInt(form.pointCost.value, 10) || 0,
        status:        form.status.value,
    };
    if (!data.code) { showMessage('voucher-error-message', 'Mã voucher không được để trống.', false); return; }
    showMessage('voucher-error-message', '', false);

    const result = await callApi(VOUCHER_API_BASE_URL, '/vouchers', 'POST', data, true);
    if (result.ok) {
        showMessage('voucher-success-message', `Tạo voucher ${data.code} thành công!`, true);
        form.reset();
        await loadAdminVouchers();
    } else {
        showMessage('voucher-error-message', `Lỗi: ${result.data?.message || result.error || 'Tạo voucher thất bại.'}`, false);
    }
}

async function handleIssueVoucher(event) {
    event.preventDefault();
    const form = event.target;
    const userId = form.userId.value.trim();
    const voucherCode = form.voucherCode.value.trim();
    if (!userId || !voucherCode) {
        showMessage('issue-error-message', 'Vui lòng nhập đầy đủ User ID và Mã Voucher.', false);
        return;
    }
    const result = await callApi(VOUCHER_API_BASE_URL, `/vouchers/issue/${encodeURIComponent(userId)}?code=${encodeURIComponent(voucherCode)}`, 'POST', null, true);
    if (result.ok) {
        showMessage('issue-success-message', `Đã gửi voucher ${voucherCode} cho user ${userId}!`, true);
        form.reset();
    } else {
        showMessage('issue-error-message', `Lỗi: ${result.data?.message || result.error || 'Phát voucher thất bại.'}`, false);
    }
}

// ===== QUẢN LÝ ROLE =====

async function loadAdminRoles() {
    const tbody = document.getElementById('roles-table')?.querySelector('tbody');
    const countEl = document.getElementById('role-count');
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--admin-text-muted);">Đang tải...</td></tr>`;

    const result = await callApi(USER_API_BASE_URL, '/roles', 'GET', null, true);

    // Backend trả về { code, message, result: [...] }
    const roles = Array.isArray(result.data?.result) ? result.data.result
        : Array.isArray(result.data) ? result.data : [];

    if (!result.ok && !roles.length) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:red;">Lỗi tải danh sách role: ${result.data?.message || result.error || 'Không kết nối được'}</td></tr>`;
        return;
    }

    if (countEl) countEl.textContent = roles.length;

    if (!roles.length) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--admin-text-muted);">Chưa có role nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = '';
    const protectedRoles = ['ADMIN', 'USER', 'SELLER'];

    roles.forEach(role => {
        const permissionNames = role.permissions?.map(p => p.name || p).join(', ') || '—';
        const isProtected = protectedRoles.includes(role.name?.toUpperCase());

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong><span class="role-badge ${role.name?.toUpperCase()}">${role.name || '—'}</span></strong></td>
            <td>${role.description || '—'}</td>
            <td style="font-size:0.8rem;color:var(--admin-text-muted);">${permissionNames}</td>
            <td class="action-buttons">
                ${isProtected
                    ? `<span style="color:var(--admin-text-muted);font-size:0.82rem;"><i class="fas fa-lock"></i> Hệ thống</span>`
                    : `<button class="btn btn-danger btn-sm btn-delete-role" data-role="${role.name}"><i class="fas fa-trash"></i> Xóa</button>`
                }
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function handleCreateRole() {
    const nameInput = document.getElementById('role-name');
    const descInput = document.getElementById('role-description');
    const msgEl = document.getElementById('role-msg');

    const name = nameInput?.value.trim().toUpperCase();
    const description = descInput?.value.trim();

    if (!name) {
        if (msgEl) { msgEl.textContent = 'Vui lòng nhập tên role.'; msgEl.style.color = 'red'; }
        return;
    }

    if (msgEl) { msgEl.textContent = 'Đang tạo...'; msgEl.style.color = 'var(--admin-text-muted)'; }

    const result = await callApi(USER_API_BASE_URL, '/roles', 'POST', {
        name,
        description,
        permission: []
    }, true);

    if (result.ok) {
        if (msgEl) { msgEl.textContent = `✅ Tạo role "${name}" thành công!`; msgEl.style.color = 'green'; }
        if (nameInput) nameInput.value = '';
        if (descInput) descInput.value = '';
        await loadAdminRoles();
        setTimeout(() => { if (msgEl) msgEl.textContent = ''; }, 4000);
    } else {
        const err = result.data?.message || result.error || 'Tạo role thất bại.';
        if (msgEl) { msgEl.textContent = `❌ Lỗi: ${err}`; msgEl.style.color = 'red'; }
    }
}

async function handleDeleteRole(roleName) {
    const result = await callApi(USER_API_BASE_URL, `/roles/${encodeURIComponent(roleName)}`, 'DELETE', null, true);
    if (result.ok || result.status === 204) {
        alert(`Đã xóa role "${roleName}" thành công!`);
        await loadAdminRoles();
    } else {
        alert(`Lỗi xóa role: ${result.data?.message || result.error || 'Xóa thất bại.'}`);
    }
}