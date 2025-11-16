# inventory - Helm Deployment

Inventory management service

## Quick Deploy

Deploy this service only:

```bash
cd deployment/helm/inventory
helm dependency update
helm install inventory . -n paklog --create-namespace
```

## Configuration

Edit `values.yaml` to configure:

- Replica count
- Resource limits
- Database connections
- Kafka settings
- Environment variables

## Verify Deployment

```bash
# Check pod status
kubectl get pods -n paklog -l app.kubernetes.io/name=inventory

# Check service
kubectl get svc -n paklog inventory

# View logs
kubectl logs -n paklog -l app.kubernetes.io/name=inventory -f

# Check health
kubectl port-forward -n paklog svc/inventory 8085:8085
curl http://localhost:8085/actuator/health
```

## Update Deployment

```bash
helm upgrade inventory . -n paklog
```

## Uninstall

```bash
helm uninstall inventory -n paklog
```

## Deploy as Part of Platform

To deploy all services together, use the umbrella chart:

```bash
cd ../../../../deployments/helm/paklog-platform
helm dependency update
helm install paklog-platform . -n paklog --create-namespace
```

See [Platform Documentation](../../../../deployments/helm/README.md) for more details.
