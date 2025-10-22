## Phase 3: Containerization (Docker)

In this phase, you will package your Spring Boot and Python applications into Docker containers for portability and easy deployment. [^11]

### 3.1. Dockerfile for Spring Boot (`synopsi-api`)
Create a `Dockerfile` in the root of your Spring Boot project.  
Use a multi-stage build to create a lean final image.

### 3.2. Dockerfile for Python Worker (`synopsi-worker`)
In your Python project’s root, create a `Dockerfile`.

### 3.3. Build and Test Docker Images
Build your Docker images:

```bash
docker build -t synopsi-api .
docker build -t synopsi-worker .