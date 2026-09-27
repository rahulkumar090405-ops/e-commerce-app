// Replace this catalog with a product-service response when you integrate the backend.
export const products = [
  {
    id: "p1",
    name: "Everyday backpack",
    category: "Accessories",
    price: 64,
    icon: "🎒",
    color: "#e8e5d9",
    description: "Room for everything that matters.",
    badge: "Bestseller",
  },
  {
    id: "p2",
    name: "Studio headphones",
    category: "Electronics",
    price: 129,
    icon: "🎧",
    color: "#e3e7e4",
    description: "A little more focus. A lot more sound.",
    badge: "Popular",
  },
  {
    id: "p3",
    name: "Stoneware mug",
    category: "Home & Living",
    price: 24,
    icon: "☕",
    color: "#eddfd0",
    description: "Slow mornings start here.",
    badge: "",
  },
  {
    id: "p4",
    name: "Classic timepiece",
    category: "Accessories",
    price: 89,
    icon: "⌚",
    color: "#e6e4e0",
    description: "Good design, around the clock.",
    badge: "New",
  },
  {
    id: "p5",
    name: "Portable speaker",
    category: "Electronics",
    price: 48,
    icon: "🔊",
    color: "#e5e7dc",
    description: "Your favorite sounds, anywhere.",
    badge: "",
  },
  {
    id: "p6",
    name: "Desk companion",
    category: "Home & Living",
    price: 32,
    icon: "🪴",
    color: "#e0e8de",
    description: "A fresh perspective for your space.",
    badge: "New",
  },
];

export const formatMoney = (value) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(
    value,
  );
