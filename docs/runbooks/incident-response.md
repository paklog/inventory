# Incident Response Runbook

## Overview
This runbook provides procedures for responding to production incidents in the Inventory Service.

---

## Severity Levels

### P0 - Critical (System Down)
- **Response Time:** Immediate
- **Examples:** Complete service outage, data loss, security breach
- **Escalation:** Page on-call engineer immediately

### P1 - High (Major Functionality Impaired)
- **Response Time:** 15 minutes
- **Examples:** CloudEvents not publishing, database connection failures, API errors >50%
- **Escalation:** Notify on-call engineer via Slack

### P2 - Medium (Degraded Performance)
- **Response Time:** 1 hour
- **Examples:** Slow queries, high latency, memory pressure
- **Escalation:** Create ticket, notify team

### P3 - Low (Minor Issues)
- **Response Time:** Next business day
- **Examples:** Non-critical warnings, minor performance degradation
- **Escalation:** Create ticket

---

## Common Incidents

### 1. Service Unresponsive / High Error Rate

#### Symptoms
- Health check failures
- HTTP 500/503 errors
- Request timeouts
- CloudEvents not publishing

#### Diagnosis Steps
```bash
# 1. Check pod status
kubectl get pods -l app=inventory-service

# 2. Check recent logs
kubectl logs -l app=inventory-service --tail=100 --since=10m

# 3. Check resource usage
kubectl top pods -l app=inventory-service

# 4. Check database connectivity
kubectl exec -it deployment/inventory-service -- \
  mongosh $MONGO_URI --eval "db.adminCommand('ping')"
```

#### Resolution Steps
1. **If pod is CrashLooping:**
   ```bash
   # View crash logs
   kubectl logs -l app=inventory-service --previous

   # Check for OOM kills
   kubectl describe pod <pod-name> | grep -i "oom"

   # Increase memory if OOM
   kubectl set resources deployment inventory-service \
     --limits=memory=2Gi --requests=memory=1Gi
   ```

2. **If database connection failed:**
   ```bash
   # Verify MongoDB is running
   kubectl get pods -l app=mongodb

   # Check MongoDB logs
   kubectl logs -l app=mongodb --tail=50

   # Restart MongoDB if needed (with caution!)
   kubectl rollout restart statefulset mongodb
   ```

3. **If application is hanging:**
   ```bash
   # Get thread dump
   kubectl exec -it deployment/inventory-service -- \
     jstack 1 > thread-dump.txt

   # Restart application
   kubectl rollout restart deployment inventory-service
   ```

---

### 2. CloudEvents Not Publishing

#### Symptoms
- Outbox events piling up
- No events in Kafka topic
- Event publish failures in logs

#### Diagnosis Steps
```bash
# 1. Check outbox queue size
kubectl exec -it deployment/inventory-service -- \
  mongosh $MONGO_URI --eval \
  "db.outbox_events.countDocuments({published: false})"

# 2. Check Kafka connectivity
kubectl exec -it deployment/inventory-service -- \
  kafka-console-producer.sh \
  --broker-list kafka:9092 \
  --topic fulfillment.inventory.v1.events

# 3. Check application logs for publisher errors
kubectl logs -l app=inventory-service | grep -i "cloudEvent"
```

#### Resolution Steps
1. **If Kafka is down:**
   ```bash
   # Check Kafka status
   kubectl get pods -l app=kafka

   # Restart Kafka
   kubectl rollout restart deployment kafka

   # Events will auto-publish when Kafka is back
   ```

2. **If schema validation failing:**
   ```bash
   # Check validation errors
   kubectl logs -l app=inventory-service | grep -i "validation"

   # Disable validation temporarily (emergency only)
   # Update application.properties:
   # cloudevents.schema.validation.enabled=false
   ```

3. **If outbox publisher stuck:**
   ```bash
   # Restart application to reset publisher
   kubectl rollout restart deployment inventory-service
   ```

---

### 3. Database Performance Degradation

#### Symptoms
- Query timeouts
- High database CPU/memory
- Slow API responses
- Connection pool exhaustion

#### Diagnosis Steps
```bash
# 1. Check MongoDB metrics
kubectl exec -it mongodb-0 -- \
  mongosh --eval "db.serverStatus()"

# 2. Check for slow queries
kubectl exec -it mongodb-0 -- \
  mongosh --eval "db.setProfilingLevel(1, {slowms: 100})"

# 3. View slow queries
kubectl exec -it mongodb-0 -- \
  mongosh --eval "db.system.profile.find().sort({ts:-1}).limit(10)"

# 4. Check current operations
kubectl exec -it mongodb-0 -- \
  mongosh --eval "db.currentOp()"
```

#### Resolution Steps
1. **Kill long-running queries:**
   ```javascript
   // Connect to MongoDB
   db.currentOp({ "secs_running": { $gte: 30 } })

   // Kill operation
   db.killOp(<opid>)
   ```

2. **Scale database resources:**
   ```bash
   # Increase MongoDB memory
   kubectl set resources statefulset mongodb \
     --limits=memory=4Gi --requests=memory=2Gi
   ```

3. **Add missing indexes:**
   ```javascript
   // Analyze query plan
   db.collection.find({...}).explain("executionStats")

   // Add index if missing
   db.collection.createIndex({ field: 1 })
   ```

4. **Scale application horizontally:**
   ```bash
   # Add more replicas to distribute load
   kubectl scale deployment inventory-service --replicas=5
   ```

---

### 4. Memory Leak / OOM Kills

#### Symptoms
- Pods restarting frequently
- OOMKilled status
- Gradual memory increase
- GC pressure warnings

