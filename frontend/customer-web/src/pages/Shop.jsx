import { Link } from "react-router-dom";
import ProductCatalog from "../components/ProductCatalog";

export default function Shop() {
  return (
    <main>
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">THE EVERYDAY COLLECTION</p>
          <h1>
            Less ordinary.
            <br />
            More everyday.
          </h1>
          <p>
            Considered essentials for the way you live.
            <br />
            Discover your next everyday favorite.
          </p>
          <Link className="primary inline" to="/products">
            Explore the collection <span>↗</span>
          </Link>
          <div className="hero-note">
            <span>01 /</span> Good design. Simple living.
          </div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <span className="art-ring"></span>
          <span className="art-headphones">🎧</span>
          <span className="art-mug">☕</span>
          <span className="art-plant">🪴</span>
          <span className="art-caption">THE ART OF EVERYDAY</span>
        </div>
      </section>
      <ProductCatalog />
      <section className="closing-note">
        <span aria-hidden="true">✳</span>
        <h2>Little things. Better days.</h2>
        <p>Make room for the things you love.</p>
      </section>
    </main>
  );
}
