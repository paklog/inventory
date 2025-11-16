# Inventory Stock Initialization Simulation

## Overview

This directory contains k6 load testing scripts for initializing inventory stock levels. The scripts are designed to be run weekly to populate the inventory system with stock data from the product catalog.

## Purpose

- Fetch all available products from the Product Catalog service
- Initialize stock levels for each SKU with random quantities between 500-2000 units
- Use the industry-standard absolute set operation (PUT endpoint)
- Provide detailed logging and metrics

## Prerequisites

### Required Services

1. **Product Catalog Service** running on `http://localhost:8082` (default)
2. **Inventory Service** running on `http://localhost:8085` (default)

### Required Tools

- **k6** - Modern load testing tool

#### Install k6

**macOS:**
```bash
brew install k6
```

**Linux (Debian/Ubuntu):**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows (Chocolatey):**
```bash
choco install k6
```

**Docker:**
```bash
docker pull grafana/k6:latest
```

For more installation options: https://k6.io/docs/get-started/installation/

## Files

| File | Description |
|------|-------------|
| `stock-initialization.js` | Main k6 script for stock initialization |
| `run-stock-init.sh` | Bash wrapper script with service checks |
| `README.md` | This documentation file |

## Usage

### Quick Start

Run with default settings (500-2000 units per SKU):

```bash
./simulation/run-stock-init.sh
```

### Custom Configuration

Override default values using environment variables:

```bash
# Custom stock range (1000-3000 units)
MIN_STOCK=1000 MAX_STOCK=3000 ./simulation/run-stock-init.sh

# Custom service URLs
PRODUCT_CATALOG_URL=http://catalog:8082 \
INVENTORY_URL=http://inventory:8085 \
./simulation/run-stock-init.sh

# All custom settings
PRODUCT_CATALOG_URL=http://localhost:8082 \
INVENTORY_URL=http://localhost:8085 \
MIN_STOCK=500 \
MAX_STOCK=2000 \
./simulation/run-stock-init.sh
```

### Run k6 Script Directly

```bash
k6 run \
  -e PRODUCT_CATALOG_URL=http://localhost:8082 \
  -e INVENTORY_URL=http://localhost:8085 \
  -e MIN_STOCK=500 \
  -e MAX_STOCK=2000 \
  simulation/stock-initialization.js
```

### Docker Execution

```bash
docker run --rm -i \
  --network=host \
  -v $(pwd)/simulation:/simulation \
  grafana/k6:latest run \
  -e PRODUCT_CATALOG_URL=http://localhost:8082 \
  -e INVENTORY_URL=http://localhost:8085 \
  -e MIN_STOCK=500 \
  -e MAX_STOCK=2000 \
  /simulation/stock-initialization.js
```

## Configuration Options

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `PRODUCT_CATALOG_URL` | `http://localhost:8082` | Product Catalog service URL |
| `INVENTORY_URL` | `http://localhost:8085` | Inventory service URL |
| `MIN_STOCK` | `500` | Minimum stock quantity per SKU |
| `MAX_STOCK` | `2000` | Maximum stock quantity per SKU |

## Script Behavior

### Execution Flow

1. **Setup Phase**
   - Display configuration
   - Test service connectivity
   - Validate both services are reachable

2. **Fetch Products**
   - Query Product Catalog service
   - Paginate through all products (20 per page)
   - Collect all available SKUs

3. **Initialize Stock**
   - For each SKU:
     - Generate random quantity between MIN_STOCK and MAX_STOCK
     - Call `PUT /inventory/stock_levels/{sku}/set`
     - Log success/failure
   - Small delay (200ms) between requests to avoid overwhelming the server

4. **Summary Report**
   - Total SKUs processed
   - Successful updates
   - Failed updates
   - Success rate percentage

### API Endpoints Used

**Product Catalog:**
- `GET /products?offset={offset}&limit=20` - Fetch products (paginated)

**Inventory:**
- `PUT /inventory/stock_levels/{sku}/set` - Set absolute stock level

### Request Example

```json
PUT /inventory/stock_levels/SKU-12345/set
{
  "quantity": 1547,
  "reasonCode": "INITIAL_STOCK",
  "comment": "Weekly stock initialization - 2025-11-09T19:00:00Z",
  "sourceSystem": "K6_SIMULATION",
  "sourceOperatorId": "automation"
}
```

## Metrics & Monitoring

### k6 Metrics

The script tracks the following metrics:

- **http_req_duration**: HTTP request duration (p95 < 5000ms threshold)
- **errors**: Error rate (< 10% threshold)
- **stock_added**: Success rate of stock additions

### Console Output

The script provides detailed console output:

```
📦 Fetching products from catalog...
✅ Fetched page 1/3 - 20 products
✅ Fetched page 2/3 - 20 products
✅ Fetched page 3/3 - 15 products
📊 Total products fetched: 55

🚀 Starting stock initialization for 55 SKUs...
📊 Stock range: 500 - 2000 units per SKU

[1/55] Processing SKU-001 - 1547 units
✅ Stock set for SKU-001: 1547 units
[2/55] Processing SKU-002 - 892 units
✅ Stock set for SKU-002: 892 units
...

============================================================
📊 STOCK INITIALIZATION SUMMARY
============================================================
Total SKUs processed: 55
✅ Successful: 55
❌ Failed: 0
Success rate: 100.00%
============================================================
```

## Scheduling

### Weekly Cron Job (Linux/macOS)

Add to crontab (`crontab -e`):

```bash
# Run every Monday at 2 AM
0 2 * * 1 cd /path/to/inventory && ./simulation/run-stock-init.sh >> /var/log/stock-init.log 2>&1

# Run every Sunday at 11 PM
0 23 * * 0 cd /path/to/inventory && ./simulation/run-stock-init.sh >> /var/log/stock-init.log 2>&1
```

### Systemd Timer (Linux)

Create `/etc/systemd/system/stock-init.service`:

```ini
[Unit]
Description=Inventory Stock Initialization
After=network.target

[Service]
Type=oneshot
WorkingDirectory=/path/to/inventory
ExecStart=/path/to/inventory/simulation/run-stock-init.sh
User=your-user
Environment="PRODUCT_CATALOG_URL=http://localhost:8082"
Environment="INVENTORY_URL=http://localhost:8085"
Environment="MIN_STOCK=500"
Environment="MAX_STOCK=2000"
```

Create `/etc/systemd/system/stock-init.timer`:

```ini
[Unit]
Description=Weekly Stock Initialization Timer
Requires=stock-init.service

[Timer]
OnCalendar=weekly
Persistent=true

[Install]
WantedBy=timers.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable stock-init.timer
sudo systemctl start stock-init.timer
```

### GitHub Actions (CI/CD)

Create `.github/workflows/stock-init.yml`:

```yaml
name: Weekly Stock Initialization

on:
  schedule:
    - cron: '0 2 * * 1'  # Every Monday at 2 AM UTC
  workflow_dispatch:  # Manual trigger

jobs:
  initialize-stock:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Install k6
        run: |
          sudo gpg -k
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update
          sudo apt-get install k6

      - name: Run Stock Initialization
        env:
          PRODUCT_CATALOG_URL: ${{ secrets.PRODUCT_CATALOG_URL }}
          INVENTORY_URL: ${{ secrets.INVENTORY_URL }}
        run: ./simulation/run-stock-init.sh
```

## Troubleshooting

### Service Not Reachable

**Error:**
```
❌ Product Catalog service is not available at http://localhost:8082
```

**Solution:**
- Verify services are running: `docker ps` or `ps aux | grep java`
- Check service ports: `netstat -an | grep 8082`
- Test manually: `curl http://localhost:8082/products?limit=1`

### k6 Not Found

**Error:**
```
❌ k6 is not installed!
```

**Solution:**
- Install k6 using instructions in Prerequisites section
- Verify installation: `k6 version`

### High Error Rate

**Error:**
```
✗ errors........................: 15.00%
```

**Possible Causes:**
- Inventory service not started
- Database connection issues
- Network problems
- SKUs don't exist in inventory (need to be created first)

**Solution:**
- Check inventory service logs
- Verify database connectivity
- Ensure SKUs are pre-created in inventory system

### Permission Denied

**Error:**
```
bash: ./simulation/run-stock-init.sh: Permission denied
```

**Solution:**
```bash
chmod +x simulation/run-stock-init.sh
```

## Best Practices

1. **Run During Off-Peak Hours**: Schedule for low-traffic periods (e.g., 2 AM)
2. **Monitor Execution**: Check logs regularly for failures
3. **Adjust Stock Range**: Modify MIN_STOCK and MAX_STOCK based on business needs
4. **Backup Before Running**: Consider backing up inventory data before bulk updates
5. **Test in Staging First**: Validate script in staging environment before production

## Performance Considerations

- **Request Rate**: 200ms delay between requests (5 requests/second max)
- **Typical Duration**: ~1 minute for 50 SKUs, ~10 minutes for 500 SKUs
- **Resource Usage**: Low CPU and memory footprint
- **Network**: Depends on service latency and number of products

## Support

For issues or questions:
- Check the main inventory service logs
- Review k6 output for specific error messages
- Consult the OpenAPI specification in `openapi.yaml`

## Version History

- **1.0.0** (2025-11-09): Initial release with k6 implementation

## License

Apache 2.0
