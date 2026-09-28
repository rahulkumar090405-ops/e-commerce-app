// Public URLs only. Never place service secrets in VITE_* variables.
export const serviceUrls = {
  user: import.meta.env.DEV
    ? "/user-service"
    : import.meta.env.VITE_USER_SERVICE_URL || "http://localhost:8081",
  order: import.meta.env.VITE_ORDER_SERVICE_URL || "http://localhost:8082",
  product: import.meta.env.VITE_PRODUCT_SERVICE_URL || "http://localhost:8083",
};
