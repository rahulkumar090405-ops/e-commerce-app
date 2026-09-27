import { useState } from "react";
import { orders } from "../data/dashboard";
import { categories, products } from "../data/catalog";

const money = (value) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(
    value,
  );

function ListPage({
  title,
  description,
  rows,
  columns,
  searchKeys,
  filterKey,
  filterLabel,
  filterOptions = [],
}) {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("");
  const visible = rows.filter(
    (row) =>
      (!filter || row[filterKey] === filter) &&
      searchKeys.some((key) =>
        String(row[key]).toLowerCase().includes(query.trim().toLowerCase()),
      ),
  );
  function reset() {
    setQuery("");
    setFilter("");
  }

  return (
    <main className="dashboard">
      <div className="page-heading">
        <div>
          <p className="eyebrow">STORE MANAGEMENT</p>
          <h1>{title}</h1>
          <p className="muted">{description}</p>
        </div>
        <span className="demo-pill">Sample data</span>
      </div>
      <section className="panel" aria-label={`${title} list`}>
        <div className="list-toolbar">
          <label className="list-field">
            <span>Search {title.toLowerCase()}</span>
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={`Search ${title.toLowerCase()}…`}
            />
          </label>
          {filterKey && (
            <label className="list-field">
              <span>{filterLabel}</span>
              <select
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
              >
                <option value="">All {filterLabel.toLowerCase()}</option>
                {filterOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </label>
          )}
          <button className="reset-button" onClick={reset}>
            Reset filters
          </button>
        </div>
        <p className="list-count" role="status">
          Showing {visible.length} of {rows.length} {title.toLowerCase()}
        </p>
        <div className="table-scroll">
          <table>
            <caption className="sr-only">{title} — demo records</caption>
            <thead>
              <tr>
                {columns.map((column) => (
                  <th key={column.key} scope="col">
                    {column.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visible.map((row) => (
                <tr key={row.id}>
                  {columns.map((column) => (
                    <td key={column.key}>
                      {column.render ? column.render(row) : row[column.key]}
                    </td>
                  ))}
                </tr>
              ))}
              {visible.length === 0 && (
                <tr>
                  <td colSpan={columns.length} className="list-empty">
                    No {title.toLowerCase()} match your filters. Try another
                    search or reset the filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
      <p className="demo-note">
        Demo records only. Connect your microservices to display live data.
      </p>
    </main>
  );
}

export function OrderList() {
  return (
    <ListPage
      title="Orders"
      description="Browse customer orders and their fulfillment status."
      rows={orders}
      searchKeys={["id", "customer"]}
      filterKey="status"
      filterLabel="Statuses"
      filterOptions={[...new Set(orders.map((order) => order.status))]}
      columns={[
        {
          key: "id",
          label: "Order",
          render: (row) => <strong>#{row.id}</strong>,
        },
        { key: "customer", label: "Customer" },
        { key: "date", label: "Date" },
        { key: "total", label: "Amount", render: (row) => money(row.total) },
        {
          key: "status",
          label: "Status",
          render: (row) => (
            <span className={`status ${row.status.toLowerCase()}`}>
              {row.status}
            </span>
          ),
        },
      ]}
    />
  );
}

const productRows = products.map((product) => ({
  ...product,
  category:
    categories.find((category) => category.id === product.categoryId)?.name ||
    "Uncategorized",
}));

export function ProductList() {
  return (
    <ListPage
      title="Products"
      description="View your catalog, pricing, and available stock."
      rows={productRows}
      searchKeys={["name", "sku", "category"]}
      filterKey="category"
      filterLabel="Categories"
      filterOptions={categories.map((category) => category.name)}
      columns={[
        { key: "sku", label: "SKU" },
        {
          key: "name",
          label: "Product",
          render: (row) => <strong>{row.name}</strong>,
        },
        { key: "category", label: "Category" },
        { key: "price", label: "Price", render: (row) => money(row.price) },
        { key: "stock", label: "Stock", render: (row) => `${row.stock} units` },
      ]}
    />
  );
}

const categoryRows = categories.map((category) => ({
  ...category,
  productCount: products.filter((product) => product.categoryId === category.id)
    .length,
}));

export function CategoryList() {
  return (
    <ListPage
      title="Categories"
      description="Browse the categories that organize your product catalog."
      rows={categoryRows}
      searchKeys={["id", "name", "description"]}
      columns={[
        { key: "id", label: "Category ID" },
        {
          key: "name",
          label: "Category",
          render: (row) => <strong>{row.name}</strong>,
        },
        { key: "description", label: "Description" },
        { key: "productCount", label: "Products" },
      ]}
    />
  );
}
