# customer-web

Standalone React + Vite customer storefront. Use Node.js 22 LTS or newer supported LTS and npm.

## Run

```sh
npm install
cp .env.example .env
npm run dev
```

On Windows PowerShell, use `Copy-Item .env.example .env` instead of `cp` if needed. Open http://localhost:5174. In PowerShell environments that block npm scripts, use `npm.cmd`.

`npm run build` generates `dist/`; `npm run preview` serves the production build on port 4174.

## Included

- `/`: six mock products, search, category filtering, and add-to-bag controls.
- `/products`: a separate full product-list page, accessible from the header and the homepage collection button. It shares the catalog, search, category filters, and cart with the homepage via `src/components/ProductCatalog.jsx`.
- `/cart`: quantity changes, remove items, subtotal, empty state, and persistence in localStorage.
- `/login`: demo login; use any valid email and any password of at least 6 characters. Sign out is available in the header.
- Responsive desktop/mobile layouts, labeled controls, keyboard focus styles, and add-to-cart announcements.

The demo cart is local to the browser and shared between demo logins in that browser. Each product allows up to 99 units. The demo catalog uses USD. Checkout is intentionally disabled until order-service integration; no payment or order is created. Emoji illustrations work without external product-image hosting. Fonts use optional Google Fonts with local sans-serif fallbacks.

## Connect your microservices

Edit `.env` and restart Vite. Defaults: user-service on 8081, order-service on 8082, product-service on 8083. These are configurable placeholders.

- `src/config/services.js`: service base URLs.
- `src/services/api.js`: optional JSON request helper. For example, call `apiRequest('product', '/your-products-endpoint')` after selecting your API routes.
- `src/data/products.js` and `src/components/ProductCatalog.jsx`: replace mock products with a product-service request and loading/error handling.
- `src/context/CartContext.jsx`: replace the imported mock catalog with your fetched catalog or service-backed cart. Keep server validation of price and availability when placing orders.
- `src/context/AuthContext.jsx` and `src/pages/Login.jsx`: wire user-service authentication and session restoration. The current form does not send credentials; passwords are never stored.
- `src/pages/Cart.jsx`: enable checkout and call order-service once your request contract and authentication are implemented. Shipping and taxes are intentionally uncalculated.

Choose your cookie/token approach and configure service CORS for this app's origin. Backend integration, real authentication, registration, order placement, and payments are left to you. No backend endpoint paths or auth contract have been assumed.

Serve `dist/` in production and configure an SPA fallback to `index.html` for `/cart`, `/login`, and other routes. `VITE_*` variables are public and embedded at build time; never include secrets.
