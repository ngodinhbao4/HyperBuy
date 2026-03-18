-- =====================================================
-- 🗄️ Khởi tạo toàn bộ database cho hệ thống HyperBuy
-- =====================================================

-- Tạo database cho UserService
CREATE DATABASE IF NOT EXISTS hyperbuy_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho ProductService
CREATE DATABASE IF NOT EXISTS hyperbuy_product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho CartService
CREATE DATABASE IF NOT EXISTS hyperbuy_cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho OrderService
CREATE DATABASE IF NOT EXISTS hyperbuy_order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho PaymentService
CREATE DATABASE IF NOT EXISTS hyperbuy_payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho NotificationService
CREATE DATABASE IF NOT EXISTS hyperbuy_notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho ChatService
CREATE DATABASE IF NOT EXISTS hyperbuy_chat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho MiniGameService
CREATE DATABASE IF NOT EXISTS hyperbuy_minigame_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo database cho VoucherService
CREATE DATABASE IF NOT EXISTS hyperbuy_voucher_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
