## Phase 5: CI/CD Automation (GitHub Actions)

This final phase will automate the building, testing, and deployment of your application.

### 5.1. Set Up Your GitHub Repository
Push your Spring Boot and Python projects to a single GitHub repository.

### 5.2. Create a GitHub Actions Workflow
In your repository, create a `.github/workflows/ci-cd.yaml` file.  
This workflow will be triggered on every push to the `main` branch. [^21][^22]

### 5.3. Workflow Steps

| Step | Description |
|------|-------------|
| **Checkout Code** | Use the `actions/checkout` action. |
| **Set up JDK and Maven** | To build and test the Spring Boot application. |
| **Set up Python** | To run tests on your Python script. |
| **Build and Test** | &lt;ul&gt;&lt;li&gt;Run Maven tests for the Spring Boot app.&lt;/li&gt;&lt;li&gt;Run unit tests for the Python worker.&lt;/li&gt;&lt;/ul&gt; |
| **Build and Push Docker Images** | &lt;ul&gt;&lt;li&gt;Use the `docker/build-push-action` to build your `synopsi-api` and `synopsi-worker` images. [^23]&lt;/li&gt;&lt;li&gt;Store your Docker Hub credentials as secrets in your GitHub repository. [^21]&lt;/li&gt;&lt;/ul&gt; |
| **Deploy to Local Kubernetes** | Since this is a local setup, the deployment will be a simulation. The GitHub Action will update the image tags in your Kubernetes manifest files and commit them back to the repository. You can then manually run `kubectl apply -f .` on your local machine to apply the updated configurations. For a more advanced setup, you could explore tools like ArgoCD for a GitOps approach, even on a local cluster. [^24] |

[^21]: GitHub Docs – Encrypted secrets  
[^22]: GitHub Docs – Workflow triggers  
[^23]: Docker GitHub Action – build-push-action  
[^24]: ArgoCD Documentation