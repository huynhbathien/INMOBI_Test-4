# SOLID và Design Patterns trong dự án Course-SPR-2026

Tài liệu này tổng hợp:

- Lý thuyết ngắn gọn của SOLID
- Ví dụ thực tế đang có trong code của dự án
- Các design pattern đang được sử dụng

## 1) SOLID trong dự án

## S — Single Responsibility Principle (SRP)

Một class nên chỉ có một lý do để thay đổi, tức là tập trung vào một trách nhiệm chính.

Ví dụ thực tế trong dự án:

- AuthController chỉ xử lý HTTP layer (nhận request, trả response), còn business logic chuyển cho service.
  - com/mycompany/controller/AuthController.java
- AuthAccountServiceImpl tập trung vào luồng tài khoản: đăng ký, xác thực email, quên mật khẩu, reset mật khẩu.
  - com/mycompany/service/Impl/AuthAccountServiceImpl.java
- AuthSessionServiceImpl tập trung vào phiên đăng nhập: login, refresh token, logout.
  - com/mycompany/service/Impl/AuthSessionServiceImpl.java

Giá trị thực tế:

- Dễ test theo tầng (controller test khác service test)
- Khi đổi logic token không ảnh hưởng logic đăng ký tài khoản

## O — Open/Closed Principle (OCP)

Mở để mở rộng, đóng để sửa đổi trực tiếp code cũ.

Ví dụ thực tế trong dự án:

- Email OTP sử dụng chiến lược theo loại OTP:
  - OtpEmailTemplateStrategy định nghĩa contract
    - com/mycompany/service/email/OtpEmailTemplateStrategy.java
  - VerifyEmailTemplateStrategy và ResetPasswordEmailTemplateStrategy là 2 biến thể
    - com/mycompany/service/email/VerifyEmailTemplateStrategy.java
    - com/mycompany/service/email/ResetPasswordEmailTemplateStrategy.java
  - EmailServiceImpl map strategy theo OtpType và chọn runtime
    - com/mycompany/service/Impl/EmailServiceImpl.java

Mở rộng thực tế:

- Thêm loại OTP mới (ví dụ CHANGE_EMAIL) chỉ cần tạo strategy mới và OtpType mới, không cần sửa luồng gửi email chính.

## L — Liskov Substitution Principle (LSP)

Class con/implementation phải thay thế được cho abstraction mà không làm sai hành vi mong đợi.

Ví dụ thực tế trong dự án:

- Các implementation của OtpEmailTemplateStrategy có thể thay thế lẫn nhau qua interface trong EmailServiceImpl.
  - com/mycompany/service/email/OtpEmailTemplateStrategy.java
  - com/mycompany/service/email/VerifyEmailTemplateStrategy.java
  - com/mycompany/service/email/ResetPasswordEmailTemplateStrategy.java
  - com/mycompany/service/Impl/EmailServiceImpl.java
- SmtpEmailSenderServiceImpl thay thế cho abstraction EmailSenderService.
  - com/mycompany/service/EmailSenderService.java
  - com/mycompany/service/Impl/SmtpEmailSenderServiceImpl.java

Ý nghĩa thực tế:

- Có thể đổi kênh gửi mail (SMTP -> provider khác) mà luồng nghiệp vụ OTP không cần đổi.

## I — Interface Segregation Principle (ISP)

Nhiều interface nhỏ, chuyên biệt tốt hơn một interface lớn bắt mọi nơi phải implement dư thừa.

Ví dụ thực tế trong dự án:

- Token được tách thành các interface nhỏ:
  - AccessTokenStore
    - com/mycompany/service/AccessTokenStore.java
  - RefreshTokenStore
    - com/mycompany/service/RefreshTokenStore.java
  - TokenBlacklistService
    - com/mycompany/service/TokenBlacklistService.java
  - UserSessionQueryService
    - com/mycompany/service/UserSessionQueryService.java
