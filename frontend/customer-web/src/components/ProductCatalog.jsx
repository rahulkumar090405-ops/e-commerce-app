import { useState } from "react";
import { Link } from "react-router-dom";
import { products, formatMoney } from "../data/products";
import { MAX_QUANTITY, useCart } from "../context/CartContext";

const categories = [
  "All products",
  ...new Set(products.map((product) => product.category)),
];

export default function ProductCatalog() {
  const [category, setCategory] = useState("All products");
  const [query, setQuery] = useState("");
  const [notice, setNotice] = useState("");
  const { addItem, items } = useCart();
  const visible = products.filter(
    (product) =>
      (category === "All products" || product.category === category) &&
      `${product.name} ${product.category}`
        .toLowerCase()
        .includes(query.trim().toLowerCase()),
  );
  function add(product) {
    addItem(product.id);
    setNotice(`${product.name} added to your bag.`);
  }
  return (
    <section id="collection" className="collection">
      <div className="collection-heading">
        <div>
          <p className="eyebrow">CURATED FOR YOU</p>
          <h2>Find your everyday.</h2>
        </div>
        <p>{products.length} essentials. Endless possibilities.</p>
      </div>
      <div className="filters">
        <div className="categories" aria-label="Product categories">
          {categories.map((value) => (
            <button
              key={value}
              className={category === value ? "selected" : ""}
              aria-pressed={category === value}
              onClick={() => setCategory(value)}
            >
              {value}
            </button>
          ))}
        </div>
        <label className="search">
          <span className="sr-only">Search products</span>
          <span aria-hidden="true">⌕</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search products"
          />
        </label>
      </div>
      <div className="catalog-status" role="status">
        {notice ? (
          <>
            {notice} <Link to="/cart">View bag →</Link>
          </>
        ) : (
          `${visible.length} products to explore`
        )}
      </div>
      <div className="product-grid">
        {visible.map((product) => {
          const atLimit = items.some(
            (item) => item.id === product.id && item.quantity >= MAX_QUANTITY,
          );
          return (
            <article className="product-card" key={product.id}>
              <div
                className="product-art"
                style={{ background: product.color }}
              >
                {product.badge && (
                  <span className="badge">{product.badge}</span>
                )}
                <span className="product-icon" aria-hidden="true">
                  {product.icon}
                </span>
                <span className="product-art-label">everyday essentials</span>
              </div>
              <div className="product-meta">
                <span>{product.category}</span>
                <span>{formatMoney(product.price)}</span>
              </div>
              <h3>{product.name}</h3>
              <p>{product.description}</p>
              <button
                className="add-button"
                onClick={() => add(product)}
                disabled={atLimit}
              >
                {atLimit ? "Bag limit reached" : "Add to bag"}
                <span aria-hidden="true">+</span>
              </button>
            </article>
          );
        })}
      </div>
      {visible.length === 0 && (
        <div className="empty-state">
          <h2>No products found.</h2>
          <p>Try another search or category.</p>
          <button
            className="primary"
            onClick={() => {
              setQuery("");
              setCategory("All products");
            }}
          >
            Reset filters
          </button>
        </div>
      )}
    </section>
  );
}
