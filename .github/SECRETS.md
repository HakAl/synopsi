# GitHub Actions Secrets

The workflows in this directory need no repository secrets.

- `api-ci.yml` runs the Java tests and builds the API jar, which it uploads as
  a workflow artifact.
- `workers-ci.yml` runs the Python worker tests.

Neither logs in to a registry or pushes anything. Container images are built
locally by `docker-compose.yml`.

## Why nothing is published from CI

Earlier versions of these workflows logged in to Docker Hub with
`DOCKER_USERNAME` / `DOCKER_PASSWORD` secrets on every push to `main`, pushed
three images, and a `deploy.yml` then committed new image tags back to `main`
with the workflow token. Those secrets were never configured, so every push
failed at the login step, and holding registry credentials in repository
secrets is a standing exfiltration risk for any workflow that runs
third-party actions. The publishing steps and the deploy workflow were
removed rather than conditioned on the secrets. If image publishing is
reintroduced, prefer GitHub's own registry with the short-lived
`GITHUB_TOKEN` (`packages: write`) over a long-lived Docker Hub token, and
keep the publishing job separate from the test job.
