# Low-Level Design (LLD) Guide

## Table of Contents

1. Introduction
2. LLD Objectives
3. Package Structure
4. Class Diagram
5. Object Relationships
6. Class Responsibilities
7. Request Execution Flow
8. Bucket Lifecycle
9. Sequence Diagram
10. SOLID Principles
11. Design Patterns
12. Error Handling
13. Configuration Design
14. Concurrency Design
15. Extensibility
16. Testing Strategy
17. Interview Discussion Points
18. Summary

---

# Introduction

While the **High-Level Design (HLD)** explains the architecture of the system, the **Low-Level Design (LLD)** focuses on how the system is implemented.

It answers questions such as:

- Which classes exist?
- What are their responsibilities?
- How do objects interact?
- Why were specific design patterns chosen?
- How is thread safety achieved?
- How can the implementation be extended?

This document maps directly to the implementation of the rate limiter.

---

# LLD Objectives

The implementation is designed to achieve:

- Clear separation of responsibilities
- High cohesion
- Low coupling
- Thread safety
- Extensibility
- Testability
- Maintainability

Every class owns a single primary responsibility.

---

# Package Structure

A simplified package layout is shown below.

```text
ratelimiter
│
├── config
│   ├── RateLimitProperties
│   ├── RateLimiterConfiguration
│   └── WebMvcConfig
│
├── interceptor
│   └── RateLimitInterceptor
│
├── service
│   ├── RateLimiterService
│   └── TokenBucketRateLimiterService
│
├── bucket
│   ├── Bucket
│   ├── TokenBucket
│   ├── BucketStore
│   └── BucketRefillStrategy
│
├── identifier
│   └── ClientIdentifier
│
├── scheduler
│   └── BucketCleanupScheduler
│
├── exception
│   ├── RateLimitExceededException
│   └── GlobalExceptionHandler
```

Each package represents a logical responsibility.

---

# Class Diagram

```
                  Bucket
                     ▲
                     │
               TokenBucket
                     ▲
                     │
             BucketStore
                     ▲
                     │
TokenBucketRateLimiterService
                     ▲
                     │
          RateLimiterService
                     ▲
                     │
        RateLimitInterceptor
                     │
                     ▼
          ClientIdentifier
```

Configuration classes remain independent and provide runtime settings.

---

# Object Relationships

## Request Flow

```
HTTP Request

↓

RateLimitInterceptor

↓

ClientIdentifier

↓

RateLimiterService

↓

BucketStore

↓

TokenBucket
```

The controller is invoked **only if** the request is allowed.

---

# Class Responsibilities

## RateLimitInterceptor

Responsibilities:

- Intercepts requests
- Extracts client identity
- Calls the service layer
- Adds rate-limit headers
- Rejects blocked requests

It contains **no algorithmic logic**.

---

## ClientIdentifier

Responsible for generating a unique identifier for each client.

Current strategy:

```
Trusted Proxy?

↓

Yes

↓

X-Forwarded-For

----------------

No

↓

Remote Address
```

Future implementations may identify clients using:

- JWT
- API Key
- OAuth Client ID
- User ID

---

## RateLimiterService

Business abstraction.

Primary methods include:

```java
allowRequest()

getRemainingTokens()

getRetryAfter()

cleanup()
```

The interface prevents callers from depending on a specific algorithm.

---

## TokenBucketRateLimiterService

Coordinates the rate-limiting workflow.

Responsibilities:

- Retrieve bucket
- Trigger lazy refill
- Consume token
- Return decision
- Cleanup expired buckets

It delegates bucket-specific logic instead of implementing it directly.

---

## BucketStore

Stores all active buckets.

Implementation:

```java
ConcurrentHashMap<String, TokenBucket>
```

Responsibilities:

- Retrieve bucket
- Create bucket
- Remove bucket
- Remove expired buckets

---

## Bucket

Represents the abstraction of a rate-limiting bucket.

Current implementation:

```
TokenBucket
```

Future implementations may include:

- RedisBucket
- SlidingWindowBucket
- LeakyBucket

---

## TokenBucket

This class contains the algorithm itself.

State maintained:

- Capacity
- Available tokens
- Refill rate
- Refill timestamp
- Last access timestamp
- Expiration time

Primary operations:

- Refill
- Consume
- Retry calculation
- Expiration check

---

## BucketRefillStrategy

Encapsulates refill calculations.

Responsibilities:

- Calculate elapsed refill intervals
- Compute refill amount
- Determine updated token count

Separating this logic keeps `TokenBucket` focused on bucket state management.

---

## BucketCleanupScheduler

Periodically removes inactive buckets.

Flow

```
Scheduler

↓

BucketStore

↓

Expired Buckets

↓

Remove
```

This prevents long-term memory growth.

---

## GlobalExceptionHandler

Handles

```
RateLimitExceededException
```

and converts it into a consistent HTTP 429 response.

---

# Request Execution Flow

```
Client Request

↓

Interceptor

↓

Client Identifier

↓

RateLimiterService

↓

BucketStore

↓

TokenBucket

↓

Lazy Refill

↓

Consume Token

↓

Allowed?

↓

Yes

↓

Controller

----------------

No

↓

HTTP 429
```

