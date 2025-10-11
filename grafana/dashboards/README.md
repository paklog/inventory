# Grafana Dashboards for Inventory

This directory contains Grafana dashboard JSON files for monitoring the Inventory application using the full observability stack: Prometheus, Loki, and Tempo.

## Available Dashboards

### 1. Inventory - Overview (`inventory-overview.json`)
Complete overview dashboard combining metrics and logs.

**Features:**
- Application health status
- Request rate and throughput
- Response time metrics (average and percentiles: p50, p95, p99)
- Error rate gauge
- HTTP status code distribution
- JVM memory usage
- JVM thread states
- CPU usage (system and process)
- Live application logs stream

**Data Sources Required:**
- Prometheus
- Loki

### 2. Inventory - Logs (`inventory-logs.json`)
Dedicated logging dashboard with advanced filtering.

**Features:**
- Log volume by level (ERROR, WARN, INFO, DEBUG)
- Total errors and warnings counters
- Error rate over time
- Searchable log viewer with filters
- Error logs panel
- Logs with trace IDs for correlation

**Data Sources Required:**
- Loki

**Variables:**
- `search_query`: Free text search filter
- `log_level`: Multi-select log level filter

### 3. Inventory - Traces (`inventory-traces.json`)
Distributed tracing dashboard powered by Tempo.

**Features:**
- Trace duration distribution
- Total traces counter
- Average trace duration
- Trace search interface
- Error traces viewer
- GET and POST request traces
- Slow traces detection (configurable threshold)

**Data Sources Required:**
- Tempo

**Variables:**
- `slow_threshold`: Threshold in milliseconds for slow traces (default: 1000ms)

## How to Import

### Option 1: Grafana UI
1. Open Grafana in your browser
2. Click **Dashboards** → **Import**
3. Click **Upload JSON file**
4. Select one of the dashboard files from this directory
5. Configure data sources:
   - Select your **Prometheus** data source
   - Select your **Loki** data source
   - Select your **Tempo** data source
6. Click **Import**

### Option 2: Grafana API
```bash
# Import overview dashboard
curl -X POST \
  -H "Content-Type: application/json" \
  -d @inventory-overview.json \
  http://admin:admin@localhost:3000/api/dashboards/db

# Import logs dashboard
curl -X POST \
  -H "Content-Type: application/json" \
  -d @inventory-logs.json \
  http://admin:admin@localhost:3000/api/dashboards/db

# Import traces dashboard
curl -X POST \
  -H "Content-Type: application/json" \
  -d @inventory-traces.json \
  http://admin:admin@localhost:3000/api/dashboards/db
```

### Option 3: Grafana Provisioning
Copy the dashboard files to your Grafana provisioning directory:
```bash
cp *.json /etc/grafana/provisioning/dashboards/
```

Then create a provisioning configuration file at `/etc/grafana/provisioning/dashboards/inventory.yaml`:
```yaml
apiVersion: 1

providers:
  - name: 'Inventory'
    orgId: 1
    folder: 'Inventory'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
      foldersFromFilesStructure: true
```

## Data Source Configuration

### Prometheus
The application exposes Prometheus metrics at `http://localhost:8082/actuator/prometheus`

Configure Prometheus to scrape this endpoint:
```yaml
scrape_configs:
  - job_name: 'inventory'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8082']
        labels:
          application: 'inventory'
          environment: 'dev'
```

### Loki
The application sends logs to Loki at `http://localhost:3100` (configured in `application.yml`)

Configure Grafana Loki data source:
- URL: `http://localhost:3100`
- No authentication required (for local development)

### Tempo
The application sends traces to Tempo via OTLP at `http://localhost:4318/v1/traces` (configured in `application.yml`)

Configure Grafana Tempo data source:
- URL: `http://localhost:3100` (Tempo's query endpoint)
- No authentication required (for local development)

## Key Metrics Available

### HTTP Metrics
- `http_server_requests_seconds_count` - Request count
- `http_server_requests_seconds_sum` - Total request duration
- `http_server_requests_seconds_bucket` - Request duration histogram

### JVM Metrics
- `jvm_memory_used_bytes` - Memory usage by area
- `jvm_threads_states_threads` - Thread states
- `system_cpu_usage` - System CPU usage
- `process_cpu_usage` - Process CPU usage

### Application Health
- `up` - Application availability

## Log Labels

Logs are labeled with:
- `service`: "inventory"
- `host`: Hostname
- `level`: Log level (ERROR, WARN, INFO, DEBUG)

## Trace Attributes

Traces include:
- `service.name`: "inventory"
- `trace_id`: Distributed trace ID
- `span_id`: Span ID
- `status`: Trace status (ok, error)
- `name`: Operation name
- `duration`: Trace/span duration

## Tips

1. **Correlation**: Use trace IDs from logs to find related traces in Tempo
2. **Filtering**: Use dashboard variables to filter by log level, search terms, or trace duration
3. **Time Range**: Adjust the time range picker to view historical data
4. **Refresh Rate**: Dashboards auto-refresh every 10-30 seconds
5. **Alerts**: Consider setting up alert rules for error rates, slow traces, and resource usage

## Troubleshooting

### No Data in Dashboards
- Verify the application is running on port 8082
- Check that data sources are configured correctly in Grafana
- Confirm Prometheus is scraping metrics from `/actuator/prometheus`
- Verify Loki is receiving logs at `http://localhost:3100`
- Ensure Tempo is receiving traces at `http://localhost:4318/v1/traces`

### Missing Metrics
- Check application logs for errors
- Verify `management.endpoints.web.exposure.include` includes required endpoints
- Ensure dependencies are correctly configured in `pom.xml`

### Trace Correlation Issues
- Verify `management.tracing.sampling.probability` is set (currently 1.0 = 100%)
- Check that trace context is propagated through all services
- Ensure trace IDs are included in log messages
