import {
  Link,
  NavLink,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import { orders, stats } from "./data/dashboard";
import { serviceUrls } from "./config/services";
import { OrderList, ProductList, CategoryList } from "./pages/Lists";
import Users from "./pages/Users";
import Profile from "./pages/Profile";
import Login from "./pages/Login";
import Register from "./pages/Register";

function AdminLayout() {
  const { user, isLoading, logout } = useAuth();
  const { pathname } = useLocation();
  const pageTitle =
    {
      "/dashboard": "Overview",
      "/orders": "Orders",
      "/products": "Products",
      "/categories": "Categories",
      "/users": "Users",
      "/profile": "My profile",
    }[pathname] || "Workspace";
  if (isLoading) return <main className="auth-loading">Checking your administrator session…</main>;
  if (!user) return <Navigate to="/login" replace />;
  return (
    <div className="admin-layout">
      <aside className="sidebar">
        <Link className="brand" to="/dashboard">
          everyday<span>adm.</span>
        </Link>
        <p className="nav-label">WORKSPACE</p>
        <nav aria-label="Admin navigation">
          {[
            { to: "/dashboard", label: "Dashboard", icon: "▦" },
            { to: "/orders", label: "Orders", icon: "↗" },
            { to: "/products", label: "Products", icon: "□" },
            { to: "/categories", label: "Categories", icon: "⊞" },
            { to: "/users", label: "Users", icon: "◉" },
            { to: "/profile", label: "My profile", icon: "○" },
          ].map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                "nav-item" + (isActive ? " active" : "")
              }
            >
              {item.icon} <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <span className="avatar">{user.email.charAt(0).toUpperCase()}</span>
          <div className="account">
            <strong>Administrator</strong>
            <small title={user.email}>{user.email}</small>
          </div>
          <button
            className="logout"
            onClick={logout}
            aria-label="Sign out"
            title="Sign out"
          >
            ↪
          </button>
        </div>
      </aside>
      <div className="admin-main">
        <header className="topbar">
          <span>
            Workspace <span className="muted">/ {pageTitle}</span>
          </span>
          <span className="demo-pill">Demo workspace</span>
        </header>
        <Outlet />
      </div>
    </div>
  );
}

function Dashboard() {
  return (
    <main className="dashboard">
      <div className="page-heading">
        <div>
          <p className="eyebrow">STORE OVERVIEW</p>
          <h1>Dashboard</h1>
          <p className="muted">A snapshot of how your store is doing.</p>
        </div>
        <span className="date-label">Sample reporting period · Sep 2026</span>
      </div>
      <section className="stats" aria-label="Store statistics">
        {stats.map((stat) => (
          <article className="stat-card" key={stat.label}>
            <div className="stat-heading">
              <span>{stat.label}</span>
              <span className="stat-icon">{stat.icon}</span>
            </div>
            <strong>{stat.value}</strong>
            <small>{stat.note}</small>
          </article>
        ))}
      </section>
      <section className="welcome-banner">
        <div>
          <p className="eyebrow">EVERYTHING IN ONE PLACE</p>
          <h2>Your next chapter starts here.</h2>
          <p>
            Your storefront is taking shape. Connect your services to bring it
            to life.
          </p>
        </div>
        <a href="#services" className="light-button">
          View service setup ↗
        </a>
      </section>
      <section className="panel" id="recent-orders">
        <div className="panel-heading">
          <div>
            <h2>Recent orders</h2>
            <p className="muted">The latest activity from your demo store.</p>
          </div>
          <Link className="count" to="/orders">
            View all orders →
          </Link>
        </div>
        <div className="table-scroll">
          <table>
            <caption className="sr-only">Sample recent orders</caption>
            <thead>
              <tr>
                <th scope="col">Order</th>
                <th scope="col">Customer</th>
                <th scope="col">Date</th>
                <th scope="col">Amount</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td>
                    <strong>#{order.id}</strong>
                  </td>
                  <td>{order.customer}</td>
                  <td>{order.date}</td>
                  <td>${order.total.toFixed(2)}</td>
                  <td>
                    <span className={`status ${order.status.toLowerCase()}`}>
                      {order.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section id="services" className="service-section">
        <div className="panel-heading">
          <div>
            <h2>Microservices</h2>
            <p className="muted">
              Configured endpoints. All dashboard data is currently mocked.
            </p>
          </div>
        </div>
        <div className="service-grid">
          {Object.entries(serviceUrls).map(([name, url]) => (
            <article className="service-card" key={name}>
              <span className="service-marker">○</span>
              <h3>{name}-service</h3>
              <code>{url}</code>
              <small>Integration pending</small>
            </article>
          ))}
        </div>
      </section>
      <footer>
        everyday admin <span>Built for your next big idea.</span>
      </footer>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<AdminLayout />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/orders" element={<OrderList />} />
        <Route path="/products" element={<ProductList />} />
        <Route path="/categories" element={<CategoryList />} />
        <Route path="/users" element={<Users />} />
        <Route path="/profile" element={<Profile />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
