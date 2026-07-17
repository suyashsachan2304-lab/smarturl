# Design Decisions

## Table of Contents

1. Introduction
2. Design Goals
3. Why Token Bucket?
4. Why an Interface-Based Design?
5. Why Separate Business Logic?
6. Why Use an Interceptor?
7. Why Lazy Refill?
8. Why ConcurrentHashMap?
9. Why AtomicLong?
10. Why ReentrantLock?
11. Why System.nanoTime()?
12. Why One Bucket Per Client?
13. Why a Cleanup Mechanism?
14. Why External Configuration?
15. Why Separate Client Identification?
16. Design Trade-offs
17. Future Improvements
18. Summary

---

# Introduction

Every software system is a collection of design decisions.

Some decisions improve performance.

Some improve maintainability.

Some improve scalability.

This document explains **why this project is designed the way it is**, the alternatives that were considered, and the trade-offs involved.

---

# Design Goals

Before implementing the rate limiter, several objectives were established.

The implementation should be:

- Simple to understand
- Easy to integrate with Spring Boot
- Thread-safe
- Memory efficient
- Highly performant
- Configurable
- Easy to extend
- Suitable for production workloads

These goals influenced every architectural and implementation decision.

---

# Why Token Bucket?

Several rate-limiting algorithms were evaluated.

| Algorithm | Burst Support | Memory | Complexity | Selected |
|------------|--------------|---------|------------|----------|
| Fixed Window | ❌ | Low | Low | ❌ |
| Sliding Window Log | ✅ | High | High | ❌ |
| Sliding Window Counter | Partial | Medium | Medium | ❌ |
| Leaky Bucket | ❌ | Low | Medium | ❌ |
| **Token Bucket** | ✅ | Low | Low | ✅ |

### Reasons

- Allows burst traffic
- Constant memory usage
- O(1) request processing
- Easy implementation
- Excellent scalability
- Widely used in production

---

# Why an Interface-Based Design?

Instead of tightly coupling the application to one implementation,

the project defines

```java
RateLimiterService
```

as an interface.

```
RateLimiterService
        ▲
        │
TokenBucketRateLimiterService
```

### Advantages

- Easy to replace implementations
- Better testing
- Supports future algorithms
- Follows Dependency Inversion Principle

Example future implementations

```
SlidingWindowRateLimiterService

RedisRateLimiterService

LeakyBucketRateLimiterService
```

No changes are required in the interceptor.

---

# Why Separate Business Logic?

The interceptor only coordinates request processing.

It does **not** contain rate-limiting logic.

Instead

```
Interceptor

↓

RateLimiterService

↓

TokenBucket
```

### Advantages

- Cleaner code
- Easier maintenance
- Better unit testing
- Reusable business logic

---

# Why Use an Interceptor?

Spring Boot provides several ways to intercept requests.

Options include:

- Filter
- Interceptor
- Controller Advice
- Aspect (AOP)

This project uses a **Spring MVC Interceptor**.

### Reasons

- Executes before controllers
- Easy registration
- Access to request and response
- Lightweight
- Integrates naturally with Spring MVC

---

## Why Not a Filter?

Filters execute before Spring MVC.

Although filters are useful,

they lack awareness of Spring MVC handler mappings and are better suited for generic servlet concerns.

Interceptors integrate more naturally with controller-based applications.

---

## Why Not AOP?

AOP is excellent for cross-cutting concerns.

However,

rate limiting operates at the HTTP request level rather than individual method execution.

An interceptor is simpler and avoids unnecessary complexity.

---

# Why Lazy Refill?

Two refill strategies exist.

### Strategy 1

Background Scheduler

```
Every Second

↓

Refill All Buckets
```

### Strategy 2

Lazy Refill

```
Request Arrives

↓

Refill Bucket

↓

Consume Token
```

This project uses **Lazy Refill**.

### Reasons

- Lower CPU usage
- No continuously running thread
- No unnecessary work for inactive clients
- Simpler implementation
- Better scalability

---

# Why ConcurrentHashMap?

Buckets must be stored in memory.

Possible choices

```
HashMap

ConcurrentHashMap

SynchronizedMap
```

The implementation uses

```java
ConcurrentHashMap
```

### Reasons

- High concurrent throughput
- Lock-free reads
- Fine-grained synchronization
- Excellent scalability

---

## Why Not HashMap?

`HashMap` is **not thread-safe**.

Concurrent updates could lead to