#### Diagnosis Steps
```bash
# 1. Check pod events for OOM
kubectl describe pod <pod-name> | grep -A 10 "Events"

# 2. Get heap dump before restart
kubectl exec -it <pod-name> -- \
  jmap -dump:live,format=b,file=/tmp/heapdump.hprof 1

# 3. Copy heap dump for analysis
kubectl cp <pod-name>:/tmp/heapdump.hprof ./heapdump.hprof

# 4. Check memory metrics
kubectl top pod <pod-name>
```

#### Resolution Steps
1. **Immediate mitigation:**
   ```bash
   # Increase memory limits
   kubectl set resources deployment inventory-service \
     --limits=memory=3Gi --requests=memory=1.5Gi

   # Restart to clear memory
   kubectl rollout restart deployment inventory-service
   ```

2. **Tune JVM settings:**
   ```yaml
   # Update deployment with JVM args
   env:
   - name: JAVA_OPTS
     value: "-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
   ```

3. **Analyze heap dump:**
   ```bash
   # Use Eclipse MAT or VisualVM
   # Look for:
   # - Large collections not being cleared
   # - Connection leaks
   # - Cache growing unbounded
   ```

---

### 5. Data Consistency Issues

#### Symptoms
- Incorrect stock levels
- Duplicate records
- Missing data
- Audit trail gaps

#### Diagnosis Steps
```bash
# 1. Check for duplicate SKUs
kubectl exec -it mongodb-0 -- mongosh --eval \
  "db.product_stocks.aggregate([
    {$group: {_id: '$sku', count: {$sum: 1}}},
    {$match: {count: {$gt: 1}}}
  ])"

# 2. Verify ledger consistency
kubectl exec -it mongodb-0 -- mongosh --eval \
  "db.inventory_ledger.find({sku: 'SKU-12345'}).sort({timestamp: -1})"

# 3. Check for failed transactions
kubectl logs -l app=inventory-service | grep -i "OptimisticLockingFailureException"
```

#### Resolution Steps
1. **For stock level discrepancies:**
   ```javascript
   // Recalculate stock from ledger
   const ledger = db.inventory_ledger.aggregate([
     { $match: { sku: "SKU-12345" } },
     { $group: { _id: "$sku", total: { $sum: "$quantityChange" } } }
   ])

   // Update product stock
   db.product_stocks.updateOne(
     { sku: "SKU-12345" },
     { $set: { quantityOnHand: ledger.total } }
   )
   ```

2. **For duplicate records:**
   ```javascript
   // Keep latest, delete duplicates
   db.product_stocks.aggregate([
     { $group: { _id: "$sku", docs: { $push: "$$ROOT" } } },
     { $match: { "docs.1": { $exists: true } } }
   ]).forEach(doc => {
     const keep = doc.docs.sort((a,b) => b.lastUpdated - a.lastUpdated)[0]
     doc.docs.forEach(d => {
       if (d._id != keep._id) {
         db.product_stocks.deleteOne({ _id: d._id })
       }
     })
   })
   ```

3. **Restore from backup if needed:**
   ```bash
   # See database-operations.md for restore procedure
   ```

---

## Communication Templates

### P0 Incident - Initial Alert
```
🚨 P0 INCIDENT - Inventory Service Down

Status: Investigating
Impact: Complete service outage
Started: 2025-10-05 14:30 UTC
Team: On-call engineer notified

Next update in 15 minutes.
```

### Status Update
```
📊 UPDATE - Inventory Service Incident

Status: Identified root cause
Root Cause: MongoDB connection pool exhaustion
Action: Increasing pool size and restarting service
ETA: 5 minutes

Next update in 10 minutes.
```

### Resolution
```
✅ RESOLVED - Inventory Service Incident

Status: Resolved
Root Cause: MongoDB connection pool exhaustion due to query spike
Resolution: Increased pool size from 100 to 200, added missing index
Duration: 45 minutes
Impact: ~5000 failed requests

Post-mortem scheduled for tomorrow 10am.
```

---

## Post-Incident Review

### Within 24 Hours
1. Create incident timeline
2. Document root cause
3. List action items
4. Schedule post-mortem

### Post-Mortem Agenda
1. What happened?
2. What was the impact?
3. What went well?
4. What went poorly?
5. What are we doing to prevent this?

### Action Items Template
```markdown
## Action Items

- [ ] Add monitoring for connection pool exhaustion
- [ ] Increase default pool size in all environments
- [ ] Add circuit breaker for database calls
- [ ] Update runbook with new procedure
- [ ] Schedule training on database tuning
```

---

## Escalation Paths

### L1 - On-Call Engineer
- Initial response
- Follow runbooks
- Escalate if unresolved in 30 min

### L2 - Senior Engineer
- Complex troubleshooting
- Code changes
- Escalate if architecture change needed

### L3 - Tech Lead / Architect
- Architecture decisions
- Major changes
- Coordinate with other teams

### L4 - VP Engineering
- Business impact decisions
- Customer communication
- Resource allocation

---

## Emergency Contacts

```
On-Call Engineer: PagerDuty rotation
Tech Lead: @tech-lead-slack
Database DBA: @dba-slack
Platform Team: @platform-team-slack
Security Team: @security-team-slack
```

---

## Quick Reference Commands

```bash
# View pods
kubectl get pods -l app=inventory-service

# View logs
kubectl logs -f -l app=inventory-service

# Restart service
kubectl rollout restart deployment inventory-service

# Scale service
kubectl scale deployment inventory-service --replicas=5

# Check database
kubectl exec -it mongodb-0 -- mongosh

# View metrics
kubectl top pods

# Get thread dump
kubectl exec -it <pod> -- jstack 1

# Get heap dump
kubectl exec -it <pod> -- jmap -dump:live,format=b,file=/tmp/heap.hprof 1
```