- TokenRedisService implement nhiều interface nhỏ, còn từng service chỉ phụ thuộc đúng phần mình cần.
  - com/mycompany/service/TokenRedisService.java
  - com/mycompany/service/Impl/AuthAccountServiceImpl.java
  - com/mycompany/service/Impl/AuthSessionServiceImpl.java

Lợi ích thực tế:

- AuthAccountServiceImpl chỉ gọi delete token cần thiết, không phải phụ thuộc toàn bộ API của token system.

## D — Dependency Inversion Principle (DIP)

Module cấp cao không phụ thuộc module cấp thấp; cả hai phụ thuộc abstraction.

Ví dụ thực tế trong dự án:

- Controller phụ thuộc interface service, không phụ thuộc class cụ thể.
  - AuthController -> AuthService
    - com/mycompany/controller/AuthController.java
    - com/mycompany/service/AuthService.java
- JwtAuthenticationFilter phụ thuộc TokenBlacklistService (abstraction), không phụ thuộc trực tiếp Redis.
  - com/mycompany/security/JwtAuthenticationFilter.java
  - com/mycompany/service/TokenBlacklistService.java
- EmailServiceImpl phụ thuộc EmailSenderService và OtpEmailTemplateStrategy abstraction.
  - com/mycompany/service/Impl/EmailServiceImpl.java
  - com/mycompany/service/EmailSenderService.java

Lợi ích thực tế:

- Dễ mock khi test
- Dễ thay implementation theo môi trường

---

## 2) Các Design Pattern đang dùng trong dự án

## 2.1 Strategy Pattern

Ý tưởng: đóng gói các thuật toán/biến thể hành vi thành các strategy và chọn strategy lúc runtime.

Trong dự án:

- OtpEmailTemplateStrategy là strategy interface
- VerifyEmailTemplateStrategy và ResetPasswordEmailTemplateStrategy là concrete strategy
- EmailServiceImpl chọn strategy theo OtpType

File liên quan:

- com/mycompany/service/email/OtpEmailTemplateStrategy.java
- com/mycompany/service/email/VerifyEmailTemplateStrategy.java
- com/mycompany/service/email/ResetPasswordEmailTemplateStrategy.java
- com/mycompany/service/Impl/EmailServiceImpl.java

## 2.2 Repository Pattern

Ý tưởng: tách logic truy cập dữ liệu ra khỏi business logic.

Trong dự án:

- Các repository kế thừa JpaRepository: UserRepository, CourseRepository, LessonRepository...
- Service chỉ gọi repository thay vì thao tác SQL trực tiếp

File liên quan:

- com/mycompany/repository/UserRepository.java
- com/mycompany/repository/CourseRepository.java
- com/mycompany/repository/LessonRepository.java
- com/mycompany/service/Impl/CourseServiceImpl.java

## 2.3 Dependency Injection (IoC)

Ý tưởng: object nhận dependency từ container thay vì tự new.

Trong dự án:

- Constructor injection qua Lombok RequiredArgsConstructor hoặc constructor thủ công
- Spring inject các bean service/repository/filter

File liên quan:

- com/mycompany/config/SecurityConfig.java
- com/mycompany/service/Impl/AuthServiceImpl.java
- com/mycompany/service/Impl/EmailServiceImpl.java

## 2.4 Builder Pattern

Ý tưởng: tạo object phức tạp theo từng bước, rõ ràng và dễ đọc.

Trong dự án:

- APIResponse dùng Lombok Builder và static factory success/error
- Một số DTO response dùng Builder
- Bucket4j và JWT cũng dùng fluent builder API

File liên quan:

- com/mycompany/dto/APIResponse.java
- com/mycompany/dto/response/AdminStatsResponse.java
- com/mycompany/security/RateLimitFilter.java
- com/mycompany/security/JwtUtils.java

## 2.5 Chain of Responsibility (Filter Chain)

Ý tưởng: request đi qua chuỗi handler/filter, mỗi filter xử lý một phần rồi chuyển tiếp.

Trong dự án:

- SecurityFilterChain cấu hình thứ tự filter:
  - rateLimitFilter
  - jwtExceptionHandlerFilter
  - jwtAuthenticationFilter
