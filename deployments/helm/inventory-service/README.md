# Paklog Inventory Service Helm Chart

This Helm chart deploys the Paklog Inventory Management Service on Kubernetes with production-ready configurations including Gateway API support.

## Prerequisites

- Kubernetes 1.26+
- Helm 3.12+
- [Gateway API](https://gateway-api.sigs.k8s.io/) v1.0+ CRDs installed
- MongoDB 6.0+
- Apache Kafka 3.0+
- Redis 7.0+ (optional, for L2 caching)

## Installation

```bash
# Add Helm repository (if published)
helm repo add paklog https://charts.paklog.com

# Install with default values
helm install inventory-service paklog/inventory-service -n paklog

# Install with custom values
helm install inventory-service paklog/inventory-service -n paklog -f custom-values.yaml

# Install from local directory
helm install inventory-service ./deployments/helm/inventory-service -n paklog
```

## Gateway API Support

This chart uses Kubernetes Gateway API (SIG-based) for routing instead of Ingress. The HTTPRoute is configured based on the OpenAPI specification.

### Configured Routes

| Path Pattern | Method | Description | Timeout |
|-------------|--------|-------------|---------|
| `/fulfillment/inventory/v1/stock_levels/{sku}` | GET | Query stock level | 10s |
| `/fulfillment/inventory/v1/stock_levels/{sku}` | PATCH | Adjust stock level | 30s |
| `/fulfillment/inventory/v1/stock_levels/{sku}/reservations` | POST | Create reservation | 15s |
| `/fulfillment/inventory/v1/allocations/bulk` | POST | Bulk allocation | 120s |
| `/fulfillment/inventory/v1/inventory_health_metrics` | GET | Health metrics | 60s |

### Gateway Configuration

```yaml
gateway:
  enabled: true
  hostnames:
    - "api.paklog.com"
  parentRefs:
    - name: paklog-gateway
      namespace: paklog-system
      sectionName: https
```

## Configuration

### Key Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of pod replicas | `3` |
| `image.repository` | Container image repository | `paklog/inventory-service` |
| `image.tag` | Image tag | Chart appVersion |
| `config.serverPort` | Application port | `8085` |
| `config.mongodb.uri` | MongoDB connection URI | `mongodb://mongodb:27017/inventorydb` |
| `config.kafka.bootstrapServers` | Kafka brokers | `kafka:9092` |
| `config.redis.host` | Redis host | `redis` |
| `autoscaling.enabled` | Enable HPA | `true` |
| `autoscaling.minReplicas` | Minimum replicas | `3` |
| `autoscaling.maxReplicas` | Maximum replicas | `20` |

### Example Custom Values

```yaml
# production-values.yaml
replicaCount: 5

image:
  repository: ghcr.io/paklog/inventory-service
  tag: "v1.2.0"

config:
  springProfiles: "production,cloud"
  mongodb:
    uri: "mongodb://mongo-svc:27017/inventorydb"
    poolMaxSize: 200
  kafka:
    bootstrapServers: "kafka-broker-1:9092,kafka-broker-2:9092,kafka-broker-3:9092"
  otel:
    enabled: true
    endpoint: "http://otel-collector.observability:4318/v1/traces"
    samplingProbability: 0.01  # 1% sampling in production

resources:
  limits:
    cpu: 4000m
    memory: 4Gi
  requests:
    cpu: 1000m
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 5
  maxReplicas: 50
  targetCPUUtilizationPercentage: 60

secrets:
  mongodb:
    username: "inventory_user"
    password: "secure_password_here"
  redis:
    password: "redis_password_here"
```

## Production Features

### High Availability

- **Pod Anti-Affinity**: Spreads pods across nodes
- **Pod Disruption Budget**: Ensures minimum availability during updates
- **Horizontal Pod Autoscaler**: Scales based on CPU/Memory utilization
- **Rolling Updates**: Zero-downtime deployments

### Security

- **Non-root User**: Runs as UID 1000
- **Read-only Filesystem**: Prevents runtime modifications
- **Network Policies**: Restricts traffic to authorized services
- **Secrets Management**: Supports external secrets operators

### Observability

- **Prometheus Metrics**: ServiceMonitor for metrics scraping
- **OpenTelemetry Tracing**: Distributed tracing support
- **Health Probes**: Liveness, readiness, and startup probes
- **Structured Logging**: JSON logs with correlation IDs

## Upgrading

```bash
# Upgrade with new values
helm upgrade inventory-service paklog/inventory-service -n paklog -f custom-values.yaml

# Rollback if needed
helm rollback inventory-service 1 -n paklog
```

## Uninstalling

```bash
helm uninstall inventory-service -n paklog
```

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -l app.kubernetes.io/name=inventory-service -n paklog
```

### View Logs

```bash
kubectl logs -l app.kubernetes.io/name=inventory-service -n paklog -f --tail=100
```

### Check HTTPRoute

```bash
kubectl get httproute -n paklog
kubectl describe httproute inventory-service -n paklog
```

### Test Connectivity

```bash
# Port forward
kubectl port-forward svc/inventory-service 8085:80 -n paklog

# Test endpoint
curl http://localhost:8085/actuator/health
```

## Architecture

```
                    ┌─────────────────┐
                    │  Gateway API    │
                    │   (HTTPRoute)   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   Service (LB)  │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │   Pod 1   │ │   Pod 2   │ │   Pod 3   │
        │ (Replica) │ │ (Replica) │ │ (Replica) │
        └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
    ┌──────▼──────┐   ┌──────▼──────┐   ┌──────▼──────┐
    │   MongoDB   │   │    Kafka    │   │    Redis    │
    │  (Storage)  │   │  (Events)   │   │   (Cache)   │
    └─────────────┘   └─────────────┘   └─────────────┘
```

## Support

For issues and feature requests, please contact:
- Platform Team: platform@paklog.com
- GitHub Issues: https://github.com/paklog/inventory/issues
