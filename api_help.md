# Inventory Management API Developer Guide

Welcome to the Inventory Management API! This guide provides everything you need to start using our API to manage and query logical inventory levels. Our API serves as the single source of truth for product stock across the fulfillment network.

## Base URL

All API endpoints are relative to the following base URL:

`https://api.example.com/fulfillment/inventory/v1`

## Core Concepts

- **SKU (Stock Keeping Unit):** The unique identifier for a product whose inventory is being tracked.
- **Stock Level:** Represents the quantity of a SKU at different states: physical on-hand, allocated to orders, and available to promise for new sales.
- **Adjustment:** A manual change made to the `quantity_on_hand` to correct discrepancies.

---

## API Endpoints

Here are the detailed descriptions of the available API endpoints.

### 1. Stock Levels Resource

This resource represents the inventory level for a single SKU.

#### GET `/stock_levels/{sku}`

- **Use Case:** Check the real-time availability of a product. This is essential for e-commerce websites to display stock status to customers, or for internal systems to verify availability before creating an order.

- **API Details:**
  - **Method:** `GET`
  - **Path Parameter:**
    - `sku` (string, required): The unique seller SKU to query.

- **Example Request:**
  ```http
  GET /stock_levels/TSHIRT-RED-L
  ```

- **Success Response (200 OK):**
  Returns the `StockLevel` object for the specified SKU.
  ```json
  {
    "sku": "TSHIRT-RED-L",
    "quantity_on_hand": 100,
    "quantity_allocated": 15,
    "available_to_promise": 85
  }
  ```

- **Error Responses:**
  - `404 Not Found`: No stock record was found for the given SKU.

#### PATCH `/stock_levels/{sku}`

- **Use Case:** Manually adjust the inventory count for a SKU. This is critical for inventory managers to correct stock levels after a physical count (cycle count) or to write off damaged or lost goods.

- **API Details:**
  - **Method:** `PATCH`
  - **Path Parameter:**
    - `sku` (string, required): The unique seller SKU to adjust.
  - **Request Body:** Requires an `UpdateStockLevelRequest` object.

- **Example Request:**
  To report 5 units as damaged:
  ```http
  PATCH /stock_levels/TSHIRT-RED-L
  Content-Type: application/json

  {
    "quantity_change": -5,
    "reason_code": "damaged",
    "comment": "Forklift accident in warehouse A."
  }
  ```

- **Success Response (200 OK):**
  The adjustment was successful and the updated `StockLevel` object is returned.
  ```json
  {
    "sku": "TSHIRT-RED-L",
    "quantity_on_hand": 95,
    "quantity_allocated": 15,
    "available_to_promise": 80
  }
  ```

- **Error Responses:**
  - `400 Bad Request`: The request body is invalid (e.g., missing `quantity_change` or `reason_code`).
  - `404 Not Found`: No stock record was found for the SKU to be adjusted.

---

### 2. Inventory Intelligence Resource

This resource provides high-level analytics and health metrics about the overall inventory.

#### GET `/inventory_health_metrics`

- **Use Case:** Get a strategic overview of inventory performance. This helps business analysts and managers identify slow-moving (dead) stock, calculate inventory turnover, and monitor the number of out-of-stock products.

- **API Details:**
  - **Method:** `GET`
  - **Query Parameters (Optional):**
    - `category` (string): Filter metrics by a specific product category.
    - `start_date` (string): The start date for the analysis period (format: `YYYY-MM-DD`).
    - `end_date` (string): The end date for the analysis period (format: `YYYY-MM-DD`).

- **Example Request:**
  ```http
  GET /inventory_health_metrics?category=womens-apparel&start_date=2023-01-01
  ```

- **Success Response (200 OK):**
  Returns the `InventoryHealthMetrics` object.
  ```json
  {
    "inventory_turnover": 4.2,
    "dead_stock_skus": [
      "SKU-OLD-MODEL-01",
      "SKU-PROMO-ITEM-99"
    ],
    "total_skus": 5250,
    "out_of_stock_skus": 132
  }
  ```

- **Error Responses:**
  - `400 Bad Request`: The query parameters are invalid.

---

## Data Models (Schemas)

### StockLevel

Represents the complete inventory level for a single SKU.

| Field                  | Type    | Description                                                              |
| ---------------------- | ------- | ------------------------------------------------------------------------ |
| `sku`                  | string  | The unique identifier for the product.                                   |
| `quantity_on_hand`     | integer | The total physical quantity of the item in the warehouse.                |
| `quantity_allocated`   | integer | The quantity reserved for open orders that have not yet been picked.     |
| `available_to_promise` | integer | The quantity available for new sales (`quantity_on_hand` - `quantity_allocated`). |

### UpdateStockLevelRequest

Describes the adjustment to be made to a stock level via the `PATCH` endpoint.

| Field             | Type    | Description                                                                                                |
| ----------------- | ------- | ---------------------------------------------------------------------------------------------------------- |
| `quantity_change` | integer | The change to apply to `quantity_on_hand`. Can be positive (e.g., found stock) or negative (e.g., damaged). |
| `reason_code`     | string  | A code indicating the reason for the adjustment. Enum values include `damaged`, `cycle_count`, `lost`, etc. |
| `comment`         | string  | (Optional) A free-text comment explaining the adjustment in more detail.                                   |

### InventoryHealthMetrics

Provides an analytical overview of the entire inventory.

| Field                 | Type           | Description                                                                 |
| --------------------- | -------------- | --------------------------------------------------------------------------- |
| `inventory_turnover`  | number         | The rate at which inventory is sold, used, or replaced.                     |
| `dead_stock_skus`     | array[string]  | A list of SKUs identified as dead stock (no movement over a period).        |
| `total_skus`          | integer        | Total number of unique SKUs being tracked.                                  |
| `out_of_stock_skus`   | integer        | The number of SKUs that currently have zero quantity on hand.               |
