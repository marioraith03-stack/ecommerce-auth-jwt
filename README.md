# eCommerce Application (Spring Boot + JWT)

Simple eCommerce backend built with Spring Boot, Hibernate (JPA), and H2.  

Authentication is username/password; authorization uses JWT.  
Sensitive routes are protected and require a valid  
Authorization: Bearer <token>` header.

---

## Tech Stack

- Java 17+, Spring Boot 3, Spring Security 6  
- H2 in-memory DB (auto-seeded)  
- JWT via `java-jwt`  
- JUnit 5 + Spring Test + MockMvc  
- JaCoCo for coverage  
- Logback for structured logging  

---

## Prerequisites

- Java 17 or newer  
- Maven 3.9+  

Verify (cmd):
java -version
mvn -version


---

## Build & Tests (cmd)
mvn clean verify

This runs tests and creates a coverage report at:  
target/site/jacoco/index.html

---

## Run locally (cmd)
mvn spring-boot:run


App will start on:  
http://localhost:8080

---

## Auth Flow (how to use)

### Create user (cmd)
curl -X POST http://localhost:8080/api/user/create \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","confirmPassword":"password123"}'


### Login to receive JWT (cmd)
curl -i -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'


On success, the response includes an Authorization: Bearer <JWT> header.

PowerShell example to capture the token: (Shell)
$resp  = Invoke-WebRequest -Method Post -Uri http://localhost:8080/login `
         -ContentType 'application/json' -Body '{"username":"testuser","password":"password123"}'
$token = $resp.Headers['Authorization']
echo $token

### Call protected endpoints (cmd)
curl -H "Authorization: Bearer <JWT>" http://localhost:8080/api/order/history/testuser


---

## API (selected)

### Public
- POST /api/user/create` – create a new user  
- POST /login` – authenticate and receive JWT  

### Protected (require JWT)
- GET /api/user/id/{id}
- GET /api/user/username/{username}
- GET /api/item
- GET /api/item/id/{id}
- GET /api/item/name/{name}
- POST /api/cart/addToCart
- POST /api/cart/removeFromCart
- POST /api/order/submit/{username}  
- GET /api/order/history/{username}

_All protected routes require:_  
Authorization: Bearer <token>

---

## Logging & Metrics

The application logs:
- CreateUser successes and failures  
- Order submit/history successes and failures  
- Unhandled exceptions  

Logs use Logback (src/main/resources/logback-spring.xml) and can be forwarded  
to Splunk HEC with proper appender configuration.

---

## Tests & Coverage

Run: (cmd)
mvn clean verify

Coverage report:  
target/site/jacoco/index.html
Includes positive & negative cases (Auth flow, UserController, context load).

---

## Notes

- Default DB is H2 in-memory; it resets on restart and seeds initial data via data.sql.  
- JWT secret & expiration are configured in src/main/resources/application.properties.
