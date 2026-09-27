// Replace with order-service, product-service, and user-service responses.
export const stats = [
  {
    label: "Total revenue",
    value: "$12,840",
    note: "Across all demo orders",
    icon: "$",
  },
  { label: "Orders", value: "148", note: "12 awaiting fulfillment", icon: "↗" },
  { label: "Products", value: "24", note: "22 currently in stock", icon: "□" },
  { label: "Customers", value: "862", note: "68 new this month", icon: "◎" },
];

export const orders = [
  {
    id: "ORD-1048",
    customer: "Alex Morgan",
    date: "Sep 27, 2026",
    total: 129,
    status: "Processing",
  },
  {
    id: "ORD-1047",
    customer: "Jamie Lee",
    date: "Sep 27, 2026",
    total: 64,
    status: "Shipped",
  },
  {
    id: "ORD-1046",
    customer: "Taylor Smith",
    date: "Sep 26, 2026",
    total: 48,
    status: "Delivered",
  },
  {
    id: "ORD-1045",
    customer: "Jordan Patel",
    date: "Sep 26, 2026",
    total: 89,
    status: "Delivered",
  },
];
