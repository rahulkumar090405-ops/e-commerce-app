import { serviceUrls } from "../config/services";

export async function apiRequest(
  service,
  path,
  { body, headers, ...options } = {},
) {
  if (!serviceUrls[service]) throw new Error(`Unknown service: ${service}`);
  const response = await fetch(
    `${serviceUrls[service].replace(/\/$/, "")}/${path.replace(/^\//, "")}`,
    {
      ...options,
      headers: {
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...headers,
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    },
  );
  const isJson = response.headers.get("content-type")?.includes("json");
  const payload = isJson
    ? await response.json().catch(() => null)
    : await response.text();

  if (!response.ok) {
    const message = payload?.detail || payload?.message || payload?.error ||
      (typeof payload === "string" && payload) ||
      `${service}-service request failed (${response.status})`;
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return payload;
}