- Mỗi filter phụ trách đúng phần của mình

File liên quan:

- com/mycompany/config/SecurityConfig.java
- com/mycompany/config/JwtExceptionHandlerFilter.java
- com/mycompany/security/JwtAuthenticationFilter.java
- com/mycompany/security/RateLimitFilter.java

## 2.6 Facade/Application Service Pattern (mức ứng dụng)

Ý tưởng: một service đứng trước điều phối nhiều service con để cung cấp API đơn giản hơn cho controller.

Trong dự án:

- AuthServiceImpl là điểm vào thống nhất cho auth flow
- Bên trong tách sang AuthAccountService và AuthSessionService

File liên quan:

- com/mycompany/service/AuthService.java
- com/mycompany/service/Impl/AuthServiceImpl.java
- com/mycompany/service/AuthAccountService.java
- com/mycompany/service/AuthSessionService.java

## 2.7 Data Mapper Pattern

Ý tưởng: tách chuyển đổi giữa domain/entity và DTO.

Trong dự án:

- MapStruct mapper chuyển đổi entity <-> DTO

File liên quan:

- com/mycompany/mapstruct/CourseMapper.java
- com/mycompany/mapstruct/UserMapper.java
- com/mycompany/mapstruct/LessonMapper.java

---

## 3) Nhận xét nhanh cho kiến trúc hiện tại

- Dự án áp dụng SOLID khá tốt ở tầng service và security.
- Strategy cho email OTP là điểm thiết kế tốt, dễ mở rộng.
- Token layer tách interface nhỏ là ví dụ ISP rõ ràng.
- Có thể cải thiện thêm bằng cách tách một phần logic validation dài trong service thành domain policy riêng nếu quy mô tiếp tục tăng.

---

## 4) Khi nào dùng interface, khi nào dùng abstract class?

## Nên dùng interface khi

- Bạn muốn định nghĩa contract/hành vi chung cho nhiều implementation khác nhau.
- Bạn cần tận dụng DI, test mock dễ dàng, và thay implementation theo môi trường.
- Các class có thể thuộc nhiều nhóm hành vi khác nhau (một class có thể implement nhiều interface).

Ví dụ trong dự án:

- AuthService, AuthAccountService, AuthSessionService tách contract cho từng luồng auth.
  - com/mycompany/service/AuthService.java
  - com/mycompany/service/AuthAccountService.java
  - com/mycompany/service/AuthSessionService.java
- TokenRedisService implement nhiều interface nhỏ (AccessTokenStore, RefreshTokenStore, TokenBlacklistService, UserSessionQueryService).
  - com/mycompany/service/TokenRedisService.java
- OtpEmailTemplateStrategy cho phép thêm template OTP mới mà không sửa luồng gửi chính.
  - com/mycompany/service/email/OtpEmailTemplateStrategy.java

## Nên dùng abstract class khi

- Các class con có logic chung hoặc state chung cần tái sử dụng.
- Bạn muốn cung cấp một phần implementation mặc định để class con kế thừa.
- Bạn muốn gom quy ước chung của một họ class vào một chỗ.

Ví dụ trong dự án:

- BaseEntity chứa state và hành vi chung cho entity (id, createdAt, updatedAt, auditing).
  - com/mycompany/entity/BaseEntity.java
  - com/mycompany/entity/Course.java
  - com/mycompany/entity/Lesson.java
- CourseMapper là abstract class của MapStruct để vừa định nghĩa mapping, vừa có method hỗ trợ chung và khả năng dùng dependency được inject.
  - com/mycompany/mapstruct/CourseMapper.java

## Quy tắc chọn nhanh

- Ưu tiên interface nếu mục tiêu là tách contract và linh hoạt thay implementation.
- Dùng abstract class khi cần chia sẻ code/state thật sự giữa nhiều class con.
- Nếu chỉ có contract, không có shared state/logic: dùng interface.
- Nếu vừa cần contract vừa cần code dùng chung: cân nhắc abstract class (hoặc interface + lớp hỗ trợ riêng).
