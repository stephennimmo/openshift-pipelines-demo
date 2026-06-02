# openshift-pipelines

A centralized OpenShift Pipelines (Tekton) setup that provides a single entry point for building and deploying multiple projects. The pipeline auto-detects the project type and runs the appropriate build task. All resources are deployed into the `openshift-pipelines` namespace.

## Directory Structure

```
openshift-pipelines/
├── operator/               # Operator installation (Namespace, OperatorGroup, Subscription)
├── tasks/                  # Custom Tekton Tasks
├── pipeline/               # Central build Pipeline
└── triggers/               # EventListener, TriggerBinding, TriggerTemplate, RBAC
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

| Resource         | Description                                              |
| ---              | ---                                                      |
| `Namespace`      | Creates the `openshift-pipelines` namespace              |
| `OperatorGroup`  | Configures AllNamespaces install mode                    |
| `Subscription`   | Installs the `openshift-pipelines-operator-rh` operator  |

### Tasks

| Task                | Description                                                                 |
| ---                 | ---                                                                         |
| `build-detect-type` | Inspects workspace to determine project type (java-maven, python)           |
| `build-java-maven`  | Builds a Java Maven project using the Maven wrapper                         |
| `build-python`      | Builds a Python project by installing dependencies                          |
| `build-image-push`  | Builds a container image with buildah and pushes to quay.io/stephennimmo    |
| `git-clone`         | Standard ClusterTask shipped with OpenShift Pipelines                       |

### Pipeline

| Pipeline                 | Description                                                              |
| ---                      | ---                                                                      |
| `central-build-pipeline` | Orchestrates git-clone, build-type detection, conditional build, and image push |

### Triggers

| Resource                          | Description                                                    |
| ---                               | ---                                                            |
| `github-push-binding`             | Extracts repo URL, name, commit SHA, and branch from GitHub push events |
| `central-build-trigger-template`  | Creates a PipelineRun with a VolumeClaimTemplate workspace     |
| `central-build-listener`          | EventListener with CEL interceptor filtering GitHub push events |

## Build Type Detection

The `build-detect-type` task inspects the cloned source and determines the project type:

| Marker                                             | Detected Type |
| ---                                                | ---           |
| `pom.xml` and `mvnw` present                      | `java-maven`  |
| `requirements.txt`, `setup.py`, or `pyproject.toml` | `python`     |

The detected type is emitted as a task result, and `when` expressions on the build tasks ensure only the matching build task runs.

## Deployment

```shell
oc apply -k openshift-pipelines/
```

See the [root README](../README.md) for full installation instructions.
