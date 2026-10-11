# everyday admin (admin-web)

React + Vite admin application at http://localhost:5173. Use a supported
Node.js LTS release and npm.

## Run locally

From this directory:

```powershell
npm.cmd install
# Copy if you do not already have .env.local.
Copy-Item .env.example .env.local
npm.cmd run dev
```

The admin login uses email/password through user-service and requires an enabled
administrator account with the `ROLE_ADMIN` role. Google sign-in is not offered
in the admin frontend. Backend authorization remains mandatory.

Service URLs are configurable with `VITE_USER_SERVICE_URL`,
`VITE_PRODUCT_SERVICE_URL`, and `VITE_ORDER_SERVICE_URL`. The defaults are
user-service on 8081, product-service on 8082, and order-service on 8083.
These frontend values are public and must not contain secrets.

## Screens

- `/login` — email/password admin login.
- `/register` — request administrator access.
- `/dashboard` — sample dashboard and configured service URLs.
- `/orders`, `/products`, `/categories` — sample catalog and order lists.
- `/users`, `/profile` — user-service account management and profile.

Dashboard and list data may still be mocked; service cards show configured
URLs, not live health checks.

## Verification and build

```powershell
npm.cmd test
npm.cmd run build
```

The build goes to `dist/`. `npm.cmd run preview` serves port 4173. Configure
production service URLs and serve `index.html` as the fallback for frontend
routes.
