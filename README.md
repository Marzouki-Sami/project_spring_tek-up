# Shop

An online store built with Spring Boot: a product catalog with stock and suppliers today,
carts, orders and role-based access next.

> Work in progress. This repository started as a university exercise and is being rebuilt
> step by step into a complete, tested application. See the roadmap below.

## Stack

Java 21, Spring Boot 3.5, Spring Data JPA, Thymeleaf, Bean Validation, MapStruct, Flyway.
H2 for development and tests, MySQL for production.

## Run it

```bash
./mvnw spring-boot:run          # in-memory H2, nothing to install
./mvnw test                     # unit and web tests
```

Open http://localhost:8081.

With MySQL:

```bash
DB_URL=jdbc:mysql://localhost:3306/shop DB_USERNAME=shop DB_PASSWORD=... \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

The schema is owned by Flyway migrations in `src/main/resources/db/migration`; Hibernate only
validates that the entities match it.

## Roadmap

1. Foundation: Java 21, Spring Boot 3.5, Flyway, domain model, catalog pages, tests (done)
2. Security: BCrypt passwords, form login, JWT for the API, roles CUSTOMER / MANAGER / ADMIN
3. Catalog UI and REST API with OpenAPI docs
4. Cart, orders, stock movements and coupons
5. Reviews, wishlist, emails and an admin dashboard
6. Docker Compose, CI and screenshots