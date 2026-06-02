# api-hello-world

A simple Hello World REST API built with Quarkus.

## Prerequisites

- Java 21+
- Maven 3.9+

## Running in Dev Mode

```shell
./mvnw quarkus:dev
```

The API will be available at http://localhost:8080/api/hello

The OpenAPI spec is available at http://localhost:8080/q/openapi and Swagger UI at http://localhost:8080/q/swagger-ui

## Building

```shell
./mvnw package
```

## Running

```shell
java -jar target/quarkus-app/quarkus-run.jar
```

## API

| Method | Path         | Description                  |
| ---    | ---          | ---                          |
| GET    | `/api/hello` | Returns a hello world message |
