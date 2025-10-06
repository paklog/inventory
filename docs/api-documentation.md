# Inventory Management API Documentation

## Overview

The Inventory Management API provides a comprehensive RESTful interface for managing product stock across the fulfillment network. This API is designed for high performance, scalability, and reliability.

## Base URL

```
Production:  https://api.paklog.com/fulfillment/inventory/v1
Staging:     https://api-staging.paklog.com/fulfillment/inventory/v1
Local:       http://localhost:8085/inventory
```

## Interactive Documentation

- **Swagger UI**: http://localhost:8085/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8085/api-docs
- **OpenAPI YAML**: Available in `openapi.yaml`

## Authentication

All API requests require authentication using Bearer tokens:

```http
Authorization: Bearer <your-access-token>
```

## Core Endpoints

### 1. Get Stock Level

Retrieves current inventory levels for a specific SKU.

**Endpoint**: `GET /stock_levels/{sku}`

**Request Example**:
```bash
curl -X GET \
  'http://localhost:8085/inventory/stock_levels/SKU-12345' \
  -H 'Authorization: Bearer <token>'
```

**Response Example** (200 OK):
```json
{
  "sku": "SKU-12345",
  "quantity_on_hand": 500,
  "quantity_allocated": 150,
  "available_to_promise": 350
}
```

**Performance**:
- L1 Cache Hit: < 1ms
- L2 Cache Hit: < 5ms
- Database Query: < 50ms

---

### 2. Adjust Stock Level

Manually adjust inventory quantity with audit trail.

**Endpoint**: `PATCH /stock_levels/{sku}`

**Request Example**:
```bash
curl -X PATCH \
  'http://localhost:8085/inventory/stock_levels/SKU-12345' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{
    "quantity_change": 100,
    "reason_code": "stock_intake",
    "comment": "New shipment received from supplier XYZ"
  }'
```

**Request Body**:
```json
{
  "quantity_change": 100,
  "reason_code": "stock_intake",
  "comment": "New shipment received"
}
```

**Reason Codes**:
- `stock_intake`: New stock received
- `damaged`: Damaged inventory removal
- `cycle_count`: Cycle count adjustment
- `lost`: Lost inventory write-off
- `found`: Found inventory correction

**Response Example** (200 OK):
```json
{
  "sku": "SKU-12345",
  "quantity_on_hand": 600,
  "quantity_allocated": 150,
  "available_to_promise": 450
}
```

**Events Published**:
- `com.paklog.inventory.stock_level.changed.v1`

---

### 3. Create Stock Reservation

Allocate stock for a specific order.

**Endpoint**: `POST /stock_levels/{sku}/reservations`

**Request Example**:
```bash
curl -X POST \
  'http://localhost:8085/inventory/stock_levels/SKU-12345/reservations' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{
    "quantity": 10,
    "order_id": "ORD-2024-001"
  }'
```

**Request Body**:
```json
{
  "quantity": 10,
  "order_id": "ORD-2024-001"
}
```

**Response** (202 Accepted):
```
HTTP 202 Accepted
```

**Events Published**:
- `com.paklog.inventory.stock.allocated.v1`

---

### 4. Bulk Stock Allocation

High-performance endpoint for processing large order batches.

**Endpoint**: `POST /allocations/bulk`

**Performance**: Handles 10,000+ allocations in < 5 seconds

**Request Example**:
```bash
curl -X POST \
  'http://localhost:8085/inventory/allocations/bulk' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{
    "requests": [
      {
        "sku": "SKU-12345",
        "quantity": 10,
        "order_id": "ORD-001"
      },
      {
        "sku": "SKU-67890",
        "quantity": 5,
        "order_id": "ORD-002"
      },
      {
        "sku": "SKU-11111",
        "quantity": 20,
        "order_id": "ORD-003"
      }
    ]
  }'
```

**Response Example** (200 OK):
```json
{
  "successful_allocations": 2,
  "failed_allocations": 1,
  "processing_time_ms": 450,
  "results": [
    {
      "sku": "SKU-12345",
      "order_id": "ORD-001",
      "success": true,
      "allocated_quantity": 10
    },
    {
      "sku": "SKU-67890",
      "order_id": "ORD-002",
      "success": true,
      "allocated_quantity": 5
    },
    {
      "sku": "SKU-11111",
      "order_id": "ORD-003",
      "success": false,
      "error_message": "Insufficient stock: available=15, requested=20"
    }
  ]
}
```

**Features**:
- Parallel processing with thread pools
- Partial success handling
- Detailed error reporting per item
- Optimistic locking for concurrency

---

### 5. Get Inventory Health Metrics

Retrieve inventory turnover and dead stock analysis.

**Endpoint**: `GET /inventory_health_metrics`

**Query Parameters**:
- `category` (optional): Filter by product category
- `start_date` (optional): Start date (ISO 8601 format: YYYY-MM-DD)
- `end_date` (optional): End date (ISO 8601 format: YYYY-MM-DD)

**Request Example**:
```bash
curl -X GET \
  'http://localhost:8085/inventory/inventory_health_metrics?start_date=2025-09-01&end_date=2025-10-05' \
  -H 'Authorization: Bearer <token>'
```

**Response Example** (200 OK):
```json
{
  "inventory_turnover": 4.5,
  "dead_stock_skus": [
    "SKU-111",
    "SKU-222",
    "SKU-333"
  ],
  "total_skus": 1500,
  "out_of_stock_skus": 25
}
```

