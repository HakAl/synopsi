# Synopsi Kubernetes Deployment Guide

## Overview

This directory contains Kubernetes manifests for deploying the Synopsi system locally or to a Kubernetes cluster. The deployment consists of:

- **synopsi-api**: Spring Boot REST API (Deployment + Service)
- **synopsi-ingestion**: Python worker for RSS feed processing (CronJob)
- **synopsi-summarization**: Python worker for article summarization (CronJob)
- **Configuration**: ConfigMap and Secrets for API connectivity

## Files

| File | Purpose |
|------|---------|
| `synopsi-config.yaml` | ConfigMap (API_BASE_URL) and Secret (API credentials, JWT secret) |
| `synopsi-api-deployment.yaml` | Spring Boot API Deployment with health checks |
| `synopsi-api-service.yaml` | NodePort Service exposing API on port 30080 |
| `ingestion-cronjob.yml` | CronJob running every 30 minutes for feed ingestion |
| `summarization-cronjob.yml` | CronJob running every 10 minutes for article summarization |
| `deploy-local.sh` | Bash script to validate and deploy all resources |

## Prerequisites

1. **Kubernetes Cluster**: A local or remote cluster must be running
   - **Option 1**: Start [minikube](https://minikube.sigs.k8s.io/)
     ```bash
     minikube start
     eval $(minikube docker-env)  # Load minikube's Docker daemon
     ```
   - **Option 2**: Enable Kubernetes in Docker Desktop (Settings > Kubernetes)
   - **Option 3**: Use an existing cluster and switch context

2. **kubectl**: Must be installed and configured
   ```bash
   kubectl version --client
   kubectl cluster-info
   ```

3. **Docker Images**: Build and load container images into the cluster
   ```bash
   # From project root
   docker build -t synopsi-api:latest -f synopsi-api/Dockerfile .
   docker build -t synopsi-ingestion:latest -f synopsi-worker/Dockerfile.ingestion .
   docker build -t synopsi-summarization:latest -f synopsi-worker/Dockerfile.summarization .

   # If using minikube, load images into cluster
   minikube image load synopsi-api:latest
   minikube image load synopsi-ingestion:latest
   minikube image load synopsi-summarization:latest
   ```

## Deployment

### Automated Deployment (Recommended)

Run the deploy script to validate and apply all manifests:

```bash
cd kubernetes
chmod +x deploy-local.sh
./deploy-local.sh
```

The script performs:
1. ✓ Validates kubectl is installed
2. ✓ Checks cluster connectivity
3. ✓ Validates YAML syntax of all manifests
4. ✓ Applies manifests in correct order (ConfigMap → Deployment → Service → CronJobs)
5. ✓ Waits for API pod to reach Ready state (timeout: 300s)
6. ✓ Verifies Service is accessible on NodePort 30080
7. ✓ Displays deployment summary

### Manual Deployment

Apply resources in order:

```bash
# 1. Create ConfigMap and Secrets (dependencies)
kubectl apply -f synopsi-config.yaml

# 2. Deploy API
kubectl apply -f synopsi-api-deployment.yaml
kubectl apply -f synopsi-api-service.yaml

# 3. Deploy CronJobs (workers)
kubectl apply -f ingestion-cronjob.yml
kubectl apply -f summarization-cronjob.yml

# 4. Verify deployment
kubectl get all -l app=synopsi-api
kubectl get cronjobs
```

## Verification

### Check Deployment Status

```bash
# View API deployment
kubectl get deployment synopsi-api
kubectl describe deployment synopsi-api

# View API pod
kubectl get pods -l app=synopsi-api
kubectl logs -f deployment/synopsi-api

# View API service
kubectl get service synopsi-api
kubectl describe service synopsi-api
```

### Access the API

#### Option 1: kubectl port-forward (Recommended for development)

```bash
kubectl port-forward -n default service/synopsi-api 8080:8080
# Access: http://localhost:8080
```

#### Option 2: NodePort (Direct access)

```bash
# Get NodePort (should be 30080)
kubectl get service synopsi-api -o jsonpath='{.spec.ports[0].nodePort}'

# Get node IP
kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}'

# Access: http://<node-ip>:30080

# For minikube:
minikube service synopsi-api
```

### Verify API Health

```bash
# Via kubectl proxy
kubectl proxy
curl http://localhost:8001/api/v1/namespaces/default/services/synopsi-api:8080/proxy/actuator/health

# Or directly if using port-forward
curl http://localhost:8080/actuator/health
```

### Check CronJobs

```bash
# View all CronJobs
kubectl get cronjobs
kubectl describe cronjob synopsi-ingestion
kubectl describe cronjob synopsi-summarization

# View CronJob execution history
kubectl get jobs
kubectl logs <job-pod-name>
```

## Configuration

### Environment Variables (from synopsi-config.yaml)

```yaml
ConfigMap:
  api-base-url: "http://synopsi-api:8080"

Secrets (base64 encoded):
  api-username: c3lub3BzaQ==  # "synopsi"
  api-password: c3lub3BzaV9wYXNz  # "synopsi_pass"
  jwt-secret: c3lub3BzaV9qd3Rfc2VjcmV0X2tleV9sb2NhbF9kZXZlbG9wbWVudA==  # "synopsi_jwt_secret_key_local_development"
```

### Secrets Decoding

To view decoded secret values:

```bash
# Get base64 value
kubectl get secret synopsi-secrets -o jsonpath='{.data.api-username}' | base64 -d

# Or view all:
kubectl get secret synopsi-secrets -o yaml
```

### Modifying Configuration

To update ConfigMap or Secrets:

```bash
# Edit synopsi-config.yaml and reapply
kubectl apply -f synopsi-config.yaml

# Or edit directly
kubectl edit configmap synopsi-config
kubectl edit secret synopsi-secrets
```

## Scaling

### Horizontal Pod Autoscaling

To scale the API pod based on CPU usage:

```bash
kubectl autoscale deployment synopsi-api --min=2 --max=5 --cpu-percent=80
```

### Manual Scaling

```bash
kubectl scale deployment synopsi-api --replicas=3
```

## Monitoring

### View Real-time Logs

```bash
# API logs
kubectl logs -f deployment/synopsi-api

# Follow logs from all pods
kubectl logs -f -l app=synopsi-api --all-containers=true
```

### Get Pod Events

```bash
kubectl get events --field-selector involvedObject.kind=Pod
```

### Describe Resources

```bash
kubectl describe pod <pod-name>
kubectl describe service synopsi-api
kubectl describe deployment synopsi-api
```

## Cleanup

Remove all Synopsi resources from the cluster:

```bash
# Delete in reverse order
kubectl delete -f synopsi-config.yaml
kubectl delete -f synopsi-api-deployment.yaml
kubectl delete -f synopsi-api-service.yaml
kubectl delete -f ingestion-cronjob.yml
kubectl delete -f summarization-cronjob.yml

# Or delete by label
kubectl delete all -l app=synopsi-api
kubectl delete configmap synopsi-config
kubectl delete secret synopsi-secrets
kubectl delete cronjob synopsi-ingestion synopsi-summarization
```

## Troubleshooting

### Pod Not Reaching Ready State

```bash
# Check pod status
kubectl describe pod -l app=synopsi-api

# Check logs
kubectl logs -l app=synopsi-api --tail=100

# Common issues:
# 1. Image not found: Ensure Docker images are built and loaded
# 2. Port already in use: Check localhost:8080
# 3. Health check failing: Verify API is running and healthy
```

### Service Not Accessible

```bash
# Verify service exists
kubectl get service synopsi-api

# Check endpoints
kubectl get endpoints synopsi-api

# Test connectivity from a pod
kubectl run -it --rm debug --image=busybox --restart=Never -- sh
wget -O- http://synopsi-api:8080/actuator/health
```

### CronJob Not Running

```bash
# Check CronJob schedule
kubectl get cronjobs

# Force manual job run
kubectl create job --from=cronjob/synopsi-ingestion synopsi-ingestion-manual

# Check job execution
kubectl get jobs
kubectl logs <job-pod>
```

## Development Tips

### Watch Resource Changes

```bash
kubectl get all -l app=synopsi-api --watch
```

### Edit and Reload Deployment

```bash
# Edit deployment directly
kubectl edit deployment synopsi-api

# Or update image
kubectl set image deployment/synopsi-api synopsi-api=synopsi-api:v2
```

### Port Forwarding for Frontend

```bash
# Forward both API and frontend
kubectl port-forward service/synopsi-api 8080:8080 &
# Access: http://localhost:8080
```

## Production Considerations

For production deployment:

1. **Use a production-grade cluster** (AWS EKS, GCP GKE, Azure AKS)
2. **Configure persistent storage** for H2 database (currently in-memory)
3. **Set up ingress** instead of NodePort
4. **Use sealed secrets** instead of plaintext base64
5. **Configure resource limits** appropriately
6. **Set up monitoring and logging** (Prometheus, ELK stack)
7. **Use external database** (PostgreSQL) instead of H2
8. **Configure SSL/TLS** for API endpoints
9. **Set up CI/CD** for automated deployments
10. **Configure autoscaling policies**

## See Also

- [CLAUDE.md](../CLAUDE.md) - Project overview and architecture
- [synopsi-api/Dockerfile](../synopsi-api/Dockerfile) - API container image
- [synopsi-worker/Dockerfile.ingestion](../synopsi-worker/Dockerfile.ingestion) - Ingestion worker image
- [synopsi-worker/Dockerfile.summarization](../synopsi-worker/Dockerfile.summarization) - Summarization worker image
