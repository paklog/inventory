# Grafana Dashboards Troubleshooting

## Common Issues and Solutions

### JSON Parsing Errors in Loki

**Error Message:**
```
pipeline error: 'JSONParserErr' for series: '{__error__="JSONParserErr", __error_details__="Value looks like object, but can't find closing '}' symbol"
```

**Cause:**
- Malformed JSON in log messages
- Multi-line JSON in Loki appender pattern
- Unescaped quotes in log messages

**Solutions:**

1. **Check logback-spring.xml format** - Ensure the Loki appender message pattern is a single line:
   ```xml
   <message>
     <pattern>{"timestamp":"%d{ISO8601}","level":"%level","message":"%replace(%msg){'\"','\\\\\"'}"}</pattern>
   </message>
   ```

2. **Use error filters in Loki queries** - Add `| __error__=""` to skip parsing errors:
   ```logql
   {service="inventory"} | json | __error__=""
   ```

3. **Restart the application** after changing logback configuration:
   ```bash
   mvn spring-boot:run
   # or restart your Docker container
   ```

4. **Clear old logs** from Loki if needed:
   ```bash
   # Stop Loki
   # Delete Loki data directory
   rm -rf /tmp/loki/*
   # Restart Loki
   ```

### No Data in Dashboards

**Prometheus Metrics:**
1. Check application is running: `curl http://localhost:8082/actuator/health`
2. Check metrics endpoint: `curl http://localhost:8082/actuator/prometheus`
3. Verify Prometheus is scraping: Check Prometheus UI at `http://localhost:9090/targets`
4. Check data source in Grafana: Settings → Data Sources → Prometheus → Test

**Loki Logs:**
1. Check Loki is running: `curl http://localhost:3100/ready`
2. Check logs are being sent: Look for Loki errors in application logs
3. Query Loki directly:
   ```bash
   curl -G -s "http://localhost:3100/loki/api/v1/query" \
     --data-urlencode 'query={service="inventory"}' | jq
   ```
4. Check data source in Grafana: Settings → Data Sources → Loki → Test

**Tempo Traces:**
1. Check Tempo is running: `curl http://localhost:3100/ready`
2. Check OTLP endpoint: `curl http://localhost:4318/v1/traces`
3. Verify tracing is enabled in application.yml:
   ```yaml
   management:
     tracing:
       sampling:
         probability: 1.0
   ```
4. Check data source in Grafana: Settings → Data Sources → Tempo → Test

### Dashboard Variables Not Working

**Issue:** Data source variables show "No options found"

**Solution:**
1. Go to Dashboard Settings → Variables
2. For each data source variable:
   - Ensure the type matches your setup (prometheus, loki, tempo)
   - Click "Update" to refresh
3. Save the dashboard

### High Memory Usage in Application

**Cause:** LogstashEncoder with `includeCallerData` and `includeContext` enabled

**Solution:** Disable expensive features in logback-spring.xml:
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <includeContext>false</includeContext>
  <includeCallerData>false</includeCallerData>
  <includeTags>false</includeTags>
</encoder>
```

### Slow Dashboard Loading

**Solutions:**
1. Reduce time range (e.g., from 24h to 1h)
2. Increase query interval in panel settings
3. Reduce number of panels per dashboard
4. Use shorter retention periods in Loki/Prometheus

### Log Messages Truncated or Missing

**Loki Configuration:**
Check Loki limits in `/etc/loki/config.yaml`:
```yaml
limits_config:
  max_line_size: 256000  # Increase if needed
  max_query_length: 721h  # ~30 days
```

**Application Configuration:**
Check Loki4j appender settings:
```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
  <batchSize>100</batchSize>
  <batchTimeoutMs>10000</batchTimeoutMs>
</appender>
```

### Traces Not Correlating with Logs

**Issue:** Trace IDs not appearing in logs

**Solution:**
1. Verify MDC is configured in logback pattern:
   ```xml
   <pattern>{"trace_id":"%mdc{trace_id:-}","span_id":"%mdc{span_id:-}"}</pattern>
   ```

2. Check Spring Boot auto-configuration for tracing:
   ```yaml
   management:
     tracing:
       enabled: true
       sampling:
         probability: 1.0
   ```

3. Ensure you're using supported HTTP client libraries (RestTemplate, WebClient, etc.)

### Query Performance Issues

**For Prometheus:**
- Use recording rules for frequently used queries
- Add more specific label filters
- Reduce cardinality of metrics

**For Loki:**
- Always filter by labels first: `{service="inventory", level="ERROR"}`
- Use line filters before JSON parsing: `{service="x"} |= "error" | json`
- Avoid unbounded queries (always set time range)

**For Tempo:**
- Use specific trace ID queries when possible
- Add service filters: `{service.name="inventory"}`
- Limit time range for exploratory queries

## Testing Your Setup

### 1. Generate Test Traffic
```bash
# Health check
curl http://localhost:8082/actuator/health

# Create products
for i in {1..10}; do
  curl -X POST http://localhost:8082/api/products \
    -H "Content-Type: application/json" \
    -d '{
      "name": "Test Product '$i'",
      "description": "Test description",
      "price": 99.99,
      "category": "test"
    }'
done

# Get products
curl http://localhost:8082/api/products
```

### 2. Verify Metrics
```bash
curl http://localhost:8082/actuator/prometheus | grep http_server_requests
```

### 3. Verify Logs in Loki
```bash
curl -G -s "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query={service="inventory"} | json | __error__=""' \
  --data-urlencode 'limit=10' | jq '.data.result'
```

### 4. Check Traces in Tempo
```bash
# List services
curl -s http://localhost:3100/api/search/tags | jq

# Search for traces
curl -G -s "http://localhost:3100/api/search" \
  --data-urlencode 'tags=service.name="inventory"' | jq
```

## Getting Help

If you continue experiencing issues:

1. Check application logs for errors
2. Enable debug logging:
   ```yaml
   logging:
     level:
       com.github.loki4j: DEBUG
       io.micrometer: DEBUG
   ```
3. Verify all services are running (app, Prometheus, Loki, Tempo, Grafana)
4. Check network connectivity between services
5. Review Grafana server logs: `/var/log/grafana/grafana.log`

## Useful Commands

```bash
# Check all services status
curl http://localhost:8082/actuator/health  # Application
curl http://localhost:9090/-/healthy        # Prometheus
curl http://localhost:3100/ready            # Loki/Tempo
curl http://localhost:3000/api/health       # Grafana

# Tail application logs
tail -f logs/application.log

# Check Loki labels
curl -s http://localhost:3100/loki/api/v1/labels | jq

# Check Prometheus targets
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job, health, lastError}'
```
