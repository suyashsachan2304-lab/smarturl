# Project Architecture

## Table of Contents

1. Introduction
2. High-Level Architecture
3. Request Flow
4. Package Structure
5. Component Responsibilities
6. Class Relationships
7. Dependency Graph
8. Sequence Diagram
9. Design Principles
10. Extensibility
11. Scalability
12. Architecture Decisions
13. Summary

---

# Introduction

This module is designed as a **modular, reusable, and extensible rate limiting library** for Spring Boot applications.

Rather than embedding rate limiting logic directly into controllers or filters, the implementation follows a layered architecture where each class has a single responsibility.

The architecture emphasizes:

- Separation of Concerns
- Dependency Injection
- Interface-based Programming
- Thread Safety
- Extensibility
- Testability

---

# High-Level Architecture

```
                HTTP Request
                     │
                     ▼
        +-------------------------+
        | RateLimitInterceptor    |
        +-------------------------+
                     │
                     ▼
        +-------------------------+
        | ClientIdentifier        |
        +-------------------------+
                     │
                     ▼
        +-------------------------+
        | RateLimiterService      |
        +-------------------------+
                     │
                     ▼
        +-------------------------+
        | BucketStore             |
        +-------------------------+
                     │
                     ▼
        +-------------------------+
        | TokenBucket             |
        +-------------------------+
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
 Allowed Request         RateLimitExceededException
        │                         │
        ▼                         ▼
 Controller                 HTTP 429
```

---

# Request Flow

Every incoming request follows the same lifecycle.

```
Client

↓

Spring DispatcherServlet

↓

RateLimitInterceptor

↓

Extract Client Identifier

↓

Find Client Bucket

↓

Lazy Refill

↓

Consume Token

↓

Token Available?
        │
   ┌────┴────┐
   │         │
 YES         NO
   │         │
   ▼         ▼
Controller  HTTP 429
```

---

# Package Structure

```
rate-limiter/

├── config/
│   ├── RateLimitConfiguration.java
│   ├── RateLimitProperties.java
│   └── WebMvcConfig.java
│
├── interceptor/
│   └── RateLimitInterceptor.java
│
├── service/
│   ├── RateLimiterService.java
│   └── TokenBucketRateLimiterService.java
│
├── bucket/
│   ├── Bucket.java
│   ├── TokenBucket.java
│   ├── BucketStore.java
│   └── BucketRefillStrategy.java
│
├── scheduler/
│   └── BucketCleanupScheduler.java
│
├── identifier/
│   └── ClientIdentifier.java
│
├── exception/
│   ├── RateLimitExceededException.java
│   └── GlobalExceptionHandler.java
│
└── README.md
```

*(Adjust package names if your project structure differs.)*

---

# Component Responsibilities

## 1. RateLimitInterceptor

Acts as the entry point.

Responsibilities:

- Intercepts HTTP requests
- Identifies the client
- Calls the rate limiter
- Blocks excessive requests
- Allows valid requests

This keeps controllers free from rate-limiting logic.

---

## 2. ClientIdentifier

Responsible for identifying a client.

Possible implementations:

- IP Address
- API Key
- JWT Subject
- User ID

Keeping this separate allows the identification strategy to change without affecting the rate limiter.

---

## 3. RateLimiterService

Defines the contract for rate limiting.

Example responsibilities:

- Check request permission
- Throw exception on limit exceeded

Because it is an interface, new algorithms can be added without changing callers.

---

## 4. TokenBucketRateLimiterService

Implements the business logic.

Responsibilities:

- Retrieve bucket
- Trigger lazy refill
- Consume token
- Return allow/reject decision

---

## 5. BucketStore

Stores buckets for every client.

Example

```
Client A

↓

Bucket A

----------------

Client B

↓

Bucket B

----------------

Client C

↓

Bucket C
```

Usually implemented using:

```
ConcurrentHashMap
```

---

## 6. TokenBucket

Represents one client's bucket.

Stores:

- Capacity
- Available tokens
- Last refill timestamp

Provides operations such as:

