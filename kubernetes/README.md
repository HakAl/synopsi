# Synopsi Kubernetes Deployment

Quick reference for deploying Synopsi to a Kubernetes cluster.

## Quick Start

### 1. Start Kubernetes Cluster

```bash
# Option A: minikube
minikube start
eval $(minikube docker-env)

# Option B: Docker Desktop
# Enable Kubernetes in Docker Desktop settings
```

### 2. Build Docker Images

```bash
cd synopsi  # project root
docker build -t synopsi-api:latest -f synopsi-api/Dockerfile .
docker build -t synopsi-ingestion:latest -f synopsi-worker/Dockerfile.ingestion .
docker build -t synopsi-summarization:latest -f synopsi-worker/Dockerfile.summarization .

# If using minikube, load images:
minikube image load synopsi-api:latest
minikube image load synopsi-ingestion:latest
minikube image load synopsi-summarization:latest
```

### 3. Deploy to Kubernetes

```bash
cd kubernetes
chmod +x deploy-local.sh
./deploy-local.sh
```

### 4. Access the API

```bash
# Port forward (recommended for development)
kubectl port-forward svc/synopsi-api 8080:8080

# Then access: http://localhost:8080
```

## Files

| File | Purpose |
|------|---------|
| `deploy-local.sh` | Automated deployment script |
| `synopsi-config.yaml` | ConfigMap and Secrets |
| `synopsi-api-deployment.yaml` | API Deployment |
| `synopsi-api-service.yaml` | API Service |
| `ingestion-cronjob.yml` | Ingestion worker CronJob |
| `summarization-cronjob.yml` | Summarization worker CronJob |
| `DEPLOYMENT.md` | Comprehensive guide |
| `VERIFICATION.md` | Verification details |

## Common Commands

```bash
# Check deployment status
kubectl get all -l app=synopsi-api

# View API logs
kubectl logs -f deployment/synopsi-api

# Access API via port-forward
kubectl port-forward svc/synopsi-api 8080:8080

# Check pod status
kubectl describe pod -l app=synopsi-api

# View CronJobs
kubectl get cronjobs
kubectl describe cronjob synopsi-ingestion

# Delete all resources
kubectl delete all,configmap,secret -l app=synopsi-api
```

## Troubleshooting

### Pod not starting

```bash
# Check pod events
kubectl describe pod -l app=synopsi-api

# View logs
kubectl logs -l app=synopsi-api

# Common fixes:
# 1. Ensure Docker images are built and loaded
# 2. Check cluster connectivity: kubectl cluster-info
```

### Cannot access API

```bash
# Check service
kubectl get service synopsi-api

# Use port-forward instead of NodePort
kubectl port-forward svc/synopsi-api 8080:8080
```

### CronJob not running

```bash
# Check if CronJob exists
kubectl get cronjob synopsi-ingestion

# Check job history
kubectl get jobs

# Check pod logs
kubectl logs <job-pod-name>
```

## Documentation

- **DEPLOYMENT.md** - Full deployment guide with all options
- **VERIFICATION.md** - Verification checklist and deployment process
- **deploy-local.sh** - Bash script for automated deployment

## Requirements

- Kubernetes 1.19+ cluster
- kubectl 1.19+
- Docker for building images
- 2 CPU, 1GB RAM minimum

## Architecture

```
┌─────────────────────────────────────────┐
│       Kubernetes Cluster                 │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐  │
│  │    synopsi-api Deployment       │  │
│  │  ┌──────────────────────────┐   │  │
│  │  │  synopsi-api Container   │   │  │
│  │  │  Spring Boot API         │   │  │
│  │  │  Port: 8080              │   │  │
│  │  │  Health: /actuator/health│   │  │
│  │  └──────────────────────────┘   │  │
│  └─────────────────────────────────┘  │
│           ↓ Service: NodePort 30080    │
│                                         │
│  ┌──────────────────────────────────┐ │
│  │   synopsi-config ConfigMap        │ │
│  │   • api-base-url                  │ │
│  └──────────────────────────────────┘ │
│                                         │
│  ┌──────────────────────────────────┐ │
│  │   synopsi-secrets Secret          │ │
│  │   • api-username                  │ │
│  │   • api-password                  │ │
│  │   • jwt-secret                    │ │
│  └──────────────────────────────────┘ │
│                                         │
│  ┌──────────────────────────────────┐ │
│  │  synopsi-ingestion CronJob        │ │
│  │  Schedule: Every 30 minutes       │ │
│  │  RSS feed processing              │ │
│  └──────────────────────────────────┘ │
│                                         │
│  ┌──────────────────────────────────┐ │
│  │ synopsi-summarization CronJob     │ │
│  │  Schedule: Every 10 minutes       │ │
│  │  Article summarization            │ │
│  └──────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

## Environment Variables

**ConfigMap (synopsi-config)**
- `API_BASE_URL`: http://synopsi-api:8080

**Secret (synopsi-secrets)**
- `api-username`: synopsi
- `api-password`: synopsi_pass
- `jwt-secret`: synopsi_jwt_secret_key_local_development

## Next Steps

1. Review `DEPLOYMENT.md` for detailed configuration options
2. Run `./deploy-local.sh` to deploy
3. Verify with `kubectl get all -l app=synopsi-api`
4. Access API at http://localhost:8080 (after port-forward)
5. Check worker CronJobs with `kubectl get cronjobs`

---

For more information, see:
- `DEPLOYMENT.md` - Complete deployment guide
- `VERIFICATION.md` - Verification checklist
- Project root `CLAUDE.md` - System architecture
