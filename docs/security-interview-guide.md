# Security Interview Guide (Du an course-application)

Tai lieu nay tong hop chi tiet cac flow va thanh phan security trong code hien tai de ban dung cho phong van.

## 1. Tong quan kien truc security

### 1.1 Security stack dang su dung

- Spring Security 6: Authentication, Authorization, filter chain, method security.
- OAuth2 Client: Dang nhap Google (co the mo rong provider khac).
- JWT (jjwt): Tao va verify access token, refresh token.
- Redis: Luu refresh token, access token (optional tracking), user session, token blacklist, brute-force counters.
- Bucket4j: Rate limiting theo IP.
- BCrypt: Hash mat khau.

### 1.2 Cac file trung tam

- [src/main/java/com/mycompany/config/SecurityConfig.java](../src/main/java/com/mycompany/config/SecurityConfig.java)
- [src/main/java/com/mycompany/security/JwtAuthenticationFilter.java](../src/main/java/com/mycompany/security/JwtAuthenticationFilter.java)
- [src/main/java/com/mycompany/config/JwtExceptionHandlerFilter.java](../src/main/java/com/mycompany/config/JwtExceptionHandlerFilter.java)
- [src/main/java/com/mycompany/security/RateLimitFilter.java](../src/main/java/com/mycompany/security/RateLimitFilter.java)
- [src/main/java/com/mycompany/security/OAuth2LoginSuccessHandler.java](../src/main/java/com/mycompany/security/OAuth2LoginSuccessHandler.java)
- [src/main/java/com/mycompany/security/JwtUtils.java](../src/main/java/com/mycompany/security/JwtUtils.java)
- [src/main/java/com/mycompany/controller/AuthController.java](../src/main/java/com/mycompany/controller/AuthController.java)
- [src/main/java/com/mycompany/service/Impl/AuthSessionServiceImpl.java](../src/main/java/com/mycompany/service/Impl/AuthSessionServiceImpl.java)
- [src/main/java/com/mycompany/service/TokenRedisService.java](../src/main/java/com/mycompany/service/TokenRedisService.java)
- [src/main/resources/application.yml](../src/main/resources/application.yml)
- [pom.xml](../pom.xml)

## 2. Filter chain va endpoint rules

Theo [SecurityConfig.java](../src/main/java/com/mycompany/config/SecurityConfig.java):

- Permit all:
  - /auth/\*\*
  - /home/\*\*
  - /oauth2/\*\*
  - /login/\*\*
- /admin/\*\* yeu cau role ADMIN.
- Tat ca endpoint con lai yeu cau authenticated.

### Thu tu filter custom

1. RateLimitFilter (addFilterBefore rateLimitFilter truoc JwtAuthenticationFilter)
2. JwtExceptionHandlerFilter (truoc JwtAuthenticationFilter)
3. JwtAuthenticationFilter (truoc UsernamePasswordAuthenticationFilter)

Y nghia:

- Request bi gioi han toc do se bi chan som nhat.
- Loi lien quan JWT duoc bat va tra JSON response thay vi vo Runtime Exception chung.
- Neu token hop le, SecurityContext duoc gan truoc khi vao controller.

## 3. Flow login username/password

### 3.1 Entry point

- Endpoint: POST /auth/login tai [AuthController.java](../src/main/java/com/mycompany/controller/AuthController.java)
- Input: LoginRequestDTO
- Lay client IP qua RequestUtils.resolveClientIp(...)

### 3.2 Cac buoc chi tiet

1. Controller goi authService.login(dto, clientIp).
2. Service thuc thi o AuthSessionServiceImpl.login(...):
   - Kiem tra IP co dang bi block khong: loginAttemptService.isBlocked(clientIp).
   - Tim user theo username: userRepository.findByUsername(...).
   - Verify password: passwordEncoder.matches(raw, encoded).
   - Neu sai:
     - Tang dem that bai: loginAttemptService.loginFailed(clientIp).
     - Nem BadCredentialsException.
   - Neu email chua verify: nem ResponseStatusException 403.
   - Neu thanh cong:
     - loginAttemptService.loginSucceeded(clientIp) de clear counter.
     - issueTokenPair(username).
3. issueTokenPair(username):
   - Tao access token: jwtUtils.generateToken(...).
   - Tao refresh token: jwtUtils.generateRefreshToken(...).
   - Luu access token: accessTokenStore.saveAccessToken(...).
   - Luu refresh token: refreshTokenStore.saveRefreshToken(...).
4. Controller set refresh token vao HttpOnly cookie qua RequestUtils.addHttpOnlyCookie(...).