**Metrics Explained**:
- `inventory_turnover`: Rate at which inventory is sold/used (higher is better)
- `dead_stock_skus`: SKUs with no movement in the analysis period
- `total_skus`: Total number of unique SKUs tracked
- `out_of_stock_skus`: Number of SKUs currently with zero quantity

**Performance**:
- Uses MongoDB aggregation pipeline
- Optimized for large datasets (100K+ SKUs)
- Response time: < 2 seconds for 30-day analysis

---

## Error Responses

All endpoints return standard error responses:

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "Invalid quantity: must be positive",
  "timestamp": "2025-10-05T20:00:00Z",
  "trace_id": "a1b2c3d4e5f6"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "No stock record found for SKU: SKU-INVALID",
  "timestamp": "2025-10-05T20:00:00Z",
  "trace_id": "a1b2c3d4e5f6"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2025-10-05T20:00:00Z",
  "trace_id": "a1b2c3d4e5f6"
}
```

**Trace ID**: Include this in support tickets for faster debugging

---

## Rate Limiting

| Tier | Requests/Second | Burst |
|------|----------------|-------|
| Standard | 100 | 150 |
| Premium | 500 | 750 |
| Enterprise | 2000 | 3000 |

Rate limit headers:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1633456800
```

---

## Caching Strategy

### L1 Cache (Caffeine - In-Memory)
- **TTL**: 5 minutes (write), 2 minutes (access)
- **Size**: 10,000 entries max
- **Use Case**: High-frequency stock queries

### L2 Cache (Redis - Distributed)
- **Product Stock**: 5 min TTL
- **SKU List**: 30 min TTL
- **ABC Classification**: 1 hour TTL
- **Use Case**: Cross-instance sharing

### Cache Invalidation
Automatic cache eviction on:
- Stock adjustments
- Stock allocations
- Stock receipts

---

## Event-Driven Integration

All state changes publish CloudEvents v1.0 to Kafka:

### Stock Level Changed Event
```json
{
  "specversion": "1.0",
  "type": "com.paklog.inventory.stock_level.changed.v1",
  "source": "inventory-service",
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "time": "2025-10-05T20:00:00Z",
  "datacontenttype": "application/json",
  "data": {
    "sku": "SKU-12345",
    "previous_quantity_on_hand": 500,
    "new_quantity_on_hand": 600,
    "change_type": "ADJUSTMENT",
    "reason_code": "stock_intake",
    "operator_id": "admin"
  }
}
```

### Stock Allocated Event
```json
{
  "specversion": "1.0",
  "type": "com.paklog.inventory.stock.allocated.v1",
  "source": "inventory-service",
  "id": "b2c3d4e5-f6g7-h8i9-j0k1-l2m3n4o5p6q7",
  "time": "2025-10-05T20:00:00Z",
  "datacontenttype": "application/json",
  "data": {
    "sku": "SKU-12345",
    "quantity": 10,
    "order_id": "ORD-2024-001",
    "remaining_available": 340
  }
}
```

---

## Observability

### Distributed Tracing (OpenTelemetry)
- **Trace Endpoint**: http://localhost:4318/v1/traces
- **Sampling**: 100% (adjustable in production)
- **Trace ID**: Returned in `trace_id` field of errors

### Metrics (Prometheus)
- **Metrics Endpoint**: http://localhost:8085/actuator/prometheus
- **Custom Metrics**:
  - `inventory_stock_adjustment_total`
  - `inventory_stock_allocation_total`
  - `inventory_stock_level_query_total`
  - `inventory_operation_duration_seconds`
  - `inventory_cache_hit_ratio`

### Health Checks
- **Health Endpoint**: http://localhost:8085/actuator/health
- **Components**: MongoDB, Redis, Kafka

---

## Best Practices

### 1. Use Bulk Endpoints for High Volume
For processing > 100 allocations, use `/allocations/bulk` instead of individual calls.

### 2. Handle Partial Failures
Bulk operations may partially succeed. Always check individual results.

### 3. Include Correlation IDs
Pass `X-Correlation-ID` header for request tracing:
```http
X-Correlation-ID: my-app-12345
```

### 4. Retry Strategy
- Implement exponential backoff
- Max retries: 3
- Retry on: 500, 502, 503, 504

### 5. Monitor Rate Limits
Check rate limit headers and implement backoff before hitting limits.

---

## Code Examples

### Java (Spring RestTemplate)
```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(token);
headers.set("X-Correlation-ID", UUID.randomUUID().toString());

HttpEntity<Void> request = new HttpEntity<>(headers);
ResponseEntity<StockLevelResponse> response = restTemplate.exchange(
    "http://localhost:8085/inventory/stock_levels/SKU-12345",
    HttpMethod.GET,
    request,
    StockLevelResponse.class
);

StockLevelResponse stockLevel = response.getBody();
System.out.println("Available: " + stockLevel.getAvailableToPromise());
```

### Python (requests)
```python
import requests

headers = {
    'Authorization': f'Bearer {token}',
    'X-Correlation-ID': 'my-app-12345'
}

response = requests.get(
    'http://localhost:8085/inventory/stock_levels/SKU-12345',
    headers=headers
)

stock_level = response.json()
print(f"Available: {stock_level['available_to_promise']}")
```

### cURL
```bash
curl -X GET \
  'http://localhost:8085/inventory/stock_levels/SKU-12345' \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Correlation-ID: my-app-12345'
```

---

## Support

- **Documentation**: https://docs.paklog.com/inventory
- **OpenAPI Spec**: `/openapi.yaml`
- **Support Email**: platform@paklog.com
- **Slack**: #paklog-platform-support
