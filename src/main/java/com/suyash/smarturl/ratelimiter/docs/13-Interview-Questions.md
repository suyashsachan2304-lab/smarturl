# Interview Questions & Answers

## Table of Contents

1. Introduction
2. Beginner Level Questions
3. Intermediate Level Questions
4. Advanced Java Questions
5. Spring Boot Questions
6. System Design Questions
7. Production Readiness Questions
8. Follow-up Questions
9. Whiteboard Questions
10. HR/Managerial Questions
11. Improvements You Can Discuss
12. Summary

---

# Introduction

This document contains interview questions that can be asked based on this Rate Limiter project.

The questions are arranged from beginner to senior-level and include detailed answers, follow-up questions, and discussion points.

If you can comfortably answer these questions, you should be able to explain this project in interviews ranging from **SDE-1** to **Senior Backend Engineer**.

---

# Beginner Level Questions

---

## 1. What is Rate Limiting?

### Answer

Rate limiting is a mechanism that restricts how many requests a client can make within a specified period.

Example:

```
10 requests

↓

1 minute

↓

11th request

↓

HTTP 429
```

Benefits:

- Prevents API abuse
- Prevents brute-force attacks
- Protects backend services
- Ensures fair resource usage

---

## 2. Why do we need Rate Limiting?

### Answer

Without rate limiting:

- A single client can overwhelm the server.
- Attackers can perform brute-force attacks.
- APIs can become unavailable.
- Infrastructure costs increase.

Rate limiting improves both security and reliability.

---

## 3. Which algorithm did you use?

### Answer

I implemented the **Token Bucket Algorithm**.

Reasons:

- Supports burst traffic
- O(1) operations
- Constant memory usage
- Widely used in production systems

---

## 4. What is a Token Bucket?

### Answer

Imagine a bucket filled with tokens.

```
Bucket

★★★★★
```

Each request consumes one token.

When the bucket becomes empty:

```
Request

↓

Rejected

↓

HTTP 429
```

Tokens are automatically refilled over time.

---

## 5. Why not use Fixed Window?

### Answer

Fixed Window suffers from boundary problems.

Example:

```
10 requests

59th second

+

10 requests

60th second

=

20 requests
```

Token Bucket avoids this issue and provides smoother traffic handling.

---

# Intermediate Level Questions

---

## 6. Why did you choose Token Bucket?

### Answer

Because it provides:

- Burst support
- Constant memory usage
- O(1) operations
- Production-proven behavior

It offers an excellent balance between simplicity and performance.

---

## 7. Explain the request flow.

### Answer

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

↓

Allow / Reject
```

If rejected:

```
HTTP 429
```

---

## 8. Why use an Interceptor instead of a Filter?

### Answer

Interceptors integrate directly with Spring MVC.

Advantages:

- Executes before controllers
- Easy configuration
- Access to request and response
- Cleaner integration with Spring

Filters are more suitable for generic servlet-level concerns.

---

## 9. Why did you use ConcurrentHashMap?

### Answer

Because multiple threads access buckets simultaneously.

Benefits:

- Thread-safe
- High concurrency
- Lock-free reads
- Excellent scalability

---

## 10. Why not HashMap?

### Answer

HashMap is not thread-safe.

Concurrent modifications can produce:

- Lost updates
- Corrupted state
- Race conditions

---

# Advanced Java Questions

---

## 11. Why use AtomicLong?

### Answer

AtomicLong allows atomic updates to the token count without synchronizing every numeric operation.

Benefits:

- Thread-safe
- Fast
- Lock-free for simple operations

---

## 12. If AtomicLong is thread-safe, why do you still need ReentrantLock?

### Answer

AtomicLong protects only **one variable**.

A refill updates multiple fields:

- Token count
- Last refill timestamp
- Last access timestamp

Those updates must occur together.

ReentrantLock ensures the entire refill-and-consume operation is atomic.

---

## 13. Why use System.nanoTime()?

### Answer

Because it measures elapsed time.

Advantages:

- Monotonic
- High precision
- Unaffected by system clock changes

`currentTimeMillis()` can move backward if the system clock changes.

---

## 14. Why not synchronize the whole bucket?

### Answer

That would work, but ReentrantLock provides:

- Better flexibility
- tryLock()
- Interruptible locking
- Fair locking options

Also, locking is limited to each bucket rather than the whole application.

---

## 15. Explain Race Condition.

### Answer

Suppose one token remains.

```
Thread A

↓

Reads 1

---------------

Thread B

↓

Reads 1

---------------

Both consume
```

Now two requests succeed even though only one token existed.

Proper synchronization prevents this.

---

# Spring Boot Questions

---

## 16. Why use Configuration Properties?

### Answer

Instead of hardcoding values,

```
capacity = 20
```

the application loads

```
application.properties

↓

RateLimitProperties
```

Benefits:

- No recompilation
- Environment-specific settings
- Easier maintenance

---

## 17. How are excluded endpoints handled?

### Answer

The interceptor checks whether the request path matches configured exclusions.

Examples:

```
/swagger-ui

/v3/api-docs