### 3.3 Ham thu vien duoc dung

- Spring Security:
  - PasswordEncoder.matches(...)
  - BadCredentialsException
- Spring Data JPA:
  - UserRepository.findByUsername(...)
- Redis (Spring Data Redis thong qua util):
  - RedisTemplate.opsForValue().increment(...)
  - RedisTemplate.expire(...)
  - RedisTemplate.delete(...)
- JWT (jjwt):
  - Jwts.builder().subject(...).expiration(...).issuedAt(...).signWith(...).compact()

## 4. Flow refresh token

### 4.1 Entry point

- Endpoint: POST /auth/refresh tai [AuthController.java](../src/main/java/com/mycompany/controller/AuthController.java)
- Controller doc cookie refreshToken bang RequestUtils.resolveCookieValue(...)

### 4.2 Cac buoc chi tiet

1. Neu khong co cookie refreshToken -> 401 REFRESH_TOKEN_NOT_FOUND.
2. AuthSessionServiceImpl.refreshToken(clientRefreshToken):
   - jwtUtils.isTokenExpired(...)
   - username = jwtUtils.getUserNameFromToken(...)
   - storedRefreshToken = refreshTokenStore.getRefreshToken(username)
   - So khop storedRefreshToken voi token client gui len.
   - Neu mismatch: xoa refresh token cu va tra 401.
   - Tao cap token moi (rotate): access + refresh.
   - Luu lai vao Redis.
3. Controller ghi de refresh cookie moi.

### 4.3 Ham thu vien duoc dung

- JWT parser (jjwt):
  - Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
- Spring Web:
  - ResponseStatusException
- Redis util:
  - get, set, delete, expire

## 5. Flow logout

### 5.1 Entry point

- Endpoint: POST /auth/logout tai [AuthController.java](../src/main/java/com/mycompany/controller/AuthController.java)

### 5.2 Cac buoc chi tiet

1. resolveAuthenticatedUsername() doc tu SecurityContextHolder.getContext().getAuthentication().
2. authService.logout(username) -> AuthSessionServiceImpl.logout(username):
   - accessTokenStore.deleteAccessToken(username)
   - refreshTokenStore.deleteRefreshToken(username)
3. Xoa refresh cookie phia client (maxAge=0).

### 5.3 Ham thu vien duoc dung

- Spring Security:
  - SecurityContextHolder
  - Authentication / AnonymousAuthenticationToken
- Spring Web cookie:
  - ResponseCookie.from(...).httpOnly(true).secure(true).sameSite("Strict")

### 5.4 Luu y phong van

- Code hien tai KHONG add access token vao blacklist khi logout.
- Nghia la access token da phat van co the dung den khi het han neu client van giu token.
- He thong co san interface va implementation blacklist (TokenBlacklistService / TokenRedisService), nhung flow logout chua goi addToBlacklist(...).

## 6. Flow OAuth2 success

### 6.1 Entry point

- oauth2Login().successHandler(oAuth2LoginSuccessHandler) trong [SecurityConfig.java](../src/main/java/com/mycompany/config/SecurityConfig.java)

### 6.2 Cac buoc trong OAuth2LoginSuccessHandler.onAuthenticationSuccess(...)

1. Cast Authentication -> OAuth2AuthenticationToken.
2. Lay registrationId (google/github/...)
3. Lay OAuth2User principal.
4. Rut trich principal name theo thu tu:
   - name
   - email
   - login
   - id
5. load authorized client: OAuth2AuthorizedClientService.loadAuthorizedClient(...)
6. jwtUtils.processOAuth2User(...):
   - map provider ID
   - tim user theo providerId/email
   - tao moi hoac update user
   - luu user vao DB
   - tra ve UserDetails noi bo
7. Replace SecurityContext principal bang UsernamePasswordAuthenticationToken noi bo.
8. Tao access token + refresh token.
9. Luu refresh token vao Redis qua refreshTokenStore.saveRefreshToken(...)
10. Tra JSON response {token, username, userId, provider, message}.

### 6.3 Ham thu vien duoc dung

- Spring Security OAuth2:
  - OAuth2AuthenticationToken
  - OAuth2User
  - OAuth2AuthorizedClientService.loadAuthorizedClient(...)
- Spring Security core:
  - UsernamePasswordAuthenticationToken
  - SecurityContextHolder
- Jackson:
  - ObjectMapper.writeValueAsString(...)
- JPA:
  - userRepository.save(...)

## 7. Rate limit va check IP

### 7.1 RateLimitFilter

