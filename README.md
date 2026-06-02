# openshift-pipelines-demo

A centralized OpenShift Pipelines demo designed to be used by multiple projects. This demonstrates how to reduce technical toil on individual development teams by providing shared build and deployment infrastructure.

## Projects

| Project                                    | Description                                                              |
| ---                                        | ---                                                                      |
| [api-hello-world](api-hello-world)         | A simple Hello World REST API built with Quarkus                         |
| [openshift-pipelines](openshift-pipelines) | OpenShift Pipelines operator, centralized Tekton tasks, pipeline, and triggers |

## Prerequisites

- OpenShift cluster (latest stable release)
- `oc` CLI authenticated to the cluster with cluster-admin privileges
- A [Quay.io](https://quay.io) account with a repository namespace (default: `quay.io/stephennimmo`)

## Installation

### Step 1 -- Deploy Everything

Apply the kustomization to install the OpenShift Pipelines operator, create the `openshift-pipelines` namespace, and deploy the centralized pipeline tasks, triggers, and RBAC:

```shell
oc apply -k openshift-pipelines/
```

Wait for the operator to install and Tekton components to become ready:

```shell
oc wait --for=condition=Ready tektonconfig/config --timeout=300s -n openshift-pipelines
```

Once ready, re-apply to ensure the Tekton CRD-based resources (Tasks, Pipeline, Triggers) are created:

```shell
oc apply -k openshift-pipelines/
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

## How It Works

1. A **GitHub push event** hits the `central-build-listener` EventListener
2. The **CEL interceptor** filters for push events and the **TriggerBinding** extracts the repo URL, name, commit SHA, and branch
3. The **TriggerTemplate** creates a `PipelineRun` with a shared workspace
4. The **central-build-pipeline** runs the following tasks:
   - `git-clone` -- clones the repository at the specified commit
   - `build-detect-type` -- inspects the source to determine the project type
   - `build-java-maven` or `build-python` -- runs the appropriate build (conditional via `when` expressions)
   - `build-image-push` -- builds a container image with buildah and pushes to Quay.io

## Image Naming

Built images are pushed to:

```
quay.io/stephennimmo/<repo-name>:<commit-sha>
quay.io/stephennimmo/<repo-name>:latest
```

Push1