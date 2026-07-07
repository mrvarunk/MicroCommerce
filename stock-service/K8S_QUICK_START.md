# Kubernetes Quick Start Guide

## 1-Minute Deployment

### Prerequisites
```bash
# Ensure Kubernetes cluster is running
kubectl cluster-info

# Ensure Docker is available
docker --version
```

### Deploy (3 commands)

```bash
# 1. Build Docker image
docker build -t stock-service:latest .

# 2. Load image (Minikube only)
minikube image load stock-service:latest

# 3. Deploy to Kubernetes
kubectl apply -f k8s/stock-service-k8s.yaml
```

### Verify

```bash
# Wait for pods to be ready (2-3 minutes)
kubectl get pods -n stock-service -w

# All should show: STATUS = Running
# READY = 1/1 or 2/2 (for deployments with replicas)
```

---

## Test the Service

### Check Health
```bash
curl http://localhost:30081/actuator/health
# Response: {"status":"UP"}
```

### Initialize Stock
```bash
curl -X POST "http://localhost:30081/api/v1/stock/init?productId=1&quantity=100"
# Response: {"productId":1,"quantity":100}
```

### Get Stock
```bash
curl "http://localhost:30081/api/v1/stock/1"
# Response: {"productId":1,"quantity":100}
```

### Decrease Stock
```bash
curl -X POST "http://localhost:30081/api/v1/stock/decrease?productId=1&quantity=10"
# Response: {"productId":1,"quantity":90}
```

### Run Concurrent Test
```bash
# From root project directory
cd ../..
mvn clean test -Dtest=StockConcurrencyIntegrationTest \
  -Dtest.base.url=http://localhost:30081
```

---

## Useful Commands

### Monitoring

```bash
# Watch pods
kubectl get pods -n stock-service -w

# View all resources
kubectl get all -n stock-service

# View pod logs
kubectl logs -f deployment/stock-service-deployment -n stock-service -c stock-service

# View specific pod
kubectl logs <pod-name> -n stock-service

# Describe pod (useful for debugging)
kubectl describe pod <pod-name> -n stock-service
```

### Debugging

```bash
# Connect to pod shell
kubectl exec -it <pod-name> -n stock-service -- /bin/sh

# View environment variables in running pod
kubectl exec <pod-name> -n stock-service -- env | grep -E 'REDIS|MYSQL'

# Check ConfigMap values
kubectl get configmap stock-service-config -n stock-service -o yaml

# Port forward to local machine
kubectl port-forward svc/stock-service 8081:8081 -n stock-service
# Now access: http://localhost:8081
```

---

## Scaling & Management

### Scale Replicas
```bash
# Increase to 3 replicas
kubectl scale deployment stock-service-deployment --replicas=3 -n stock-service

# View replicas
kubectl get deployment stock-service-deployment -n stock-service
```

### Update Configuration
```bash
# Edit ConfigMap
kubectl edit configmap stock-service-config -n stock-service

# Pods automatically pick up changes on restart
kubectl rollout restart deployment/stock-service-deployment -n stock-service
```

### Rolling Update (without downtime)
```bash
# Rebuild and reload image
docker build -t stock-service:latest .
minikube image load stock-service:latest

# Trigger rolling restart
kubectl rollout restart deployment/stock-service-deployment -n stock-service

# Watch progress
kubectl rollout status deployment/stock-service-deployment -n stock-service
```

---

## Cleanup

### Remove Everything
```bash
# Delete all resources in namespace
kubectl delete namespace stock-service

# Confirm deletion
kubectl get namespace stock-service
# Should show: "NotFound"
```

### Remove Only Stock Service (keep Redis & MySQL)
```bash
kubectl delete deployment stock-service-deployment -n stock-service
kubectl delete service stock-service -n stock-service
```

---

## Accessing Remote Kubernetes Cluster

### For AWS EKS / Google GKE / DigitalOcean / etc.

```bash
# Get external load balancer IP
kubectl get svc stock-service -n stock-service

# Use the EXTERNAL-IP (not localhost)
curl http://<EXTERNAL-IP>:8081/actuator/health
curl -X POST "http://<EXTERNAL-IP>:8081/api/v1/stock/init?productId=1&quantity=100"
```

---

## Architecture Diagram

```
┌────────────────────────────────────────────┐
│      Your Kubernetes Cluster               │
│                                            │
│  ┌─────────────────────────────────────┐  │
│  │  stock-service Namespace            │  │
│  │                                     │  │
│  │  2 stock-service Pods (replicas)   │  │
│  │         ↓          ↓                │  │
│  │  [Pod 1: 8081] [Pod 2: 8081]       │  │
│  │         ↓          ↓                │  │
│  │    │ ────────────────── │           │  │
│  │    └─ Redis Pod        │           │  │
│  │    └─ MySQL Pod        │           │  │
│  │                                     │  │
│  │  NodePort Service                  │  │
│  │  Port 8081 → 30081 (external)     │  │
│  └─────────────────────────────────────┘  │
│                  ↑                        │
└──────────────────┼────────────────────────┘
                   │
            External Access
         (localhost:30081 or IP:30081)
```

---

## Troubleshooting Quick Fixes

### Pods not starting?
```bash
kubectl describe pod <pod-name> -n stock-service
# Look for: Events section → Warning messages
```

### Connection errors to MySQL/Redis?
```bash
# Test from inside pod
kubectl exec <pod-name> -n stock-service -- \
  mysql -h mysql-service -u root -proot -e "SELECT 1"

kubectl exec <pod-name> -n stock-service -- \
  redis-cli -h redis-service ping
```

### Out of memory errors?
```bash
# Check resource usage
kubectl top nodes
kubectl top pods -n stock-service

# Increase limits in k8s/stock-service-k8s.yaml
# Update resources → limits → memory
kubectl apply -f k8s/stock-service-k8s.yaml
```

---

## Next Steps

- Read `K8S_MIGRATION.md` for detailed architecture
- Read `QUICKSTART.md` for JUnit test information
- Check `README.md` for full project overview
