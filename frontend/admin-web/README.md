# everyday admin (`admin-web`)

Standalone React + Vite admin application. Use Node.js 22 LTS or newer supported LTS and npm.

## Run

```sh
npm install
cp .env.example .env
npm run dev
```

On Windows PowerShell, use `Copy-Item .env.example .env` instead of `cp` if needed. Open http://localhost:5173. In PowerShell environments that block npm scripts, use `npm.cmd`.

`npm run build` generates `dist/`; `npm run preview` serves the production build on port 4173.

## Included

- `/login`: demo login with native email validation and minimum 6-character password.
- `/dashboard`: route guarded by a demo session, statistics, recent orders, service configuration, and sign out.
- `/orders`: separate order list with customer/order search and status filters.
- `/products`: separate product list with SKU/name search, category filters, prices, and stock.
- `/categories`: separate category list with search, descriptions, and product counts.
- All admin list routes require the same demo session as the dashboard. Data is read-only and mocked; replace `src/data/catalog.js` and `src/data/dashboard.js` when integrating your services.
- Responsive layout. Login persists for the current browser tab using sessionStorage; passwords are never stored.

## Connect your microservices

Set the three `VITE_*_SERVICE_URL` values in `.env`, then restart Vite. Defaults are user-service on 8081, order-service on 8082, and product-service on 8083. These ports are placeholders, not an assumed backend contract.

- `src/config/services.js`: service base URLs.
- `src/services/api.js`: optional JSON request helper, e.g. `apiRequest('product', '/your-products-endpoint')`.
- `src/context/AuthContext.jsx`: replace demo login with user-service login, session restoration, role checking, and logout. The current form deliberately does not send credentials anywhere.
- `src/data/dashboard.js`: replace static dashboard figures and recent orders with responses from your services.

Demo authentication is only a UI placeholder. Enforce authentication and admin authorization on the backend; the client route guard is not access control. Choose the cookie/token approach appropriate to your services and configure CORS for this app's origin. No endpoint paths, token format, or role model have been assumed.

All dashboard values are sample data. The service cards show configured URLs, not live health checks. Fonts are optional Google Fonts with local sans-serif fallbacks.

For production hosting, serve `dist/` and configure your server to return `index.html` for app routes such as `/dashboard`. `VITE_*` variables are public and are embedded at build time; never put secrets in them.
