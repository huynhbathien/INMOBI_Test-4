# INMOBI_Test-4 (English)

This is a backend REST API for the Java 4 interview assignment (number-guessing game), built with Java + Spring Boot, with JWT security, OAuth2 login support, OTP email flows, and concurrency-safe behavior.

## 1. Assignment Requirements

### 1.1 Required APIs

- POST /register: register an account
- POST /login: log in
- POST /guess: guess a number from 1-5
- POST /buy-turns: buy 5 extra turns
- GET /leaderboard: top 10 users by score
- GET /me: current user profile (email, score, turns)

### 1.2 Business rules

- Users can only guess when turns > 0
- Each guess consumes 1 turn
- Correct guess gives +1 score
- Random number is in [1..5]
- Win rate is configurable (target: 5%)

### 1.3 Security and scalability expectations

- APIs are protected with authentication
- /guess must be correct under concurrent requests from the same user
- /leaderboard and /me should stay fast under high user traffic

## 2. Current Implementation Coverage

### 2.1 Implemented endpoints (with /api context path)

- POST /api/register
- POST /api/login
- POST /api/guess
- POST /api/buy-turns
- GET /api/leaderboard
- GET /api/me
- GET /api/leaderboard/me (extra: returns current user profile + rank)

### 2.2 Security

- JWT-based authentication for business APIs
- active=true checks before protected game/auth actions (soft-delete support)
- IP-based brute-force protection on login (rate limiting)

### 2.3 Concurrency correctness

- Write transactions for /guess and /buy-turns
- Pessimistic row locking per user to prevent race conditions on turns/score updates

### 2.4 Read performance

- /leaderboard uses projection query and top 10 limit at DB level
- Leaderboard caching with cache eviction after score/game state updates
- /me uses unique username lookup

## 3. Design And Code Quality

- Clear layering: controller -> service -> repository
- Enum-based response code/message constants (avoid hard-coded strings)
- MapStruct mappers for Entity/Projection -> DTO
- Request validation via jakarta validation
- Unit tests for auth/game/email services

## 4. Extra Supported Capabilities Beyond Minimum Scope

These are value-added capabilities for evaluation:

- OAuth2 login (Google): /oauth2/authorization/google
- OTP email for email verification and password reset
- Refresh token and logout flow
- Global exception handling with unified API response format
- Redis token/session support (revoke/session management)
- Logback logging + Actuator endpoints for operations

## 5. Environment Setup

Requirements:

- Java 21
- Maven 3.9+
- MySQL
- Redis

Minimum environment variables (reference):

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

Run tests:

```bash
mvn test
```

## 7. Quick API Checks

Register:

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

Login:

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"Password@123"}'
```

Buy turns:

```bash
curl -X POST http://localhost:8080/api/buy-turns \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Guess:

```bash
curl -X POST http://localhost:8080/api/guess \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"guessNumber":3}'
```

Get leaderboard:

```bash
curl -X GET http://localhost:8080/api/leaderboard \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Get current game profile:

```bash
curl -X GET http://localhost:8080/api/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Get current leaderboard profile + rank:

```bash
curl -X GET http://localhost:8080/api/leaderboard/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```
