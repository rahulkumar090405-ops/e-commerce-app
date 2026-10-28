// Public URLs only. Never place service secrets in VITE_* variables.
export const serviceUrls = {
  user: import.meta.env?.VITE_USER_SERVICE_URL || "http://localhost:8084",
  order: import.meta.env?.VITE_ORDER_SERVICE_URL || "http://localhost:8084",
  product: import.meta.env?.VITE_PRODUCT_SERVICE_URL || "http://localhost:8084",
};
