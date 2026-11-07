# Shop

An online store built with Spring Boot: a public storefront, customer accounts, a back office
for managers and a REST API with token authentication.

The project started as a university exercise and has been rebuilt in place, with its history kept,
into a tested, containerised application.

## Features

- **Storefront**: guests browse, search, filter and sort products without an account: category and
  type, price range, in stock only, minimum rating, and sorting by name, price, newest or best rated.
  Light and dark theme that follows the system setting.
- **Lists that scale**: every back-office list (products, orders, customers, users, coupons, suppliers,
  categories, subcategories, audit log) and the customer's own orders and wishlist are searched,
  filtered, sorted and paged in the database, with page sizes capped at 100 and search text escaped.
  Pages are checked by tests that count SQL statements so a list never costs one query per row.
- **Products, options and variants**: a *product type* (for example Clothing) lists which *options* its
  products come in (Size, Color). Options are shared and their values are managed once, so a manager
  never re-enters them per product. Tick the values a product is sold in and every missing
  combination becomes a variant with its own SKU, price override, stock and active flag. A unique key
  on the combination makes duplicates impossible, even with two managers working at once.
  The variants page (`/manage/products/{id}/variants`) pages, searches and filters on the server,
  scrolls with a sticky header, and offers bulk actions on ticked rows or on everything a search
  matches: set or clear a price, add or remove stock (logged), activate, deactivate, delete (ordered
  variants are protected). Missing option values can be added in place. SKUs are suggested from the
  product name and options and can be edited.
- **Product images**: several pictures per product, shown as a carousel with a primary picture.
- **Notifications**: success, info, warning and error messages appear as toasts at the bottom right.- **Cart**: guests can fill a basket and keep it through sign-in; it is lost when the session ends.
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
- **Customers** (/manage/customers): searchable, sortable, paged list of customers with their order
  count and spend (cancelled orders excluded) and a detail page with their order history.
- **PDF invoices**: every order gets one invoice, numbered INV-2026-000001 onwards with no repeats and no
  gaps (a unique key per year, and a lost race retries with the next number). Prices include VAT, so the
  invoice shows the included share at the configured rate. The seller details and VAT rate are copied
  in when the invoice is issued, so changing the settings never alters an invoice that was already sent.
  Rendered from an HTML template by OpenHTMLtoPDF (everything typed by customers is escaped), attached to
  the order confirmation email and downloadable by the customer, managers and the API.
  Emails are sent from a small separate thread pool after the request finishes (MAIL_ASYNC), so a slow
  mail server or PDF render never holds a customer's request or a database connection.
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
  files in `mail-outbox/` instead of being sent. Admins edit the wording of every email at
  `/admin/emails` (plain text with `{{placeholders}}`, live preview, a test email to yourself and a
  one-click return to the original); nothing is ever executed or rendered as markup, and unknown
  placeholders are refused. Name, accent colour, footer and support address come from
  `/admin/settings`. A per-address cooldown (`MAIL_COOLDOWN`, default 60s) keeps the sign-up and reset
  forms from being used to flood someone. Staff can switch low-stock emails off in their profile, and
  each product can set its own low-stock level.- **REST API** (`/api`, interactive docs at `/api/docs`): public catalog search, manager-only product
  changes, and register, log in, refresh and log out with short-lived JWT access tokens and rotating
  refresh tokens.
- **Orders API**: POST /api/orders places an order from a list of variants, a coupon and either a saved
  address or a typed one. It needs an Idempotency-Key header: a retry with the same key and body returns
  the same order (Idempotent-Replayed: true) instead of ordering twice, even when retries arrive at the
  same moment, and a different body under the same key is refused. Customers list, read and cancel their
  own orders; managers list, filter and move any order along its workflow under /api/manage/orders.
  Errors are RFC 9457 problem documents.

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
- **Two-factor authentication** with any authenticator app (TOTP, RFC 6238). Managers and admins must use
  it; customers may. Secrets are encrypted at rest, a code works only once, wrong codes are rate limited,
  and recovery codes are single-use and stored as keyed hashes. "Trust this device" skips the code for
  30 days (random cookie, stored only as a hash; forgotten when the password changes). The API asks staff
  for the code too (otp field). An admin can reset someone's two-factor from the user list.
- **Audit log** (/admin/audit): every change by a manager or admin, in the back office and through the
  API, is recorded with who, what, result and IP. The table is append-only; the application has no
  way to edit or delete entries. Security changes such as turning two-factor on are recorded too.
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