## Phase 4: Local Orchestration (Kubernetes)

You’ll use the Kubernetes instance included with Docker Desktop to manage your containers locally.

### 4.1. Enable Kubernetes in Docker Desktop
Go to Docker Desktop settings, navigate to the **Kubernetes** tab, and enable Kubernetes.[^1]  
This will set up a single-node cluster.

### 4.2. Create Kubernetes Manifests
- **Deployment for synopsi-api**:  
  Create `synopsi-api-deployment.yaml` to define how to run your Spring Boot application pod.

- **Service for synopsi-api**:  
  Create `synopsi-api-service.yaml` to expose your Spring Boot application within the cluster and to your local machine using a **NodePort**.

- **CronJob for synopsi-worker**:  
  Create `synopsi-worker-cronjob.yaml` to schedule your Python worker to run periodically (e.g., every hour).[^2]

### 4.3. Deploy to Your Local Cluster
Apply the manifests to your Kubernetes cluster:

```bash
kubectl apply -f synopsi-api-deployment.yaml
kubectl apply -f synopsi-api-service.yaml
kubectl apply -f synopsi-worker-cronjob.yaml