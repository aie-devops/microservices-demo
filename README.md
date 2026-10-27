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

## Solution branch

This `solution` branch adds the DevOps files written in Lesson 4.16, plus a Bruno collection that tests the running services:

```
microservices-demo/
├── user-service/
│   ├── Dockerfile
│   └── .dockerignore
├── order-service/
│   ├── Dockerfile
│   └── .dockerignore
├── docker-compose.yml
└── api-tests/            <- Bruno collection
```

The starter project students download is tag `v4.16.0`, which has no Docker files. This solution is tag `v4.16.1` - the images are tagged `v4.16.1` to match.

> H2 is an in-memory database: all users and orders are lost whenever a service restarts. That keeps the lesson focused on containers and deployment - a real system would use a persistent database per service.

## Published images

The `solution` branch has a GitHub Actions workflow, `.github/workflows/publish-images.yml`, that builds both services for `linux/amd64` and `linux/arm64` and pushes them to GitHub Container Registry on every push to `solution`:

```
ghcr.io/aie-devops/user-service:v4.16.1
ghcr.io/aie-devops/order-service:v4.16.1
```

These are the fallback images in Lesson 4.16 - students can deploy them to Railway if they can't build or push their own. They are public, so anyone can pull them without logging in:

```bash
docker pull ghcr.io/aie-devops/user-service:v4.16.1
```

## Run it locally with Docker Compose

Run these commands from the project root (the folder with `docker-compose.yml`). You only need Docker Desktop - Java and Maven run inside the build stage of each Dockerfile.

### 1. Build the images and start the services

```bash
docker compose up --detach --build
```

- `docker compose up` creates and starts every service in `docker-compose.yml`, plus a network so the services can reach each other by name.
- `--build` rebuilds both images from their Dockerfiles before starting. Without it, Compose only builds an image the **first** time - after that it reuses the existing image, so changes to the Java code or a Dockerfile would be ignored and you'd be running an old version. With `--build`, what runs always matches the code in the folder. Docker's layer cache keeps unchanged steps fast.
- `--detach` runs the containers in the background and gives you your terminal back. Without it, Compose streams the logs of every container and stops them all when you press `Ctrl+C`. (`-d` is the short form of `--detach`.)

The first build takes a few minutes while Maven downloads dependencies for both services.

### 2. Check the containers are running

```bash
docker compose ps
```

Lists the containers in this Compose project and their status. Expect both services to be `Up`, with `user-service` showing `(healthy)`. Compose starts `order-service` only after the `user-service` healthcheck (`/actuator/health`) passes, because of `depends_on: condition: service_healthy`.

### 3. Read the logs

```bash
docker compose logs user-service
docker compose logs order-service
```

Prints everything a service has written to the console. Look for `Started UserServiceApplication` and `Started OrderServiceApplication`. Add `--follow` to keep watching new log lines (`Ctrl+C` stops watching, not the container).

### 4. Create a user

```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com"}'
```

- `-X POST` sends a POST request, which creates a new user.
- `-H "Content-Type: application/json"` tells the service the body is JSON.
- `-d '...'` is the request body.
- `localhost:8081` is the **host** port mapped to user-service's container port 8080 (`"8081:8080"` in `docker-compose.yml`).

Expected: the new user as JSON, with `"id":1`.

### 5. Create an order for that user

```bash
curl -i -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productName":"Laptop","quantity":1,"totalPrice":1299.99}'
```

- `-i` prints the response status line and headers, so you can see the status code.
- `localhost:8082` reaches order-service. Before saving the order, order-service calls `http://user-service:8080/api/users/1` - using the Compose **service name**, not `localhost` - to check the user exists.

Expected: `HTTP/1.1 201`. That proves the two services are talking to each other.

### 6. Create an order for a user that doesn't exist

```bash
curl -i -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":999,"productName":"Monitor","quantity":1,"totalPrice":299.99}'
```

user-service answers `404`, so order-service rejects the order. Expected: `HTTP/1.1 400` with `{"error":"User not found with ID: 999"}`.

### 7. See what happens when a dependency is down

```bash
docker compose stop user-service
```

Stops only the user-service container. Now repeat the order request from step 5: order-service can't reach user-service, so it returns `HTTP/1.1 503` with `User Service is unavailable at http://user-service:8080`. This is the weakness of **synchronous** calls covered in Lesson 4.15.

```bash
docker compose start user-service
```

Starts the stopped container again. Because H2 is in-memory, the restarted user-service has **no users** - an order for user 1 now returns `400` until you create the user again.

### 8. Stop and clean up

```bash
docker compose down
```

Stops and removes the containers and the network that `up` created. The images stay on your computer, so the next `up` is faster.

## Test with Bruno

The `api-tests/` folder is a [Bruno](https://www.usebruno.com/) collection with 13 requests. Each request checks the status code and the response body, and the requests pass IDs to each other (the order requests use the user ID returned by "Create user John").

| # | Request | Expected |
|---|---|---|
| 1-2 | User / Order service health | `200`, `status` is `UP` |
| 3-4 | Create user John / Jane | `201`, saves the new ID |
| 5-7 | Get all users / user by id / unknown user `999` | `200` / `200` / `404` |
| 8-9 | Create order for John / Jane | `201`, status `PENDING` |
| 10 | Create order for unknown user `999` | `400`, `User not found with ID: 999` |
| 11-13 | Get all orders / order by id / orders for John | `200` |

Start the services first (`docker compose up --detach --build`), then either:

- **Bruno app:** *Open Collection* -> select the `api-tests` folder -> right-click the collection -> *Run*
- **Bruno CLI:**
  ```bash
  cd api-tests
  npx @usebruno/cli run
  ```
  `run` sends every request in order and prints a pass/fail summary.

The emails include a timestamp (`john.{{$timestamp}}@example.com`), so you can run the collection repeatedly without hitting the unique-email rule.

**Testing on Railway:** the service URLs are collection variables in `api-tests/opencollection.yml` (`userServiceUrl`, `orderServiceUrl`, default `http://localhost:8081` and `http://localhost:8082`). Change them to your Railway domains, e.g. `https://user-service-production-xxxx.up.railway.app`, and run the collection again.

The "user-service is down" case (step 7) isn't in the collection, because it needs a container stopped first - test it by hand.