- Race conditions
- Lost updates
- Corrupted internal state

---

# Why AtomicLong?

The number of available tokens changes frequently.

Possible implementations

```
long

AtomicLong
```

Current implementation

```java
AtomicLong
```

### Reasons

- Atomic operations
- Thread-safe reads
- Minimal overhead
- Excellent performance

---

# Why ReentrantLock?

Refilling a bucket updates multiple values simultaneously.

Example

```
Tokens

Timestamp

Last Access Time
```

Updating them independently is unsafe.

The implementation protects this critical section using

```java
ReentrantLock
```

### Why not synchronize?

Both would work.

`ReentrantLock` provides additional capabilities such as

- Explicit locking
- tryLock()
- Fairness policies
- More flexible control

Although only basic locking is currently required, the implementation remains extensible.

---

# Why System.nanoTime()?

Time calculations are central to token refilling.

Possible choices

```
currentTimeMillis()

nanoTime()
```

Current implementation

```java
System.nanoTime()
```

### Reasons

- Monotonic
- Higher precision
- Immune to system clock adjustments
- Better for elapsed-time calculations

---

# Why One Bucket Per Client?

Each client owns its own bucket.

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

### Advantages

- Independent rate limits
- No interference between users
- Easy lookup
- Constant-time access

---

# Why a Cleanup Mechanism?

Suppose

```
1 Million Clients

↓

1 Million Buckets
```

Many clients may never send another request.

Without cleanup

```
Unused Buckets

↓

Memory Growth
```

The cleanup process removes inactive buckets automatically.

Benefits

- Prevents memory leaks
- Stable memory usage
- Better long-running performance

---

# Why External Configuration?

Values such as

```
Capacity

Refill Tokens

Refill Duration

Expiration
```

are stored in configuration.

Instead of

```java
capacity = 20;
```

the application loads

```properties
rate-limit.capacity=20
```

### Advantages

- No recompilation
- Easy tuning
- Environment-specific settings
- Cleaner code

---

# Why Separate Client Identification?

Different systems identify users differently.

Examples

```
IP Address

JWT

API Key

OAuth Client

Customer ID
```

Embedding this logic inside the interceptor would tightly couple authentication and rate limiting.

Instead

```
ClientIdentifier
```

is responsible for generating the unique identifier.

This follows the **Single Responsibility Principle**.

---

# Design Trade-offs

Every design has advantages and limitations.

| Decision | Benefit | Trade-off |
|-----------|----------|-----------|
| Token Bucket | Burst support | Requires refill calculation |
| Lazy Refill | Lower CPU usage | Refill occurs during request processing |
| ConcurrentHashMap | Excellent concurrency | Slightly higher memory overhead |
| AtomicLong | Fast atomic updates | Only suitable for single-variable operations |
| ReentrantLock | Safe critical sections | Small locking overhead |
| In-Memory Storage | Extremely fast | Single JVM only |
| Interceptor | Spring integration | Limited to HTTP requests |

Understanding these trade-offs is essential when designing production systems.

---

# Future Improvements

The current design intentionally leaves room for future enhancements.

Possible improvements include:

### Distributed Bucket Store

Replace

```
ConcurrentHashMap
```

with

```
Redis
```

to support multiple application instances.

---

### Multiple Algorithms

Support

- Sliding Window
- Leaky Bucket
- Fixed Window

through additional implementations of

```
RateLimiterService
```

---

### Dynamic Configuration

Reload rate limits without restarting the application.

---

### Monitoring

Expose metrics such as

- Active buckets
- Rejected requests
- Remaining tokens
- Average refill time

through Micrometer and Prometheus.

---

### Per-Endpoint Limits

Example

```
/login

↓

5 requests/minute

----------------

/search

↓

100 requests/minute
```

---

### Role-Based Limits

Different limits for

- Anonymous users
- Premium users
- Administrators

---

# Summary

The design of this project is guided by simplicity, performance, and maintainability.

Key decisions include:

- Token Bucket algorithm
- Interface-driven architecture
- Spring MVC Interceptor
- Lazy refill strategy
- ConcurrentHashMap for bucket storage
- AtomicLong for token management
- ReentrantLock for thread safety
- System.nanoTime() for precise elapsed-time measurement
- Externalized configuration
- Separate client identification
- Automatic cleanup of inactive buckets

Each decision balances correctness, scalability, and ease of maintenance while leaving the architecture flexible enough to evolve into a distributed rate limiter in the future.

---
