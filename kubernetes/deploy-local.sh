#!/bin/bash

# Synopsi Local Kubernetes Deployment Script
# Deploys all Kubernetes resources to a local cluster in the correct order
# Usage: ./deploy-local.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
NAMESPACE="${KUBE_NAMESPACE:-default}"
TIMEOUT="${KUBE_TIMEOUT:-300}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate YAML files
validate_manifests() {
    log_info "Validating YAML manifests..."

    local manifests=(
        "$SCRIPT_DIR/synopsi-config.yaml"
        "$SCRIPT_DIR/synopsi-api-deployment.yaml"
        "$SCRIPT_DIR/synopsi-api-service.yaml"
        "$SCRIPT_DIR/ingestion-cronjob.yml"
        "$SCRIPT_DIR/summarization-cronjob.yml"
    )

    for manifest in "${manifests[@]}"; do
        if [[ ! -f "$manifest" ]]; then
            log_error "Manifest not found: $manifest"
            return 1
        fi

        log_info "Validating $manifest..."
        if ! kubectl apply -f "$manifest" --dry-run=client --namespace "$NAMESPACE" > /dev/null 2>&1; then
            log_error "Invalid YAML in $manifest"
            kubectl apply -f "$manifest" --dry-run=client --namespace "$NAMESPACE"
            return 1
        fi
        log_info "✓ $manifest is valid"
    done

    return 0
}

# Check if kubectl is installed
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        return 1
    fi
    log_info "✓ kubectl is available"
    return 0
}

# Check if cluster is accessible
check_cluster() {
    log_info "Checking cluster connectivity..."
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        log_warn ""
        log_warn "To use this script, you need a running Kubernetes cluster."
        log_warn "Options:"
        log_warn "  1. Start minikube: minikube start"
        log_warn "  2. Use Docker Desktop Kubernetes: Enable in Docker Desktop settings"
        log_warn "  3. Use an existing cluster: kubectl config use-context <context>"
        log_warn ""
        return 1
    fi
    log_info "✓ Connected to cluster"
    return 0
}

# Apply manifests in correct order
apply_manifests() {
    log_info "Applying Kubernetes manifests..."

    # Step 1: Apply ConfigMap and Secrets first (dependencies)
    log_info "Applying ConfigMap and Secrets..."
    kubectl apply -f "$SCRIPT_DIR/synopsi-config.yaml" --namespace "$NAMESPACE"
    log_info "✓ ConfigMap and Secrets applied"

    # Step 2: Apply API Deployment and Service
    log_info "Applying synopsi-api Deployment..."
    kubectl apply -f "$SCRIPT_DIR/synopsi-api-deployment.yaml" --namespace "$NAMESPACE"
    log_info "✓ synopsi-api Deployment applied"

    log_info "Applying synopsi-api Service..."
    kubectl apply -f "$SCRIPT_DIR/synopsi-api-service.yaml" --namespace "$NAMESPACE"
    log_info "✓ synopsi-api Service applied"

    # Step 3: Apply CronJobs
    log_info "Applying ingestion CronJob..."
    kubectl apply -f "$SCRIPT_DIR/ingestion-cronjob.yml" --namespace "$NAMESPACE"
    log_info "✓ Ingestion CronJob applied"

    log_info "Applying summarization CronJob..."
    kubectl apply -f "$SCRIPT_DIR/summarization-cronjob.yml" --namespace "$NAMESPACE"
    log_info "✓ Summarization CronJob applied"

    return 0
}

# Wait for API pod to be ready
wait_for_api_ready() {
    log_info "Waiting for synopsi-api pod to reach Ready state (timeout: ${TIMEOUT}s)..."

    if kubectl wait --for=condition=Ready pod \
        -l app=synopsi-api \
        --namespace "$NAMESPACE" \
        --timeout="${TIMEOUT}s" 2>/dev/null; then
        log_info "✓ synopsi-api pod is Ready"
        return 0
    else
        log_error "synopsi-api pod failed to reach Ready state within ${TIMEOUT}s"
        log_warn "Current pod status:"
        kubectl get pods -l app=synopsi-api --namespace "$NAMESPACE"
        kubectl describe pods -l app=synopsi-api --namespace "$NAMESPACE"
        return 1
    fi
}

# Verify service is accessible
verify_service() {
    log_info "Verifying synopsi-api Service..."

    local service_info=$(kubectl get svc synopsi-api --namespace "$NAMESPACE" -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")

    if [[ -z "$service_info" ]]; then
        log_error "Service synopsi-api not found or NodePort not assigned"
        return 1
    fi

    log_info "✓ Service synopsi-api is available"
    log_info "  NodePort: $service_info"

    # Get cluster info to find node IP or use localhost for local clusters
    local node_ip=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}' 2>/dev/null || echo "localhost")

    log_info "  Access API at: http://$node_ip:$service_info"
    log_info "  Or via kubectl port-forward: kubectl port-forward -n $NAMESPACE svc/synopsi-api 8080:8080"

    return 0
}

# Display deployment summary
show_summary() {
    log_info "=== Deployment Summary ==="

    echo ""
    log_info "Deployed Resources:"
    kubectl get all -l app=synopsi-api --namespace "$NAMESPACE"

    echo ""
    log_info "ConfigMap and Secrets:"
    kubectl get configmaps,secrets -l app=synopsi-api --namespace "$NAMESPACE" 2>/dev/null || \
        kubectl get configmap synopsi-config secret synopsi-secrets --namespace "$NAMESPACE" 2>/dev/null

    echo ""
    log_info "CronJobs:"
    kubectl get cronjobs --namespace "$NAMESPACE" | grep synopsi

    echo ""
}

# Main execution
main() {
    log_info "Starting Synopsi Kubernetes deployment..."
    echo ""

    check_kubectl || exit 1
    check_cluster || exit 1
    validate_manifests || exit 1

    echo ""
    apply_manifests || exit 1

    echo ""
    wait_for_api_ready || exit 1

    echo ""
    verify_service || exit 1

    echo ""
    show_summary

    log_info "=== Deployment Complete ==="
    log_info "All Synopsi resources are deployed and ready"
}

main "$@"
