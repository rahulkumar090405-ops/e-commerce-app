import ProductCatalog from "../components/ProductCatalog";

export default function Products() {
  return (
    <main>
      <header className="products-heading">
        <p className="eyebrow">THE FULL COLLECTION</p>
        <h1>Product list</h1>
        <p>
          Browse our everyday essentials, find your favorites, and add them to
          your bag.
        </p>
      </header>
      <ProductCatalog />
    </main>
  );
}