Tai [RateLimitFilter.java](../src/main/java/com/mycompany/security/RateLimitFilter.java):

- Rule theo IP:
  - POST /auth/login: 5 request/phut
  - POST /auth/register: 3 request/phut
  - Con lai: 60 request/phut

### 7.2 Cac buoc

1. resolveClientIp() uu tien X-Forwarded-For, sau do X-Real-IP, cuoi cung request.getRemoteAddr().
2. Chon bucket theo path + method.
3. bucket.tryConsume(1):
   - true -> cho qua
   - false -> tra 429 + Retry-After: 60

### 7.3 Ham thu vien duoc dung

- Bucket4j:
  - Bucket.builder()
  - Bandwidth.builder().capacity(...).refillGreedy(...)
  - Bucket.tryConsume(...)
- Servlet API:
  - HttpServletRequest.getHeader(...)
  - HttpServletResponse.setStatus(...)

## 8. Brute-force protection

Tai [LoginAttemptService.java](../src/main/java/com/mycompany/security/LoginAttemptService.java):

- Key pattern:
  - login_attempt:{key}
  - login_blocked:{key}
- loginFailed(key):
  - increment dem
  - refresh TTL cua attempt key
  - neu vuot maxAttempts -> set blocked key voi TTL
- isBlocked(key): check ton tai blocked key
- loginSucceeded(key): xoa key dem va key block
- getRemainingAttempts(key): tinh so lan con lai

Config doc tu [application.yml](../src/main/resources/application.yml):

- security.brute-force.max-attempts
- security.brute-force.block-duration-minutes

## 9. JWT xu ly trong request

### 9.1 JwtAuthenticationFilter

1. Doc Authorization header.
2. Neu co Bearer token:
   - Kiem tra blacklist: tokenBlacklistService.isTokenBlacklisted(token)
   - Neu khong blacklist:
     - userName = jwtUtils.getUserNameFromToken(token)
     - load UserDetails
     - jwtUtils.validateToken(token, userDetails)
     - set SecurityContext
   - Neu blacklist -> throw RuntimeException("Token is blacklisted")
3. chain.doFilter(...)

### 9.2 JwtExceptionHandlerFilter

- Bao quanh chain de bat:
  - TokenExpiredException
  - TokenRevokedException
  - RuntimeException co thong diep lien quan token/jwt/expired/revoked
- Clear SecurityContext va tra 401 JSON APIResponse.

### 9.3 Ham thu vien duoc dung

- Spring Security:
  - OncePerRequestFilter
  - SecurityContextHolder
  - UsernamePasswordAuthenticationToken
- JWT (jjwt):
  - parser + claims extraction

## 10. Token storage model trong Redis

Tai [TokenRedisService.java](../src/main/java/com/mycompany/service/TokenRedisService.java):

- Refresh token:
  - Key: refresh_token:{username}
  - TTL: token.refresh-token-expiration
- Access token (tracking optional):
  - Key: access_token:{username}
  - TTL: token.access-token-expiration
- User session hash:
  - Key: user_session:{username}
  - Field: userId, refreshToken, loginTime
- Blacklist:
  - Key: token_blacklist:{token}
  - TTL: den het han token

## 11. Authorization o method level

@EnableMethodSecurity(prePostEnabled = true) da bat trong [SecurityConfig.java](../src/main/java/com/mycompany/config/SecurityConfig.java).

Project su dung @PreAuthorize tren controller, vi du:

- [src/main/java/com/mycompany/controller/AdminController.java](../src/main/java/com/mycompany/controller/AdminController.java)
- [src/main/java/com/mycompany/controller/CourseController.java](../src/main/java/com/mycompany/controller/CourseController.java)
- [src/main/java/com/mycompany/controller/LessonController.java](../src/main/java/com/mycompany/controller/LessonController.java)
- [src/main/java/com/mycompany/controller/UserController.java](../src/main/java/com/mycompany/controller/UserController.java)

Ngoai ra co annotation [CurrentUser.java](../src/main/java/com/mycompany/security/CurrentUser.java) su dung @AuthenticationPrincipal de inject principal.

## 12. Cookie va transport security

Tai [RequestUtils.java](../src/main/java/com/mycompany/util/RequestUtils.java):

- Refresh token cookie duoc tao voi:
  - HttpOnly = true
  - Secure = true
  - SameSite = Strict
  - Path = /auth/refresh

Luu y khi phong van:

- Secure=true yeu cau HTTPS. Neu test local HTTP thi cookie co the khong duoc browser gui.

## 13. CORS va CSRF

