import {
  Link,
  NavLink,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import { orders, stats } from "./data/dashboard";
import { serviceUrls } from "./config/services";
import { OrderList, ProductList, CategoryList } from "./pages/Lists";

function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  if (user) return <Navigate to="/dashboard" replace />;
  function submit(event) {
    event.preventDefault();
    login(new FormData(event.currentTarget).get("email").trim());
    navigate("/dashboard", { replace: true });
  }
  return (
    <main className="login-layout">
      <section className="login-story">
        <Link className="brand" to="/">
          c<span>Commerce</span>
        </Link>
        <div>
          <p className="eyebrow">YOUR STORE, SIMPLIFIED</p>
          <h1>
            A little clarity.
            <br />A lot of possibility.
          </h1>
          <p>One home for your products, orders, and customers.</p>
        </div>
        <small>Commerce / Administration</small>
      </section>
      <section className="login-form-panel">
        <form className="login-form" onSubmit={submit}>
          <p className="eyebrow">ADMIN WORKSPACE</p>
          <h2>Welcome back.</h2>
          <p className="muted">Sign in to your store dashboard.</p>
          <label htmlFor="email">Email address</label>
          <input
            id="email"
            name="email"
            type="email"
            placeholder="admin@example.com"
            autoComplete="username"
            required
          />
          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            placeholder="Enter your password"
            autoComplete="current-password"
            required
            minLength={6}
          />
          <button className="primary" type="submit">
            Sign in <span>→</span>
          </button>
          <p className="demo-note">
            Demo mode: use any valid email and a password of at least 6
            characters. Backend authentication is ready for you to integrate.
          </p>
        </form>
      </section>
    </main>
  );
}

function AdminLayout() {
  const { user, logout } = useAuth();
  const { pathname } = useLocation();
  const pageTitle =
    {
      "/dashboard": "Overview",
      "/orders": "Orders",
      "/products": "Products",
      "/categories": "Categories",
    }[pathname] || "Workspace";
  if (!user) return <Navigate to="/login" replace />;
  return (
    <div className="admin-layout">
      <aside className="sidebar">
        <Link className="brand" to="/dashboard">
          c<span>Commerce</span>
        </Link>
        <p className="nav-label">WORKSPACE</p>
        <nav aria-label="Admin navigation">
          {[
            { to: "/dashboard", label: "Dashboard", icon: "▦" },
            { to: "/orders", label: "Orders", icon: "↗" },
            { to: "/products", label: "Products", icon: "□" },
            { to: "/categories", label: "Categories", icon: "⊞" },
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
        Commerce Admin <span>Built for your next big idea.</span>
      </footer>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<AdminLayout />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/orders" element={<OrderList />} />
        <Route path="/products" element={<ProductList />} />
        <Route path="/categories" element={<CategoryList />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