- refill()
- consume()
- availableTokens()

---

## 7. BucketRefillStrategy

Encapsulates refill calculations.

Advantages:

- Cleaner code
- Easy to test
- Different refill strategies can be introduced later

---

## 8. BucketCleanupScheduler

Responsible for removing inactive buckets.

Without cleanup:

```
Old Clients

↓

Old Buckets

↓

Memory Growth
```

Cleanup prevents memory leaks.

---

## 9. RateLimitProperties

Loads configuration from Spring Boot.

Example

```properties
rate-limit.capacity=20
rate-limit.refill-tokens=10
rate-limit.refill-duration=60s
```

Configuration stays outside the code.

---

## 10. RateLimitConfiguration

Creates Spring Beans.

Typical responsibilities:

- Register services
- Register interceptor
- Wire dependencies

---

## 11. GlobalExceptionHandler

Converts

```
RateLimitExceededException
```

into

```
HTTP 429 Too Many Requests
```

This keeps controllers clean and provides a consistent API response.

---

# Class Relationships

```
RateLimitInterceptor
        │
        ▼
RateLimiterService
        │
        ▼
TokenBucketRateLimiterService
        │
        ▼
BucketStore
        │
        ▼
TokenBucket
```

Supporting classes

```
ClientIdentifier

RateLimitProperties

BucketCleanupScheduler

GlobalExceptionHandler
```

---

# Dependency Graph

```
              Configuration
                     │
                     ▼
          RateLimiterService
                     │
                     ▼
        TokenBucketRateLimiterService
             │                 │
             ▼                 ▼
      BucketStore      ClientIdentifier
             │
             ▼
        TokenBucket
```

Notice that dependencies always point downward.

No circular dependencies exist.

---

# Sequence Diagram

```
Client

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

↓

Refill()

↓

Consume()

↓

Return Result

↓

Controller
```

If no tokens remain:

```
Consume()

↓

RateLimitExceededException

↓

GlobalExceptionHandler

↓

HTTP 429
```

---

# Design Principles

The architecture follows several well-known software engineering principles.

## Single Responsibility Principle

Each class has one responsibility.

Examples:

- Bucket stores tokens
- Store manages buckets
- Interceptor intercepts requests
- Scheduler performs cleanup

---

## Open/Closed Principle

Adding another algorithm (for example Sliding Window) should require creating a new implementation of `RateLimiterService`, not modifying existing classes.

---

## Dependency Inversion Principle

Higher-level components depend on abstractions (`RateLimiterService`) instead of concrete implementations.

---

## Separation of Concerns

Responsibilities are divided into:

- Configuration
- Request interception
- Client identification
- Business logic
- Storage
- Cleanup
- Exception handling

---

# Extensibility

The current design allows future enhancements without major refactoring.

Possible additions include:

- Redis-backed bucket store
- Sliding Window algorithm
- Leaky Bucket implementation
- Role-based rate limits
- API-specific limits
- Metrics collection
- Prometheus integration
- Distributed rate limiting

---

# Scalability

### Current Architecture

```
Application

↓

In-Memory Buckets
```

Suitable for:

- Single JVM
- Internal services
- Development
- Small deployments

---

### Future Architecture

```
Client

↓

Load Balancer

↓

Application A

↓

Redis

↑

Application B

↓

Application C
```

Using Redis allows all application instances to share bucket state.

---

# Architecture Decisions

| Decision | Reason |
|----------|--------|
| Spring Interceptor | Intercepts requests before controllers |
| Interface-based service | Easy to swap algorithms |
| ConcurrentHashMap | Fast concurrent bucket lookup |
| Lazy refill | Avoids background refill threads |
| Cleanup scheduler | Prevents memory growth |
| Exception handler | Consistent HTTP 429 responses |
| Spring configuration | Externalized configuration |

---

# Summary

The architecture is intentionally modular and layered.

Key characteristics:

- Clear separation of responsibilities
- Thread-safe design
- Extensible through interfaces
- Spring Boot friendly
- Production-oriented
- Easy to test
- Easy to maintain
