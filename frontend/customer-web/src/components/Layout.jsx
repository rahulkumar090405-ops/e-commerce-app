import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";

export default function Layout() {
  const { user, logout } = useAuth();
  const { count } = useCart();
  return (
    <>
      <div className="announcement">
        Thoughtful essentials. Everyday possibilities.
      </div>
      <header className="site-header">
        <Link className="brand" to="/">
          everyday<span>®</span>
        </Link>
        <nav aria-label="Main navigation">
          <NavLink to="/" end>
            Home
          </NavLink>
          <NavLink to="/products">Products</NavLink>
          <NavLink to="/cart">Your cart</NavLink>
        </nav>
        <div className="header-actions">
          {user ? (
            <><NavLink to="/profile">Profile</NavLink><button className="text-button" onClick={logout} title={`Signed in as ${user.email}`}>Sign out</button></>
          ) : (
            <NavLink to="/login">Sign in</NavLink>
          )}
          <Link
            className="cart-link"
            to="/cart"
            aria-label={`Shopping bag, ${count} items`}
          >
            Bag <span>{count}</span>
          </Link>
        </div>
      </header>
      <Outlet />
      <footer className="site-footer">
        <Link className="brand" to="/">
          everyday<span>®</span>
        </Link>
        <p>Good things, made for everyday life.</p>
        <small>Demo storefront · Microservice integration pending</small>
      </footer>
    </>
  );
}
