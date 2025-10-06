# Inventory Management Business Capabilities

This document outlines the business capabilities of the inventory management system, categorized into a hierarchical structure of L1, L2, and L3 capabilities, including the API endpoints that support them.

## L1: Inventory Management

This is the highest-level capability, encompassing the core functionalities for managing and controlling inventory.

### L2: Stock Control

This capability focuses on the processes and operations related to the quantity and status of stock.

#### L3: Stock Adjustments

- **Description:** Manually adjust the quantity of stock on hand for a specific SKU. This is used to correct discrepancies found during cycle counts or for other reasons.
- **API Endpoints:**
  - `POST /inventory/adjustments`

#### L3: Stock Allocation

- **Description:** Allocate available stock to fulfill orders or other demands. This reduces the "available to promise" quantity without affecting the on-hand quantity.
- **API Endpoints:**
  - `POST /inventory/stock_levels/{sku}/reservations`
  - `POST /inventory/allocations/bulk`

#### L3: Stock Picking

- **Description:** Process the picking of items from inventory. This typically follows an allocation and results in a decrease in the on-hand quantity.
- **Note:** This capability is handled internally by the `InventoryCommandService` but does not have a direct, dedicated public API endpoint in the current implementation. It is often triggered by other processes (e.g., a shipment confirmation).

#### L3: Stock Receiving

- **Description:** Receive new stock into the inventory. This increases the on-hand quantity.
- **Note:** This capability is handled internally by the `InventoryCommandService` but does not have a direct, dedicated public API endpoint in the current implementation. It is often triggered by an external purchasing or receiving system.

### L2: Physical Inventory Management

This capability deals with the physical location and movement of stock within the warehouse.

#### L3: Physical Stock Movements

- **Description:** Move stock from one physical location to another within the warehouse.
- **API Endpoints:**
  - `POST /inventory/physical-stock/movements`

#### L3: Physical Stock Reservation

- **Description:** Reserve a specific quantity of stock at a particular location for an order or other purpose.
- **API Endpoints:**
  - `POST /inventory/physical-stock/physical-reservations`
  - `DELETE /inventory/physical-stock/physical-reservations/{sku}/{aisle}/{shelf}/{bin}/{reservationId}`

#### L3: Physical Stock Picking

- **Description:** Pick a specific quantity of stock from a location, typically fulfilling a reservation.
- **API Endpoints:**
  - `POST /inventory/physical-stock/picks`

### L2: Inventory Holds

This capability allows for placing and releasing holds on inventory for various reasons.

#### L3: Place and Release Holds

- **Description:** Place a hold on a certain quantity of a SKU for reasons such as quality control, legal issues, or recalls. Holds can also be released, making the stock available again.
- **API Endpoints:**
  - `POST /api/v1/inventory/holds`
  - `POST /api/v1/inventory/holds/{sku}/quality`
  - `POST /api/v1/inventory/holds/{sku}/credit`
  - `DELETE /api/v1/inventory/holds/{holdId}`
  - `POST /api/v1/inventory/holds/release-expired`

### L2: Stock Status Management

This capability involves managing the different statuses of stock, such as "available," "quarantine," or "damaged."

#### L3: Change Stock Status

- **Description:** Change the status of a specified quantity of stock. For example, moving stock from "available" to "quarantine."
- **API Endpoints:**
  - `POST /api/v1/inventory/stock-status/change`
  - `POST /api/v1/inventory/stock-status/{sku}/quarantine`
  - `POST /api/v1/inventory/stock-status/{sku}/damaged`
  - `POST /api/v1/inventory/stock-status/{sku}/expired`
  - `POST /api/v1/inventory/stock-status/{sku}/release-quarantine`

### L2: Unit of Measure (UOM) Management

This capability handles conversions between different units of measure for a SKU.

#### L3: UOM Conversion

- **Description:** Define and manage conversion factors between different UOMs for a SKU (e.g., from cases to eaches). The system can then convert quantities between these UOMs.
- **API Endpoints:**
  - `POST /api/v1/inventory/uom/conversions`
  - `POST /api/v1/inventory/uom/convert`
  - `DELETE /api/v1/inventory/uom/conversions/{sku}/{fromUOM}/{toUOM}`

## L1: Product Information Management

This capability focuses on managing product-related information that is relevant to inventory.

### L2: ABC Classification

This capability involves classifying SKUs based on their importance to the business.

#### L3: SKU Classification

- **Description:** Classify SKUs into A, B, or C categories based on criteria such as value, velocity, or a combination of factors.
- **API Endpoints:**
  - `POST /api/v1/inventory/abc-classification/classify`
  - `POST /api/v1/inventory/abc-classification/{sku}/classify-combined`
  - `POST /api/v1/inventory/abc-classification/{sku}/classify-by-value`
  - `POST /api/v1/inventory/abc-classification/{sku}/classify-by-velocity`

#### L3: Re-classification

- **Description:** Periodically re-classify all SKUs to ensure their classification remains accurate.
- **API Endpoints:**
  - `POST /api/v1/inventory/abc-classification/reclassify-all`
  - `GET /api/v1/inventory/abc-classification/requiring-reclassification`

### L2: Kit and Bill of Materials (BOM) Management

This capability deals with the definition and management of kits (or BOMs).

#### L3: Kit Definition