/actuator
```

Excluded endpoints bypass rate limiting.

---

## 18. Why throw a custom exception?

### Answer

Instead of returning HTTP responses directly,

the service throws

```
RateLimitExceededException
```

which is converted into HTTP 429 by a global exception handler.

This keeps business logic independent of HTTP concerns.

---

# System Design Questions

---

## 19. What happens if you run two application instances?

### Answer

Each instance has its own in-memory buckets.

```
Load Balancer

↓

Instance A

↓

Bucket A

---------------

Instance B

↓

Bucket B
```

The client effectively receives separate limits on each instance.

---

## 20. How would you support multiple servers?

### Answer

Replace

```
ConcurrentHashMap
```

with

```
Redis
```

All application instances then share the same bucket state.

---

## 21. Why Redis?

### Answer

Redis provides:

- In-memory speed
- Atomic operations
- Expiration support
- High availability
- Clustering

It is commonly used for distributed rate limiting.

---

## 22. Would you move rate limiting to the API Gateway?

### Answer

Yes.

Benefits:

- Single enforcement point
- Reduced code duplication
- Consistent policies
- Better scalability

Examples:

- Spring Cloud Gateway
- Kong
- Envoy
- NGINX

---

## 23. How would you support different rate limits?

### Answer

Possible strategies:

- Per endpoint
- Per API key
- Per user
- Per subscription plan
- Per organization

Each client can receive its own bucket configuration.

---

# Production Readiness Questions

---

## 24. What happens after application restart?

### Answer

Buckets are stored in memory.

After restart,

all buckets are recreated as clients send new requests.

A distributed implementation would persist bucket state in Redis.

---

## 25. How is memory controlled?

### Answer

Inactive buckets are automatically removed.

```
Inactive

↓

Expired

↓

Cleanup

↓

Removed
```

This prevents unlimited memory growth.

---

## 26. How would you monitor this system?

### Answer

Expose metrics such as:

- Active buckets
- Rejected requests
- Allowed requests
- Average remaining tokens
- Cleanup executions

using Micrometer with Prometheus and Grafana.

---

## 27. What are the bottlenecks?

### Answer

Current bottlenecks include:

- Single JVM storage
- One lock per bucket
- Memory proportional to active clients

These are acceptable for an in-memory implementation.

---

# Follow-up Questions

---

### Why not Sliding Window?

Because Token Bucket offers burst support with lower memory usage.

---

### Why not Leaky Bucket?

Leaky Bucket smooths traffic but does not allow bursts.

---

### Why per-bucket locking?

To avoid global synchronization and improve concurrency.

---

### Why interface-based design?

To support multiple implementations without modifying callers.

---

### Why separate ClientIdentifier?

Authentication strategy and rate limiting evolve independently.

---

# Whiteboard Questions

---

### Draw the architecture.

```
Client

↓

Interceptor

↓

RateLimiterService

↓

BucketStore

↓

TokenBucket
```

---

### Draw production architecture.

```
Internet

↓

API Gateway

↓

Redis

↓

Spring Boot Cluster
```

---

### Draw bucket lifecycle.

```
Create

↓

Use

↓

Inactive

↓

Expired

↓

Cleanup
```

---

# HR / Managerial Questions

---

## Explain this project in two minutes.

> I built a production-style Token Bucket Rate Limiter in Spring Boot that limits client requests while supporting burst traffic. The implementation uses ConcurrentHashMap for bucket storage, AtomicLong for efficient token management, ReentrantLock for thread-safe refill and consume operations, and System.nanoTime() for precise elapsed-time calculations. It integrates with Spring MVC using an interceptor, supports configurable limits, excluded endpoints, cleanup of inactive buckets, and returns standard HTTP 429 responses. The architecture is modular, making it easy to evolve into a distributed solution using Redis or API Gateways.

---

## What was the most difficult part?

Designing thread-safe refill logic.

It required ensuring that multiple fields (tokens and timestamps) were updated atomically without introducing unnecessary locking.

---

## What would you improve next?

- Redis-based bucket storage
- Distributed rate limiting
- Micrometer metrics
- Per-user limits
- Dynamic configuration
- Gateway integration

---

# Improvements You Can Discuss

Future enhancements include:

- Redis Cluster
- Spring Cloud Gateway
- Kafka analytics
- Prometheus monitoring
- Grafana dashboards
- Dynamic configuration
- Role-based limits
- API key limits
- Weighted tokens
- Distributed locking
- Adaptive rate limiting using traffic patterns

---

# Summary

This project demonstrates knowledge across multiple backend engineering areas:

- Java concurrency
- Spring Boot architecture
- REST API design
- Thread safety
- Token Bucket algorithm
- Performance optimization
- System design
- Configuration management
- Exception handling
- Scalability planning
- Production architecture

Because the implementation progresses from an in-memory solution to an enterprise-ready distributed design, it provides excellent material for discussing backend engineering decisions in technical interviews.

---

# Final Notes

- **Why** you chose Token Bucket,
- **How** thread safety is achieved,
- **Why** the architecture follows SOLID principles,
- **How** the system scales to multiple instances,
- **How** you would evolve it using Redis and an API Gateway,
