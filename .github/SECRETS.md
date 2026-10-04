# GitHub Actions Secrets

This document describes the secrets required for the Synopsi CI/CD GitHub Actions workflows.

## Required Secrets

### DOCKER_USERNAME
- **Description**: Docker Hub username for publishing container images
- **Used in**:
  - `.github/workflows/api-ci.yml` - Publishes `synopsi-api` image to Docker Hub
  - `.github/workflows/docker-build.yml` - Publishes `synopsi-ingestion` and `synopsi-summarization` images
- **Type**: Personal Docker Hub account username
- **Access**: Only on `push` to `main` branch

### DOCKER_PASSWORD
- **Description**: Docker Hub password or personal access token for authentication
- **Used in**:
  - `.github/workflows/api-ci.yml` - Authenticates Docker Hub push
  - `.github/workflows/docker-build.yml` - Authenticates Docker Hub push
- **Type**: Docker Hub personal access token (recommended) or password
- **Access**: Only on `push` to `main` branch
- **Security**: Automatically masked in workflow logs

## Setup Instructions for Repository Maintainers

### Creating Docker Hub Credentials

1. **Create a Docker Hub Personal Access Token** (Recommended):
   - Log in to [Docker Hub](https://hub.docker.com)
   - Go to **Account Settings** → **Security** → **New Access Token**
   - Create a token with `Read & Write` permissions
   - Copy the token value (this is your `DOCKER_PASSWORD`)

2. **Alternatively, use your Docker Hub password**:
   - Log in to [Docker Hub](https://hub.docker.com)
   - Your username is the `DOCKER_USERNAME`
   - Your password is the `DOCKER_PASSWORD`
   - **Note**: Personal access tokens are more secure and recommended

### Adding Secrets to GitHub Repository

1. Navigate to your GitHub repository
2. Go to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add `DOCKER_USERNAME`:
   - Name: `DOCKER_USERNAME`
   - Value: Your Docker Hub username
   - Click **Add secret**
5. Click **New repository secret** again
6. Add `DOCKER_PASSWORD`:
   - Name: `DOCKER_PASSWORD`
   - Value: Your Docker Hub personal access token (or password)
   - Click **Add secret**

### Verification

Once configured, the secrets will be used automatically by the workflows:

- **api-ci.yml** will push the API Docker image on push to `main`
- **docker-build.yml** will push the worker Docker images on changes to `synopsi-worker/**`
- **deploy.yml** will automatically update Kubernetes manifests after successful image builds

## Security Considerations

- Secrets are never logged or exposed in workflow output
- Secrets are only available to workflows on the repository's default branch
- Only workflows on the `main` branch can push images to Docker Hub
- Pull request workflows cannot access secrets (controlled by GitHub for security)
- Consider rotating your Docker Hub personal access token periodically

## Troubleshooting

### Image Push Fails with "unauthorized: authentication required"
- Verify `DOCKER_USERNAME` and `DOCKER_PASSWORD` are correctly set in repository secrets
- Check that the Docker Hub personal access token hasn't expired
- Ensure your Docker Hub account has push permissions to the image repositories

### Workflow Cannot Access Secrets
- Verify secrets are set in the repository (not just organization level)
- Ensure the workflow is running on the `main` branch (secrets are branch-restricted)
- Check that the secret names exactly match the workflow references (case-sensitive)
