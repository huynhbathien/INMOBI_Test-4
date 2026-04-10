# INMOBI_Test-4 (Bản Tiếng Việt)

Backend REST API cho bài test Java 4 (Game đoán số), xây dựng bằng Java + Spring Boot, có JWT security, hỗ trợ OAuth2 login, OTP email, và tối ưu cho tính đúng đắn khi concurrent.

## 1. Yêu Cầu Bài Toán

### 1.1 API chính

- POST /register: đăng ký tài khoản
- POST /login: đăng nhập
- POST /guess: đoán số từ 1-5
- POST /buy-turns: mua thêm 5 lượt chơi
- GET /leaderboard: top 10 người dùng có score cao nhất
- GET /me: thông tin người dùng hiện tại (email, score, turns)

### 1.2 Rule nghiệp vụ

- Chỉ được đoán khi turns > 0
- Mỗi lần đoán sẽ trừ 1 turn
- Đoán đúng thì score +1
- Số random trong [1..5]
- Win-rate có thể cấu hình (mục tiêu 5%)

### 1.3 Bảo mật và khả năng mở rộng

- API được bảo vệ bởi cơ chế xác thực
- Cần xử lý đúng đắn khi 1 user gọi /guess đồng thời nhiều request
- /leaderboard và /me cần trả kết quả nhanh khi lượng user lớn

## 2. Mức Độ Đáp Ứng

### 2.1 Endpoint thực tế (có context-path /api)

- POST /api/register
- POST /api/login
- POST /api/guess
- POST /api/buy-turns
- GET /api/leaderboard
- GET /api/me
- GET /api/leaderboard/me (bổ sung: trả profile + rank hiện tại trên BXH)

### 2.2 Bảo mật

- JWT cho API business flow
- Có check user active=true trước khi cho thao tác (soft-delete support)
- Có brute-force protection cho login theo IP (rate limit login attempts)

### 2.3 Đúng đắn khi concurrent

- /guess và /buy-turns dùng transaction ghi
- Sử dụng pessimistic lock theo user khi cập nhật turns/score để tránh race condition

### 2.4 Hiệu năng cho read APIs

- /leaderboard dùng projection + top 10 ở tầng DB
- Có cache cho leaderboard, auto-evict khi score/thông tin chơi thay đổi
- /me truy vấn theo username có index unique

## 3. Thiết Kế Và Chất Lượng Code

- Phân tách layer rõ ràng: controller -> service -> repository
- Enum hóa message/code cho game và auth để tránh hard-code string
- MapStruct mapper cho Entity/Projection -> DTO
- Validation request với jakarta validation
- Unit test cho auth/game/email service

## 4. Chức Năng Hỗ Trợ Ngoài Yêu Cầu Đề Bài

Đây là các điểm cộng để người review đánh giá hệ thống vượt qua scope tối thiểu:

- OAuth2 login (Google): /oauth2/authorization/google
- OTP email cho verify email và reset password
- Refresh token và logout flow
- Global exception handling, response format thống nhất
- Redis token/session support (cho revoke và quản lý session)
- Logback logging + Actuator endpoints cho vận hành

## 5. Cấu Hình Môi Trường

Yêu cầu:

- Java 21
- Maven 3.9+
- MySQL
- Redis

Biến môi trường tối thiểu (tham khảo):

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/game_db
SPRING_DATASOURCE_USERNAME=your_user
SPRING_DATASOURCE_PASSWORD=your_password

SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

JWT_SECRET_FILE=secrets/jwt_hs512_key.txt
TOKEN_ACCESS_EXPIRATION=1800
TOKEN_REFRESH_EXPIRATION=604800

OTP_EXPIRY_MINUTES=10
GAME_WIN_RATE=0.05
```

## 6. Build, Run, Test

```bash
mvn clean package
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Chạy test:

```bash
mvn test
```

## 7. Test Nhanh API

Đăng ký:

```bash
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"player1",
    "email":"player1@example.com",
    "password":"Password@123",
    "confirmPassword":"Password@123",
    "fullName":"Player One"
  }'
```

Đăng nhập:

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"Password@123"}'
```

Mua turns:

```bash
curl -X POST http://localhost:8080/api/buy-turns \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Đoán số:

```bash
curl -X POST http://localhost:8080/api/guess \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"guessNumber":3}'
```

Xem leaderboard:

```bash
curl -X GET http://localhost:8080/api/leaderboard \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Xem profile game hiện tại:

```bash
curl -X GET http://localhost:8080/api/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Xem leaderboard profile + rank hiện tại:

```bash
curl -X GET http://localhost:8080/api/leaderboard/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```
