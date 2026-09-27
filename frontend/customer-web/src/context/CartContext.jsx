import { createContext, useContext, useEffect, useState } from "react";
import { products } from "../data/products";

const CartContext = createContext(null);
const KEY = "commerce-customer-cart";
export const MAX_QUANTITY = 99;

function readCart() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY));
    if (!Array.isArray(saved)) return [];
    // Keep only known products and valid quantities; don't trust stored prices.
    return products.flatMap((product) => {
      const item = saved.find((entry) => entry?.id === product.id);
      return Number.isInteger(item?.quantity) && item.quantity > 0
        ? [{ id: product.id, quantity: Math.min(item.quantity, MAX_QUANTITY) }]
        : [];
    });
  } catch {
    return [];
  }
}

export function CartProvider({ children }) {
  const [cart, setCart] = useState(readCart);
  useEffect(() => {
    try {
      localStorage.setItem(KEY, JSON.stringify(cart));
    } catch {
      /* Cart still works in memory. */
    }
  }, [cart]);
  function addItem(id) {
    if (!products.some((product) => product.id === id)) return;
    setCart((current) =>
      current.some((item) => item.id === id)
        ? current.map((item) =>
            item.id === id
              ? { ...item, quantity: Math.min(item.quantity + 1, MAX_QUANTITY) }
              : item,
          )
        : [...current, { id, quantity: 1 }],
    );
  }
  function removeItem(id) {
    setCart((current) => current.filter((item) => item.id !== id));
  }
  function setQuantity(id, quantity) {
    if (!Number.isInteger(quantity)) return;
    if (quantity <= 0) return removeItem(id);
    setCart((current) =>
      current.map((item) =>
        item.id === id
          ? { ...item, quantity: Math.min(quantity, MAX_QUANTITY) }
          : item,
      ),
    );
  }
  const items = cart.map((item) => ({
    ...products.find((product) => product.id === item.id),
    quantity: item.quantity,
  }));
  const count = items.reduce((total, item) => total + item.quantity, 0);
  const subtotal =
    items.reduce(
      (total, item) => total + Math.round(item.price * 100) * item.quantity,
      0,
    ) / 100;
  return (
    <CartContext.Provider
      value={{ items, count, subtotal, addItem, removeItem, setQuantity }}
    >
      {children}
    </CartContext.Provider>
  );
}

export const useCart = () => useContext(CartContext);
