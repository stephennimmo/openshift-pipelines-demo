# openshift-pipelines-demo

A centralized OpenShift Pipelines (Tekton) setup that provides a single entry point for building and deploying multiple projects. The pipeline auto-detects the project type and runs the appropriate build task. All resources are deployed into the `openshift-pipelines` namespace.

## Demo Project

| Project | Description |
| --- | --- |
| [quarkus-demo-api](https://github.com/stephennimmo/quarkus-demo-api) | A Quarkus REST API used as the demo project for this pipeline |

## Directory Structure

```
├── operator/       # Operator installation (Namespace, OperatorGroup, Subscription)
├── pipeline/       # Central build Pipeline
├── tasks/          # Custom Tekton Tasks
├── triggers/       # EventListener, TriggerBinding, TriggerTemplate, RBAC
└── kustomization.yaml
```

## Architecture

```
GitHub Webhook --> EventListener --> TriggerBinding --> TriggerTemplate --> PipelineRun
                                                                              |
                                                                              v
                                                                    Central Build Pipeline
                                                                              |
                                                              +---------------+---------------+
                                                              |               |               |
                                                          git-clone    build-detect-type      |
                                                                              |               |
                                                                    +---------+---------+     |
                                                                    |                   |     |
                                                            build-java-maven      build-python|
                                                                    |                   |     |
                                                                    +---------+---------+     |
                                                                              |               |
                                                                      build-image-push--------+
                                                                              |
                                                                              v
                                                                  quay.io/stephennimmo
```

## Components

### Operator

| Resource | Description |
| --- | --- |
| `Namespace` | Creates the `openshift-pipelines` namespace |
| `OperatorGroup` | Configures AllNamespaces install mode |
| `Subscription` | Installs the `openshift-pipelines-operator-rh` operator |

### Tasks

| Task | Description |
| --- | --- |
| `build-detect-type` | Inspects workspace to determine project type (java-maven, python) |
| `build-java-maven` | Builds a Java Maven project using the Maven wrapper |
| `build-python` | Builds a Python project by installing dependencies |
| `build-image-push` | Builds a container image with buildah and pushes to quay.io/stephennimmo |
| `git-clone` | Standard ClusterTask shipped with OpenShift Pipelines |

### Pipeline

| Pipeline | Description |
| --- | --- |
| `central-build-pipeline` | Orchestrates git-clone, build-type detection, conditional build, and image push |

### Triggers

| Resource | Description |
| --- | --- |
| `github-push-binding` | Extracts repo URL, name, commit SHA, and branch from GitHub push events |
| `central-build-trigger-template` | Creates a PipelineRun with a VolumeClaimTemplate workspace |
| `central-build-listener` | EventListener with CEL interceptor filtering GitHub push events |

## Build Type Detection

The `build-detect-type` task inspects the cloned source and determines the project type:

| Marker | Detected Type |
| --- | --- |
| `pom.xml` and `mvnw` present | `java-maven` |
| `requirements.txt`, `setup.py`, or `pyproject.toml` | `python` |

The detected type is emitted as a task result, and `when` expressions on the build tasks ensure only the matching build task runs.

## Prerequisites

- OpenShift cluster (latest stable release)
- `oc` CLI authenticated to the cluster with cluster-admin privileges
- A [Quay.io](https://quay.io) account with a repository namespace (default: `quay.io/stephennimmo`)

## Installation

### Step 1 -- Deploy Everything

Apply the kustomization to install the OpenShift Pipelines operator, create the `openshift-pipelines` namespace, and deploy the centralized pipeline tasks, triggers, and RBAC:

```shell
oc apply -k .
```

Wait for the operator to install and Tekton components to become ready:

```shell
oc wait --for=condition=Ready tektonconfig/config --timeout=300s -n openshift-pipelines
```

Once ready, re-apply to ensure the Tekton CRD-based resources (Tasks, Pipeline, Triggers) are created:

```shell
oc apply -k .
```

### Step 2 -- Get the EventListener Route URL

The Route is created automatically by the kustomization. Retrieve the URL for configuring webhooks:

```shell
oc get route el-central-build-listener -n openshift-pipelines -o jsonpath='{.spec.host}'
```

### Step 3 -- Configure the Quay.io Registry Secret

Create a secret with your Quay.io credentials so that `build-image-push` can push images:

```shell
oc create secret docker-registry quay-auth \
  --docker-server=quay.io \
  --docker-username=<your-quay-username> \
  --docker-password=<your-quay-password> \
  -n openshift-pipelines
```

Link the secret to the pipeline service account:

```shell
oc secrets link pipeline-sa quay-auth --for=mount -n openshift-pipelines
```

### Step 4 -- Configure GitHub Webhooks

For each repository you want the central pipeline to build, add a webhook in the GitHub repository settings:

1. Go to **Settings > Webhooks > Add webhook**
2. Set the following:
   - **Payload URL:** `http://<route-url>` (from Step 2)
   - **Content type:** `application/json`
   - **Secret:** *(optional, recommended for production)*
   - **Events:** Select **Just the push event**
3. Click **Add webhook**

### Step 5 -- Verify

Push a commit to a configured repository and verify the pipeline runs:

```shell
oc get pipelineruns -n openshift-pipelines -w
```

## Image Naming

Built images are pushed to:

```
quay.io/stephennimmo/<repo-name>:<commit-sha>
quay.io/stephennimmo/<repo-name>:latest
```
