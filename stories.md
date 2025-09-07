# Epic 1: Core ProductStock Model & State Management
**Goal:** To build the heart of the service: the ProductStock aggregate that accurately tracks inventory levels and enforces the rules of stock calculation.

| Story ID | User Story | Acceptance Criteria |
|----------|------------|----------------------|
| **INV-01** | As a DevOps Engineer, I want to set up a new microservice foundation so that the development team has a standardized, buildable, and deployable starting point. | - A new git repository (`inventory-service`) is created.<br>- A CI/CD pipeline is configured for automated builds, testing, and deployment.<br>- The service includes basic dependencies for a web framework, persistence, and logging. |
| **INV-02** | As a Developer, I want to implement the `ProductStock` aggregate so that the system can model the inventory level of a single SKU as a consistent unit. | - A `ProductStock` aggregate root is created.<br>- It contains the properties: `quantity_on_hand`, `quantity_allocated`.<br>- It includes a calculated, read-only property for `available_to_promise` (`on_hand - allocated`).<br>- Methods like `allocate(quantity)` and `deallocate(quantity)` contain the business logic to modify the state. |
| **INV-03** | As a Developer, I want to implement a persistence layer for the ProductStock aggregate so that inventory state is durable and can be reliably retrieved. | - A database schema is designed to store the `ProductStock` aggregate.<br>- A Repository pattern is implemented to handle loading and saving the aggregate to the database. |

---

# Epic 2: Event-Driven Integration & Choreography
**Goal:** To make the service a good citizen in our event-driven ecosystem, allowing it to react to changes in other contexts and to proactively broadcast its own state changes.

| Story ID | User Story | Acceptance Criteria |
|----------|------------|----------------------|
| **INV-04** | As the System, I want to consume an event that signals inventory needs to be reserved so that I can decrease the `AvailableToPromise` quantity for a SKU. | - A Kafka consumer is implemented to listen for an `InventoryAllocationRequested` event (or similar, from Warehouse Operations).<br>- Upon receiving the event, the corresponding `ProductStock` aggregate is loaded, its `allocate()` method is called, and the aggregate is saved. |
| **INV-05** | As the System, I want to consume an event that signals physical inventory has been removed from the shelf so that I can decrease the `QuantityOnHand`. | - The Kafka consumer listens for an `ItemPicked` event from Warehouse Operations.<br>- Upon receiving the event, the `ProductStock` aggregate is updated to decrease `quantity_on_hand` and reverse the previous allocation (by calling `deallocate()`). |
| **INV-06** | As the System, whenever the `AvailableToPromise` for a product changes, I want to publish a `StockLevelChanged` event so that sales channels and other interested parties can be notified in near real-time. | - After any transaction that modifies a `ProductStock` aggregate, a `StockLevelChanged` event is generated.<br>- The event contains the `sku` and the new `available_to_promise` quantity.<br>- The event is published to a dedicated Kafka topic (e.g., `fulfillment.inventory.v1.events`). |
| **INV-07** | As a Developer, I want to implement the Transactional Outbox pattern for publishing events so that we can guarantee that stock level updates are sent if and only if the business transaction was successfully committed. | - An outbox table is added to the service's database.<br>- Saving the `ProductStock` aggregate and writing its `StockLevelChanged` event to the outbox occur in the same database transaction.<br>- A separate worker process reliably reads from the outbox and publishes events to Kafka. |

---

# Epic 3: Inventory Adjustments & Auditing
**Goal:** To provide the necessary tools for inventory managers to maintain accuracy and for the business to have a complete, traceable history of every stock movement.

| Story ID | User Story | Acceptance Criteria |
|----------|------------|----------------------|
| **INV-08** | As an Inventory Manager, I want a way to perform a manual stock adjustment so that I can correct discrepancies found during a cycle count or due to damage. | - A secure `POST /inventory/adjustments` REST endpoint is created.<br>- The request body accepts a `sku`, the `quantity_change` (positive or negative), and a `reason_code`.<br>- The endpoint triggers a command that updates the `quantity_on_hand` of the `ProductStock` aggregate. |
| **INV-09** | As the System, I want to maintain a complete audit trail of every change to a product's stock level so that we can trace the source of any discrepancy. | - An `inventory_ledger` table is created.<br>- Every operation that changes a `ProductStock` aggregate (allocation, pick, adjustment, return) must write an immutable entry to the ledger.<br>- The ledger entry includes the `sku`, `timestamp`, `quantity_change`, `type_of_change`, and a reference to the source (e.g., `order_id`). |

---

# Epic 4: Providing Inventory Visibility
**Goal:** To expose inventory data through a clear query interface, allowing both humans and other systems to get the information they need to make decisions.

| Story ID | User Story | Acceptance Criteria |
|----------|------------|----------------------|
| **INV-10** | As a Merchandising Manager, I want an API to check the current stock level for a specific product so that I can make informed decisions about promotions and purchasing. | - A `GET /inventory/stock_levels/{sku}` REST endpoint is created.<br>- The endpoint returns the current `quantity_on_hand`, `quantity_allocated`, and `available_to_promise` for the given SKU. |
| **INV-11** | As a Data Analyst, I want to retrieve inventory health metrics, like inventory turnover, so that I can analyze the performance of our product catalog. | - A `GET /inventory/reports/health` endpoint is created.<br>- The endpoint can accept parameters to filter by product category or date range.<br>- The system calculates and returns key metrics like `inventory_turnover` and identifies `dead_stock` (items with no movement over a period). |