- **Description:** Create and manage the definition of kits, including their components and quantities. Kits can be physical (requiring assembly) or virtual (a logical grouping of items).
- **Note:** This capability is handled internally by the `KitManagementService` but does not have a direct public API endpoint in the current implementation.

#### L3: Kit Assembly and Disassembly

- **Description:** Manage the process of assembling kits from their components and disassembling them back into components.
- **Note:** This capability is handled internally by the `AssemblyService` but does not have a direct public API endpoint in the current implementation.

## L1: Warehouse Operations

This capability covers the operational aspects of managing inventory within a warehouse.

### L2: Containerization (LPN)

This capability involves managing License Plate Numbers (LPNs) or containers to track groups of items.

#### L3: Container Lifecycle Management

- **Description:** Manage the entire lifecycle of a container, from creation to shipping, including moving and nesting containers.
- **API Endpoints:**
  - `POST /api/v1/inventory/containers`
  - `POST /api/v1/inventory/containers/with-lpn`
  - `POST /api/v1/inventory/containers/{lpn}/move`
  - `POST /api/v1/inventory/containers/{lpn}/close`
  - `POST /api/v1/inventory/containers/{lpn}/ship`
  - `POST /api/v1/inventory/containers/{childLpn}/nest`

#### L3: Item Management within Containers

- **Description:** Add items to and remove items from containers.
- **API Endpoints:**
  - `POST /api/v1/inventory/containers/{lpn}/items`
  - `POST /api/v1/inventory/containers/{lpn}/items/remove`
  - `POST /api/v1/inventory/containers/{lpn}/empty`

### L2: Cycle Counting

This capability focuses on the process of periodically counting a subset of inventory to ensure accuracy.

#### L3: Cycle Count Scheduling

- **Description:** Schedule cycle counts based on ABC classification or other criteria.
- **Note:** This capability is handled internally by the `CycleCountService` but does not have a direct public API endpoint in the current implementation.

#### L3: Cycle Count Execution and Adjustment

- **Description:** Manage the execution of cycle counts, including starting, completing, and approving counts. If a variance is found, the system can adjust the inventory accordingly.
- **Note:** This capability is handled internally by the `CycleCountService` but does not have a direct public API endpoint in the current implementation.

### L2: Stock Transfers

This capability deals with moving stock between different locations.

#### L3: Inter-location Transfers

- **Description:** Manage the transfer of stock from a source location to a destination location, including tracking the transfer status (e.g., in-transit, completed).
- **API Endpoints:**
  - `POST /api/v1/inventory/transfers`
  - `POST /api/v1/inventory/transfers/{transferId}/in-transit`
  - `POST /api/v1/inventory/transfers/{transferId}/complete`
  - `POST /api/v1/inventory/transfers/{transferId}/cancel`

## L1: Inventory Intelligence & Reporting

This capability focuses on providing insights and data about the inventory.

### L2: Inventory Valuation

This capability involves calculating and tracking the financial value of the inventory.

#### L3: Costing and Valuation

- **Description:** Initialize and update the valuation of SKUs based on different methods (e.g., standard cost, moving average). This includes calculating the Cost of Goods Sold (COGS).
- **API Endpoints:**
  - `POST /api/v1/inventory/valuation/initialize`
  - `POST /api/v1/inventory/valuation/{sku}/receipt`
  - `POST /api/v1/inventory/valuation/{sku}/issue`
  - `GET /api/v1/inventory/valuation/{sku}/cogs`

### L2: Inventory Visibility & Querying

This capability provides tools for querying and understanding the current state of the inventory.

#### L3: Stock Level Queries

- **Description:** Query the stock level of a SKU, including on-hand, allocated, and available quantities.
- **API Endpoints:**
  - `GET /inventory/stock_levels/{sku}`

#### L3: Inventory Health Metrics

- **Description:** Calculate and retrieve key health metrics for the inventory, such as turnover and dead stock.
- **API Endpoints:**
  - `GET /inventory/reports/health`

### L2: Serial Number Tracking

This capability provides the ability to track individual items by their serial numbers.

#### L3: Serial Number Lifecycle Management

- **Description:** Manage the entire lifecycle of a serial number, from receipt to shipping, including allocation, movement, and status changes.
- **API Endpoints:**
  - `POST /api/v1/inventory/serial-numbers/receive`
  - `POST /api/v1/inventory/serial-numbers/{serialNumber}/allocate`
  - `POST /api/v1/inventory/serial-numbers/{serialNumber}/ship`
  - `POST /api/v1/inventory/serial-numbers/{serialNumber}/move`
  - `POST /api/v1/inventory/serial-numbers/{serialNumber}/status`

### L2: Inventory Snapshots & Auditing

This capability allows for taking snapshots of inventory at specific points in time for auditing and analysis.

#### L3: Snapshot Creation

- **Description:** Create snapshots of inventory for a SKU or for all SKUs. Snapshots can be created on-demand or on a schedule (e.g., daily, monthly).
- **API Endpoints:**
  - `POST /api/v1/inventory/snapshots`

#### L3: Time-travel Queries

- **Description:** Reconstruct the state of inventory at a specific point in time by using a combination of snapshots and event replay. This allows for "time-travel" queries to see what the inventory looked like in the past.
- **API Endpoints:**
  - `GET /api/v1/inventory/snapshots/{sku}/at`
  - `GET /api/v1/inventory/snapshots/all/at`