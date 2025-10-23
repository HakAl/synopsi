```
synopsi/
├── .github/
│   └── workflows/
│       └── ci-cd.yml         # GitHub Actions workflow for CI/CD
│
├── synopsi-api/              # Spring Boot Application (The "API")
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── io/github/yourusername/synopsi/
│   │   │   │       ├── controller/
│   │   │   │       │   └── ArticleController.java
│   │   │   │       ├── service/
│   │   │   │       │   └── ArticleService.java
│   │   │   │       ├── repository/
│   │   │   │       │   └── ArticleRepository.java
│   │   │   │       ├── model/
│   │   │   │       │   └── Article.java
│   │   │   │       └── SynopsiApplication.java
│   │   │   └── resources/
│   │   │       ├── static/         # For your CSS, JS, and images
│   │   │       │   └── dashboard.js
│   │   │       ├── templates/      # For your HTML files
│   │   │       │   └── index.html
│   │   │       └── application.properties
│   │   └── test/
│   │       └── ...
│   ├── pom.xml                 # Or build.gradle for Gradle
│   └── Dockerfile              # Dockerfile specifically for the Spring Boot app
│
├── synopsi-worker/           # Python NLP Application (The "Worker")
│   ├── nlp/
│   │   ├── __init__.py
│   │   ├── fetcher.py          # Logic for fetching from RSS/websites
│   │   ├── summarizer.py       # Logic for loading the model and summarizing
│   │   └── api_client.py       # Logic to send data to the Spring Boot API
│   ├── notebooks/
│   │   └── prototype.ipynb     # Jupyter Notebook for experimentation
│   ├── tests/
│   │   └── test_summarizer.py
│   ├── worker.py               # Main script, entry point for the worker
│   ├── requirements.txt        # Python dependencies
│   └── Dockerfile              # Dockerfile specifically for the Python app
│
├── kubernetes/               # All Kubernetes manifests in one place
│   ├── synopsi-api-deployment.yaml
│   ├── synopsi-api-service.yaml
│   └── synopsi-worker-cronjob.yaml
│
├── docs/                     # Project documentation
│   ├── architecture.md
│   └── api_contract.json     # Example of API specifications
│
├── .gitignore                # Git ignore file for both Java and Python
├── LICENSE
└── README.md                 # Main project README
```