---

# Bucket Lifecycle

```
Client

↓

First Request

↓

Create Bucket

↓

Store Bucket

↓

Process Requests

↓

Inactive

↓

Expired

↓

Cleanup

↓

Removed
```

This lifecycle ensures that memory usage remains proportional to active clients.

---

# Sequence Diagram

```
Client
   │
   ▼
Interceptor
   │
   ▼
ClientIdentifier
   │
   ▼
RateLimiterService
   │
   ▼
BucketStore
   │
   ▼
TokenBucket
   │
   ▼
Allow / Reject
   │
   ▼
HTTP Response
```

Every request follows this sequence.

---

# SOLID Principles

## Single Responsibility Principle (SRP)

Each class performs one primary task.

| Class | Responsibility |
|--------|----------------|
| Interceptor | HTTP interception |
| ClientIdentifier | Client identification |
| BucketStore | Bucket management |
| TokenBucket | Token algorithm |
| RefillStrategy | Refill calculations |

---

## Open/Closed Principle (OCP)

New algorithms can be introduced without modifying existing callers.

Example:

```
RateLimiterService

↓

TokenBucket

↓

SlidingWindow

↓

LeakyBucket
```

---

## Liskov Substitution Principle (LSP)

Any implementation of

```java
Bucket
```

or

```java
RateLimiterService
```

can replace another without changing client code.

---

## Interface Segregation Principle (ISP)

Consumers depend only on the methods they require.

The service interface exposes only rate-limiting operations.

---

## Dependency Inversion Principle (DIP)

High-level components depend on abstractions.

```
Interceptor

↓

RateLimiterService

↓

Implementation
```

instead of

```
Interceptor

↓

TokenBucketRateLimiterService
```

---

# Design Patterns

## Strategy Pattern

```
Bucket

↓

TokenBucket
```

Future strategies can implement the same abstraction.

---

## Dependency Injection

Spring manages component creation.

```
@Bean

↓

@Autowired

↓

Application Components
```

No class manually creates its dependencies.

---

## Factory Behaviour

`computeIfAbsent()` effectively acts as a bucket factory by creating buckets only when required.

---

# Error Handling

When a client exceeds the limit:

```
Bucket Empty

↓

RateLimitExceededException

↓

GlobalExceptionHandler

↓

HTTP 429
```

This centralizes error handling and keeps business logic clean.

---

# Configuration Design

Configuration is externalized.

```
application.properties

↓

RateLimitProperties

↓

Injected into Service
```

Configurable values include:

- Capacity
- Refill tokens
- Refill duration
- Bucket expiration
- Trusted proxy support
- Excluded endpoints

No source code changes are required to adjust these settings.

---

# Concurrency Design

Thread safety is achieved through multiple mechanisms.

| Component | Purpose |
|-----------|---------|
| ConcurrentHashMap | Thread-safe bucket storage |
| AtomicLong | Atomic token count |
| ReentrantLock | Protect refill and consume operations |
| volatile fields | Memory visibility |

Each bucket owns its own lock.

This avoids global synchronization and allows requests for different clients to execute concurrently.

---

# Extensibility

The architecture allows future enhancements with minimal changes.

Examples:

### Replace Storage

```
ConcurrentHashMap

↓

Redis
```

---

### Add New Algorithm

```
TokenBucket

↓

SlidingWindow
```

---

### Change Client Identification

```
IP Address

↓

JWT

↓

API Key
```

---

### Add Monitoring

Integrate Micrometer and Prometheus without changing the algorithm.

---

# Testing Strategy

The design supports testing at multiple levels.

## Unit Tests

- TokenBucket
- BucketRefillStrategy
- ClientIdentifier
- BucketStore

---

## Integration Tests

- Interceptor
- Service layer
- Configuration
- Exception handling

---

## Concurrency Tests

Verify:

- Correct token consumption
- No race conditions
- Proper refill behaviour
- Cleanup correctness

---

# Interview Discussion Points

### Why use interfaces?

To support multiple implementations and follow the Dependency Inversion Principle.

---

### Why is BucketStore separate?

To isolate storage concerns from algorithm logic.

---

### Why is ClientIdentifier independent?

Authentication and rate limiting evolve independently.

---

### Why separate refill calculations?

Improves readability, testability, and reuse.

---

### Why not place all logic inside the interceptor?

It would violate the Single Responsibility Principle and make testing more difficult.

---

### How would you migrate to Redis?

Replace the BucketStore implementation while keeping the service and interceptor unchanged.

---

### Which classes are easiest to unit test?

- TokenBucket
- BucketRefillStrategy
- ClientIdentifier
- BucketStore

because they have focused responsibilities and minimal external dependencies.

---

# Summary

The Low-Level Design emphasizes modularity, clear responsibilities, and extensibility.

Key characteristics include:

- Layered architecture
- Interface-driven design
- One responsibility per class
- Thread-safe bucket management
- Externalized configuration
- Centralized exception handling
- Easy testing
- Future-ready architecture

By adhering to SOLID principles and leveraging Spring's dependency injection, the implementation remains easy to understand, maintain, and evolve into a distributed production-grade rate limiter.

---
