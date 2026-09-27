import { Link } from "react-router-dom";
import { MAX_QUANTITY, useCart } from "../context/CartContext";
import { formatMoney } from "../data/products";

export default function Cart() {
  const { items, count, subtotal, removeItem, setQuantity } = useCart();
  return (
    <main className="cart-page">
      <p className="eyebrow">YOUR EVERYDAY FINDS</p>
      <h1>
        Your bag <span className="heading-count">({count})</span>
      </h1>
      {items.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon" aria-hidden="true">
            ♧
          </span>
          <h2>A little room for something good.</h2>
          <p>Your bag is empty. Find your next everyday favorite.</p>
          <Link to="/" className="primary inline">
            Explore products →
          </Link>
        </div>
      ) : (
        <div className="cart-layout">
          <section aria-label="Items in your bag">
            <div className="cart-items">
              {items.map((item) => (
                <article className="cart-item" key={item.id}>
                  <div
                    className="cart-art"
                    style={{ background: item.color }}
                    aria-hidden="true"
                  >
                    {item.icon}
                  </div>
                  <div className="cart-item-details">
                    <small>{item.category}</small>
                    <h2>{item.name}</h2>
                    <p>{formatMoney(item.price)} each</p>
                    <button
                      className="remove-button"
                      onClick={() => removeItem(item.id)}
                      aria-label={`Remove ${item.name}`}
                    >
                      Remove
                    </button>
                  </div>
                  <div className="cart-item-controls">
                    <strong>{formatMoney(item.price * item.quantity)}</strong>
                    <div className="quantity">
                      <button
                        onClick={() => setQuantity(item.id, item.quantity - 1)}
                        aria-label={`Decrease ${item.name} quantity`}
                      >
                        −
                      </button>
                      <span aria-label={`Quantity: ${item.quantity}`}>
                        {item.quantity}
                      </span>
                      <button
                        disabled={item.quantity >= MAX_QUANTITY}
                        onClick={() => setQuantity(item.id, item.quantity + 1)}
                        aria-label={`Increase ${item.name} quantity`}
                      >
                        +
                      </button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
            <Link className="continue-link" to="/">
              ← Continue shopping
            </Link>
          </section>
          <aside className="order-summary">
            <h2>Bag summary</h2>
            <div>
              <span>Subtotal ({count} items)</span>
              <span>{formatMoney(subtotal)}</span>
            </div>
            <div>
              <span>Shipping & tax</span>
              <span>Not calculated</span>
            </div>
            <div className="summary-total">
              <strong>Subtotal</strong>
              <strong>{formatMoney(subtotal)}</strong>
            </div>
            <button className="primary" disabled>
              Checkout coming soon
            </button>
            <p>
              Checkout will be available after order-service integration. No
              order or payment is submitted.
            </p>
          </aside>
        </div>
      )}
    </main>
  );
}
