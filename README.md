# Shop

An online store built with Spring Boot: a public storefront, customer accounts, a back office
for managers and a REST API with token authentication.

> Work in progress. This repository started as a university exercise and is being rebuilt
> step by step into a complete, tested application. See the roadmap below.

## What works today

- **Storefront**: guests browse, search, filter and sort products without an account.
- **Cart**: guests can fill a basket and keep it through sign-in; it is lost when the session ends.
  Checkout needs an account.
- **Orders**: delivery address, optional percentage coupon, live stock check, order history, customer
  cancellation while an order is still placed, and a manager workflow
  (placed, processing, shipped, delivered, cancelled). Stock is taken atomically so two buyers can never
  oversell, every change is logged in stock_movements, and cancelling returns the items.
- **Reviews and wishlist**: verified buyers can rate and review a product (one review each, editable);
  signed-in users keep a wishlist.
- **Dashboard** (/manage): revenue, orders by status, best sellers and low-stock alerts.
- **Email**: confirmation link at sign-up, password reset, order confirmation, shipping notice and a
  low-stock alert to managers. Links are single-use and stored only as hashes. By default emails are
  saved as text files in mail-outbox/ instead of being sent.
- **Coupons** (/manage/coupons): percentage, optional expiry date and usage limit.
- **Accounts**: sign-up with a strong-password policy, sign-in, profile and password change.
- **Roles**: `CUSTOMER`, `MANAGER` (manages the catalog) and `ADMIN` (also manages users). Each role
  inherits what the roles below it can do.
- **Back office** (`/manage`): products, categories, subcategories and suppliers.
- **User administration** (`/admin/users`): change roles, disable or unlock accounts.
- **REST API** (`/api`): register, log in, refresh and log out with short-lived JWT access tokens
  and rotating refresh tokens.

## Security

- Passwords are stored with BCrypt. Public sign-up can only ever create a `CUSTOMER`.
- Five wrong passwords lock an account for 15 minutes.
- The website uses a session cookie (HttpOnly, SameSite=Lax) with CSRF protection; the API is
  stateless and uses bearer tokens.
- Refresh tokens are random, stored only as hashes, valid once, and reuse of an old token signs the
  account out everywhere.
- Strict Content-Security-Policy, frame denial and referrer policy; all assets are served locally.
- CI runs CodeQL, gitleaks and dependency review. See [SECURITY.md](SECURITY.md).

## Stack

Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Thymeleaf, Bootstrap 5, Bean Validation,
MapStruct, Flyway. H2 for development and tests, MySQL for production.

## Run it

```bash
./mvnw spring-boot:run                                   # in-memory H2, nothing to install
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # same, with sample products and accounts
./mvnw test
```

Open http://localhost:8081. With the `demo` profile the sample accounts
(`admin@demo.shop`, `manager@demo.shop`, `customer@demo.shop`) get random passwords that are
printed once to the console.

To create your own first administrator, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` before starting.

Everything in containers (app, MySQL and a mail inbox), with sample data:

```bash
export JWT_SECRET=$(openssl rand -base64 48)
docker compose up --build
```

Open http://localhost:8081 for the shop and http://localhost:8025 for the mail inbox where
confirmation and order emails arrive. The sample accounts have random passwords:
`docker compose logs app | grep "Demo account"`.

With MySQL:

```bash
DB_URL=jdbc:mysql://localhost:3306/shop DB_USERNAME=shop DB_PASSWORD=... \
JWT_SECRET=$(openssl rand -base64 48) \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

`JWT_SECRET` (at least 32 characters) is required with the `mysql` profile. Set
`SESSION_COOKIE_SECURE=true` when serving over HTTPS.

Email is off by default. To send real mail set MAIL_ENABLED=true, SMTP_HOST, SMTP_PORT,
SMTP_USERNAME, SMTP_PASSWORD, MAIL_FROM and APP_BASE_URL (the public address used in email links).
With email on, customers must confirm their address before checking out.

The schema is owned by Flyway migrations in `src/main/resources/db/migration`; Hibernate only
validates that the entities match it.

## Roadmap

1. Foundation: Java 21, Spring Boot 3.5, Flyway, domain model, catalog pages, tests (done)
2. Security: BCrypt passwords, form login, JWT for the API, roles, storefront (done)
3. Catalog REST API with OpenAPI docs, dark mode (done)
4. Cart (guest and signed-in), orders, stock movements and coupons (done)
5. Reviews, wishlist, dashboard and emails (done)
6. Docker Compose, screenshots, release polish