# Order Management API

A simple Spring Boot REST API for managing orders.
The application exposes endpoints to create orders, retrieve them, list them,
and perform basic order status transitions.

---

## Tech Stack

- Java 25
- Spring Boot 4.0
- Spring Web (WebMVC)
- Jakarta Validation
- Gradle
- JUnit 5 (integration tests)
- In-memory persistence (ConcurrentHashMap)

---

## Domain Model

An **Order** consists of:
- `id` (UUID)
- `customerEmail`
- `status` (`CREATED`, `PAID`, `CANCELLED`)
- list of order items
- creation timestamp

An **OrderItem** contains:
- product name
- quantity

---

## API Endpoints

### Create order

**POST** `/orders`

### Request body:

{
  "customerEmail": "customer@example.com",
  "items": [
    {
      "productName": "Milk",
      "quantity": 2
    }
  ]
}

### Response:

201 Created

Location header pointing to /orders/{id}

### Get order by id

GET /orders/{id}

### Response:

{
  "id": "b3d9e1d2-9a3e-4f4a-9b27-0f4c7d4b6e8a",
  "customerEmail": "customer@example.com",
  "status": "CREATED",
  "items": [
    {
      "productName": "Milk",
      "quantity": 2
    }
  ],
  "createdAt": "2025-01-01T12:00:00Z"
}

### List orders

GET /orders

### Optional query parameter:

status (CREATED, PAID, CANCELLED)

### Example:

GET /orders?status=CREATED

### Order Status Transitions

Orders start in status CREATED.

Pay order

POST /orders/{id}/pay

### Allowed transition:

CREATED → PAID

### Cancel order

POST /orders/{id}/cancel

### Allowed transition:

CREATED → CANCELLED

### Invalid transitions

Any other transition (for example PAID → CANCELLED) results in:

### HTTP 400 Bad Request

JSON error response

{
  "status": 400,
  "message": "Invalid status transition: PAID -> CANCELLED"
}

### Error Handling

The API returns errors in a consistent JSON format:

{
  "status": 404,
  "message": "Order {id} not found"
}

---

## Handled scenarios:

Order not found → 404

Validation errors → 400

Invalid status transitions → 400

### Running the Application

Build
./gradlew clean build

Run
./gradlew bootRun

---

## Application will be available at:

http://localhost:8080

---

## Running Tests

The project contains integration tests covering:

Order creation

Order retrieval

Status transitions

Error handling

### Run tests with:

./gradlew test

---

## Notes

Persistence is currently in-memory for simplicity.

No authentication or security mechanisms are implemented.

The project follows a simple layered architecture:
Controller → Service → Store

A PostgreSQL + Testcontainers persistence variant is planned on a separate branch.