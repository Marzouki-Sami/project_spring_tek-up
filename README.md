# Shop

An online store built with Spring Boot: a public storefront, customer accounts, a back office
for managers and a REST API with token authentication.

The project started as a university exercise and has been rebuilt in place, with its history kept,
into a tested, containerised application.

## Features

- **Storefront**: guests browse, search, filter and sort products without an account. Light and dark
  theme that follows the system setting.
- **Cart**: guests can fill a basket and keep it through sign-in; it is lost when the session ends.
  Checkout needs an account. A signed-in customer's basket is saved and follows them across devices
  (baskets idle for 60 days are cleaned up).
- **Address book and map** (`/account/addresses`): save up to ten delivery addresses. In the form and
  at checkout type an address, search, use your current location, or drop a pin on an OpenStreetMap
  map; every field stays editable. Geocoding uses Nominatim through the server (cached and throttled).
  Configure with `GEO_ENABLED`, `GEO_URL`, `GEO_USER_AGENT` and `GEO_TILE_URL`; for production use
  your own provider or instance in line with the Nominatim usage policy.
- **Orders**: delivery address, optional percentage coupon, live stock check, order history, customer
  cancellation while an order is still placed, and a manager workflow (placed, processing, shipped,
  delivered, cancelled). Stock is taken atomically so two buyers can never oversell, every change is
  logged in `stock_movements`, and cancelling returns the items.
- **Reviews and wishlist**: verified buyers can rate and review a product (one review each, editable);
  signed-in users keep a wishlist.
- **Coupons** (`/manage/coupons`): percentage discount with an optional expiry date and usage limit.
- **Dashboard** (`/manage`): revenue, orders by status, best sellers and low-stock alerts.
- **Back office** (`/manage`): products, categories, subcategories, suppliers, orders and coupons.
- **Accounts and roles**: sign-up with a strong-password policy, email confirmation, password reset by
  email, profile and password change. Roles are `CUSTOMER`, `MANAGER` (runs the shop) and `ADMIN`
  (also manages users at `/admin/users`); each role inherits what the roles below it can do.
- **Email**: confirmation, password reset, order confirmation, shipping notice and a low-stock alert
  to managers. Links are single-use and stored only as hashes. By default emails are saved as text
  files in `mail-outbox/` instead of being sent.
- **REST API** (`/api`, interactive docs at `/api/docs`): public catalog search, manager-only product
  changes, and register, log in, refresh and log out with short-lived JWT access tokens and rotating
  refresh tokens.

## Security

- Passwords are stored with BCrypt. Public sign-up can only ever create a `CUSTOMER`.
- Five wrong passwords lock an account for 15 minutes.
- The website uses a session cookie (HttpOnly, SameSite=Lax, Secure by default in production) with CSRF
  protection; the API is stateless and uses bearer tokens.
- Website sessions follow the account: disabling it or changing its password ends them, and a role
  change applies immediately.
- Refresh tokens are random, stored only as hashes, valid once (claimed atomically), and reuse of an old
  token signs the account out everywhere.
- Order status changes and coupon or stock updates are conditional database updates, so concurrent
  requests cannot restock or redeem twice.
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
JWT_SECRET=$(openssl rand -base64 48) MAIL_ENABLED=false \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

With the `mysql` profile `JWT_SECRET` (at least 32 characters) and `MAIL_ENABLED` must be set, and the
session cookie is `Secure` by default. Set `SESSION_COOKIE_SECURE=false` only for plain-HTTP local runs.

Email is off by default. To send real mail set `MAIL_ENABLED=true`, `SMTP_HOST`, `SMTP_PORT`,
`SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` and `APP_BASE_URL` (the public address used in email links).
With email on, customers must confirm their address before checking out.

The schema is owned by Flyway migrations in `src/main/resources/db/migration`; Hibernate only
validates that the entities match it.

## Project layout

```
src/main/java/com/marzouki/shop
  catalog/    products, categories, suppliers and their back-office pages
  cart/       session basket and pricing
  order/      orders, coupons, stock movements, checkout
  engagement/ reviews and wishlist
  dashboard/  manager overview
  user/       accounts, roles, tokens, recovery
  security/   web and API security, JWT, session handling
  api/        REST controllers and OpenAPI
  mail/       email composition and delivery
  setup/      first-admin bootstrap and demo data
```

## Tests

`./mvnw test` runs unit, web and integration tests (security matrix, checkout flow, concurrency
guards, email flows). CI also runs the suite against MySQL 8.4, CodeQL, gitleaks, dependency review
and a container image build.

## Roadmap

Delivered: foundation, security and roles, catalog API with OpenAPI, dark mode, cart and orders,
coupons, reviews, wishlist, dashboard, emails, Docker Compose.

Ideas for later: online payments, multiple languages, product images, an orders API.

## License

[MIT](LICENSE)