# microservices-demo

Starter project for **Lesson 4.16 - Microservices Architecture Part 2** (AI Engineering Module 4: DevOps).

Two Spring Boot 4 microservices, each with its own in-memory H2 database:

| Service | Endpoints | Database |
|---|---|---|
| `user-service` | `GET /api/users`, `GET /api/users/{id}`, `POST /api/users` | H2 `userdb` |
| `order-service` | `GET /api/orders`, `GET /api/orders/{id}`, `GET /api/orders/user/{userId}`, `POST /api/orders` | H2 `orderdb` |

When an order is created, `order-service` calls `user-service` (`GET /api/users/{id}`) to check the user exists:

- user exists - `201 Created`
- user does not exist - `400 Bad Request`
- `user-service` cannot be reached - `503 Service Unavailable`

Both services expose `GET /actuator/health`.

## Configuration

| Environment variable | Service | Default | Purpose |
|---|---|---|---|
| `SERVER_PORT` | both | `8080` | Port the service listens on |
| `USER_SERVICE_URL` | order-service | `http://localhost:8081` | Base URL of the User Service |

## What you add in Lesson 4.16

The Java code is complete. In the lesson you add the DevOps files:

```
microservices-demo/
├── user-service/
│   ├── Dockerfile        <- you add
│   └── .dockerignore     <- you add
├── order-service/
│   ├── Dockerfile        <- you add
│   └── .dockerignore     <- you add
└── docker-compose.yml    <- you add
```

Then you run everything locally with Docker Compose, push both images to Docker Hub and deploy them to Railway.

> H2 is an in-memory database: all users and orders are lost whenever a service restarts. That keeps the lesson focused on containers and deployment - a real system would use a persistent database per service.