- CORS cho phep origin:
  - https://localhost:3000
  - https://localhost:8080
- CSRF dang disable trong SecurityConfig.

Giai thich phong van:

- Kien truc dang su dung JWT bearer + API stateless, nen thuong disable CSRF.
- Tuy nhien dang co refresh token trong cookie, can can nhac CSRF strategy bo sung neu mo rong cho browser-based flow.

## 14. Mapping thu vien -> function dang dung

### Spring Security

- HttpSecurity.cors(...), csrf(...), authorizeHttpRequests(...), oauth2Login(...), addFilterBefore(...)
- SecurityContextHolder.getContext().getAuthentication(), setAuthentication(...), clearContext()
- UsernamePasswordAuthenticationToken(...)
- UserDetailsService.loadUserByUsername(...)
- PasswordEncoder.matches(...), BCryptPasswordEncoder
- @PreAuthorize(...), @AuthenticationPrincipal

### Spring Security OAuth2

- OAuth2AuthenticationToken.getAuthorizedClientRegistrationId()
- OAuth2AuthorizedClientService.loadAuthorizedClient(...)
- OAuth2User.getAttribute(...)

### JWT (io.jsonwebtoken / jjwt)

- Jwts.builder().subject().expiration().issuedAt().claim().signWith().compact()
- Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
- Keys.hmacShaKeyFor(...)
- Jwts.SIG.HS512.key().build()

### Redis (Spring Data Redis)

- RedisTemplate.opsForValue().set/get/increment
- RedisTemplate.expire(...)
- RedisTemplate.hasKey(...)
- RedisTemplate.delete(...)
- Hash ops thong qua RedisUtil.hset/hget

### Bucket4j

- Bucket.builder().addLimit(...)
- Bandwidth.builder().capacity(...).refillGreedy(...)
- Bucket.tryConsume(1)

### Spring Web/Servlet

- HttpServletRequest.getHeader(...), getServletPath(), getRemoteAddr()
- HttpServletResponse.setStatus(), setHeader(), getWriter().write(...)
- ResponseCookie.from(...).httpOnly().secure().sameSite().maxAge()

### Jackson

- ObjectMapper.writeValueAsString(...)

## 15. Cac diem manh/yeu de tra loi phong van

### Diem manh

- Co day du login/register/verify-email/refresh/logout.
- Co ket hop nhieu lop bao ve:
  - Rate-limit theo IP (Bucket4j)
  - Brute-force counter bang Redis
  - JWT + method-level authorization
- OAuth2 va local login dung chung token model.
- Cookie refresh token dat HttpOnly + SameSite=Strict.

### Diem can cai thien

- Logout chua blacklist access token hien tai.
- JwtAuthenticationFilter nem RuntimeException khi token blacklisted, nen doi sang TokenRevokedException de ro nghia hon.
- Principal khong dong nhat:
  - JWT filter set principal la username String.
  - OAuth2 flow set principal la UserDetails.
  - Co the anh huong @CurrentUser o mot so endpoint.
- RateLimitFilter dang luu bucket map in-memory, scale ngang nhieu instance can doi sang distributed store.

## 16. Cheat sheet tra loi nhanh trong phong van

1. Login local di qua gi?

- AuthController -> AuthSessionServiceImpl -> UserRepository + PasswordEncoder -> JwtUtils -> TokenRedisService -> set refresh cookie.

2. OAuth2 thanh cong di qua gi?

- OAuth2LoginSuccessHandler -> JwtUtils.processOAuth2User -> UserRepository.save -> tao JWT cap moi -> luu refresh Redis -> tra JSON token.

3. Logout da revoke token chua?

- Dang xoa token luu Redis theo username, chua blacklist access token dang cam tren client.

4. He thong chan tan cong dang nhap the nao?

- Layer 1: Bucket4j rate-limit theo IP.
- Layer 2: LoginAttemptService block theo IP bang Redis TTL.

5. JWT loi se xu ly ra sao?

- JwtExceptionHandlerFilter bat exception lien quan token, clear SecurityContext, tra 401 JSON co ma loi.

## 17. Goi y nang cap de trinh bay them

- Blacklist access token ngay khi logout/password reset.
- Chuan hoa principal trong JwtAuthenticationFilter thanh CustomUserDetails thay vi String.
- Tach loginAttempt key theo combo username+ip de tinh te hon.
- Neu deploy multi-instance, chuyen rate-limit state sang Redis hoac gateway layer.

---

Tai lieu duoc tao tu source code hien tai cua project, phu hop de on phong van theo huong practical architecture + code-level details.
