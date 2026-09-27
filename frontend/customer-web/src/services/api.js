import { serviceUrls } from "../config/services";

// Integration helper; demo screens do not make backend requests.
// Define your endpoint paths and authentication policy when connecting services.
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
  if (!response.ok)
    throw new Error(`${service}-service request failed (${response.status})`);
  if (response.status === 204) return null;
  return response.headers.get("content-type")?.includes("application/json")
    ? response.json()
    : response.text();
}
