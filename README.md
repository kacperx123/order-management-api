
# Order & Inventory Management API

A production-style Order & Inventory Management System built with Spring Boot.
The project demonstrates transactional consistency, concurrency handling, and event-driven architecture using Kafka with the Outbox Pattern.

This is not a demo CRUD application — it models real-world order placement, inventory control, and asynchronous integrations.

## ✨ Key Features

Product and inventory management

Order lifecycle with strict status transitions

Inventory consistency with optimistic locking

Transactional domain events with Outbox Pattern

Asynchronous event publishing via Kafka

Clean error handling and integration tests with Testcontainers

## 🛠️ Tech Stack

Java 25

Spring Boot 4

Spring Web (REST API)

Spring Data JPA (Hibernate)

PostgreSQL

Kafka (producer + consumer)

Jackson (manual serialization for Outbox)

Docker & Docker Compose

JUnit 5

Testcontainers

## 📦 Domain Model

### Product

Represents a sellable product.

id (UUID)

name

price

active

### Inventory

Represents stock for a product (separate aggregate).

id (UUID)

productId

available

reserved

version (optimistic locking)

### Order

Represents a customer order.

id (UUID)

customerEmail

status (CREATED, PAID, CANCELLED)

createdAt

items

### OrderItem

Snapshot of product data at purchase time.

productId

productNameSnapshot

unitPriceAtPurchase

quantity

## 🔄 Order Flow (Business Logic)
### 1. Create Product

POST /products

Creates Product

Creates corresponding Inventory

Emits:

ProductCreatedEvent

StockAdjustedEvent

### 2. Place Order

POST /orders

Transactional flow:

Validate product existence and active status

Load inventory

Verify sufficient stock

Decrease inventory.available

Create Order + OrderItem (with price snapshot)

Persist everything in one transaction

Persist OrderCreatedEvent in the Outbox

If any step fails → transaction is rolled back.

### 3. Pay Order

POST /orders/{id}/pay

Allowed transition:

CREATED → PAID


Updates order status

Persists OrderPaidEvent in Outbox

### 4. Cancel Order

POST /orders/{id}/cancel

Allowed transition:

CREATED → CANCELLED


Updates order status

Persists OrderCancelledEvent in Outbox

## ⚠️ Error Handling

The API returns consistent JSON error responses:

{
  "status": 409,
  "message": "Out of stock for product ..."
}


### Handled scenarios:

Scenario	HTTP
Resource not found	404
Validation error	400
Invalid order status transition	400
Out of stock / inactive product	409
Optimistic locking conflict	409
🔐 Concurrency & Consistency
Optimistic Locking

Inventory updates use @Version to prevent race conditions.

### Scenario:
Two users try to buy the last item.

Result:

One order succeeds

One order fails with 409 Conflict

This behavior is verified with concurrent integration tests.

## 📣 Domain Events & Outbox Pattern

### Why Outbox?

Publishing events directly to Kafka inside a transaction is unsafe.

This project uses the Outbox Pattern:

Domain events are persisted to the outbox_events table

Persistence happens in the same transaction as business data

A background job publishes events to Kafka

Events are marked as published after successful send

This guarantees no lost events.

### 🧾 Outbox Event Structure

id

aggregateType (ORDER, PRODUCT)

aggregateId

type (OrderCreated, OrderPaid, etc.)

payloadJson

occurredAt

publishedAt

### 🛰️ Kafka Integration
Topics

order-events

product-events

Producer

Reads unpublished outbox events

Publishes JSON payloads to Kafka

Uses aggregateId as message key

Consumer

A demo Kafka consumer logs received events and represents downstream systems such as:

billing

shipping

analytics

Kafka infrastructure is disabled in tests to keep them fast and deterministic.

### 🧪 Testing Strategy

Integration tests only

No in-memory databases

PostgreSQL via Testcontainers

Kafka disabled in test context

Outbox behavior verified directly via database assertions

## 🐳 Running the Application

### Start infrastructure
docker compose up -d

### Run the app
./gradlew bootRun

### 🧪 Run Tests
./gradlew test

