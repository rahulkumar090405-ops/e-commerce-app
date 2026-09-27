import { Link, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import Shop from "./pages/Shop";
import Products from "./pages/Products";
import Cart from "./pages/Cart";
import Login from "./pages/Login";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Shop />} />
        <Route path="products" element={<Products />} />
        <Route path="cart" element={<Cart />} />
        <Route path="login" element={<Login />} />
        <Route
          path="*"
          element={
            <main className="empty-state">
              <h1>Page not found</h1>
              <p>Let's get you back to the good things.</p>
              <Link className="primary inline" to="/">
                Back to the shop
              </Link>
            </main>
          }
        />
      </Route>
    </Routes>
  );
}
