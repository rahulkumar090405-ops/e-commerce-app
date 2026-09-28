import { useEffect, useMemo, useState } from "react";
import { getUsers } from "../services/userApi";

function displayName(user) {
  return user.name || [user.firstName, user.lastName].filter(Boolean).join(" ") || "—";
}

export default function Users() {
  const [users, setUsers] = useState([]);
  const [query, setQuery] = useState("");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("Loading users…");

  useEffect(() => {
    getUsers()
      .then((response) => {
        const list = Array.isArray(response) ? response : response?.data || [];
        setUsers(list);
        setStatus("");
      })
      .catch((error) => setStatus(error.message || "Unable to load users."));
  }, []);

  const roles = useMemo(
    () => [...new Set(users.flatMap((user) => user.roles || []))].sort(),
    [users],
  );
  const visibleUsers = users.filter((user) => {
    const searchable = `${displayName(user)} ${user.email} ${(user.roles || []).join(" ")}`.toLowerCase();
    return (!role || user.roles?.includes(role)) && searchable.includes(query.trim().toLowerCase());
  });

  return (
    <main className="dashboard">
      <div className="page-heading">
        <div><p className="eyebrow">ACCESS MANAGEMENT</p><h1>Users</h1><p className="muted">Registered accounts and the roles assigned by user-service.</p></div>
        <span className="demo-pill">{users.length} registered</span>
      </div>
      <section className="panel" aria-label="Registered users">
        <div className="list-toolbar">
          <label className="list-field"><span>Search users</span><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Name or email" /></label>
          <label className="list-field"><span>Role</span><select value={role} onChange={(event) => setRole(event.target.value)}><option value="">All roles</option>{roles.map((item) => <option key={item} value={item}>{item.replace("ROLE_", "")}</option>)}</select></label>
          <button className="reset-button" onClick={() => { setQuery(""); setRole(""); }}>Reset filters</button>
        </div>
        {status ? <p className="list-count" role="status">{status}</p> : <>
          <p className="list-count" role="status">Showing {visibleUsers.length} of {users.length} users</p>
          <div className="table-scroll"><table><caption className="sr-only">Registered users and their roles</caption><thead><tr><th>Name</th><th>Email</th><th>Mobile</th><th>Roles</th><th>Joined</th></tr></thead><tbody>
            {visibleUsers.map((user) => <tr key={user.id}><td><strong>{displayName(user)}</strong></td><td>{user.email}</td><td>{user.mobile || "—"}</td><td><div className="role-list">{(user.roles || []).map((item) => <span key={item} className="role-badge">{item.replace("ROLE_", "")}</span>)}</div></td><td>{user.createdDate ? new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(user.createdDate)) : "—"}</td></tr>)}
            {visibleUsers.length === 0 && <tr><td colSpan="5" className="list-empty">No users match these filters.</td></tr>}
          </tbody></table></div>
        </>}
      </section>
    </main>
  );
}
