# 🚦 Rate Limiter Module

A production-ready **Token Bucket Rate Limiter** built using **Java** and **Spring Boot**, designed to protect REST APIs from abuse while maintaining high throughput and thread safety.

This module provides a reusable and extensible implementation of the **Token Bucket Algorithm** with configurable rate limits, lazy token refill, automatic cleanup of inactive clients, and seamless integration with Spring Boot using an HTTP interceptor.

---

# Table of Contents

- Overview
- Features
- Why Rate Limiting?
- Why Token Bucket?
- Module Architecture
- Package Structure
- Request Flow
- Core Components
- Configuration
- Thread Safety
- Performance
- Future Enhancements
- Documentation
- References

---

# Overview

Modern backend services receive requests from thousands or even millions of users. Without proper request control, APIs become vulnerable to:

- API abuse
- DDoS attacks
- Brute-force login attempts
- Resource exhaustion
- Increased infrastructure cost
- Unfair resource consumption

Rate limiting solves these problems by controlling how many requests a client can make during a specific period.

This module implements the **Token Bucket Algorithm**, one of the most widely used rate-limiting algorithms in production systems.

---

# Features

## Core Features

- Token Bucket Algorithm
- Configurable bucket capacity
- Configurable refill rate
- Lazy token refill
- Burst traffic support
- Thread-safe implementation
- Automatic inactive bucket cleanup
- Spring MVC Interceptor integration
- Client identification abstraction
- Custom exception handling
- Easy configuration using properties

---

## Engineering Features

- SOLID principles
- Clean Architecture
- Dependency Injection
- Interface-based design
- Extensible implementation
- Production-oriented design
- Minimal locking
- O(1) request processing

---

# Why Rate Limiting?

Rate limiting protects APIs by limiting how many requests a client can send.

Without rate limiting:

```
Attacker

↓

100000 Requests

↓

Application

↓

CPU Exhaustion

↓

Database Exhaustion

↓

Application Crash
```

With rate limiting:

```
Attacker

↓

100000 Requests

↓

Rate Limiter

↓

100 Allowed

↓

99900 Rejected

↓

Application
```

Benefits:

- Prevent API abuse
- Protect backend services
- Prevent brute-force attacks
- Reduce infrastructure cost
- Ensure fair usage
- Improve stability
- Improve scalability

---

# Why Token Bucket?

Many rate limiting algorithms exist.

| Algorithm | Burst Support | Memory | Accuracy |
|------------|--------------|---------|----------|
| Fixed Window | ❌ | Low | Medium |
| Sliding Window | ✅ | High | High |
| Leaky Bucket | ❌ | Low | High |
| Token Bucket | ✅ | Low | High |

This project uses **Token Bucket** because it:

- Allows controlled bursts
- Has constant memory usage
- Has constant processing time
- Is widely used in production systems
- Is simple to scale

---

# High-Level Architecture

```
                Client
                   │
                   ▼
        RateLimitInterceptor
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
        ┌──────────┴──────────┐
        │                     │
    Token Available       No Token
        │                     │
        ▼                     ▼
 Continue Request      HTTP 429 Response
```

---

# Package Structure

```
rate-limiter
│
├── Bucket.java
├── TokenBucket.java
├── BucketStore.java
├── BucketRefillStrategy.java
├── RateLimiterService.java
├── TokenBucketRateLimiterService.java
├── ClientIdentifier.java
├── RateLimitInterceptor.java
├── BucketCleanupScheduler.java
├── RateLimitConfiguration.java
├── RateLimitProperties.java
├── RateLimitExceededException.java
└── README.md
```

---

# Request Lifecycle

Every incoming request follows this sequence.

```
HTTP Request
      │
      ▼
RateLimitInterceptor
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
Find Bucket
      │
      ▼
Lazy Refill
      │
      ▼
Consume Token
      │
 ┌────┴────┐
 │         │
 ▼         ▼
Allow     Reject
 │         │
 ▼         ▼
Controller HTTP 429
```

---

# Core Components

## Bucket

Defines the contract for bucket implementations.

Responsibilities:

- Consume tokens
- Report available tokens
- Support future algorithms

---

## TokenBucket

Concrete implementation of the Token Bucket algorithm.

Responsibilities:

- Store tokens
- Refill tokens
- Consume tokens
- Handle burst traffic
- Ensure thread safety

---

## BucketStore

Maintains buckets for all clients.

Responsibilities:

- Create bucket
- Retrieve bucket
- Remove inactive buckets

---

## BucketRefillStrategy

Defines how buckets are refilled.

This abstraction allows multiple refill strategies without modifying bucket implementations.

---

## RateLimiterService

Business layer responsible for checking whether a request should be allowed.

---

## TokenBucketRateLimiterService

Implements rate limiting using Token Bucket.

---

## ClientIdentifier

Extracts a unique identifier for each client.

Possible identifiers:

- IP Address
- API Key
- User ID
- JWT Subject

---

## RateLimitInterceptor

Intercepts every incoming request before it reaches the controller.

Responsibilities:

- Identify client
- Check bucket
- Reject when limit exceeded

---

## BucketCleanupScheduler

Periodically removes inactive buckets to prevent memory leaks.

---

## RateLimitProperties

Loads configuration from Spring Boot properties.

Example:

```properties
rate-limit.capacity=20
rate-limit.refill-tokens=10
rate-limit.refill-duration=60s
```

---

## RateLimitConfiguration

Creates and wires Spring Beans.

---

## RateLimitExceededException

Custom exception thrown when a client exceeds the configured limit.

---

# Thread Safety

The implementation is designed for concurrent environments.

Key techniques:

- ConcurrentHashMap
- ReentrantLock
- Atomic operations
- Lazy refill
- Fine-grained locking

This allows multiple clients to access the rate limiter simultaneously without corrupting bucket state.

---

# Performance Characteristics

| Operation | Complexity |
|------------|-----------:|
| Find Bucket | O(1) |
| Create Bucket | O(1) |
| Consume Token | O(1) |
| Lazy Refill | O(1) |
| Cleanup | O(n) |

Memory usage grows linearly with the number of active clients.

---

# Future Enhancements

Current implementation is suitable for a single application instance.

Future improvements include:

- Redis-backed buckets
- Distributed rate limiting
- Lua scripts for atomic updates
- API Gateway integration
- Adaptive rate limiting
- User-based quotas
- Role-based limits
- Metrics with Micrometer
- Prometheus monitoring
- Grafana dashboards

---

# Documentation

Detailed documentation is available under the `docs/` directory.

| File | Description |
|------|-------------|
| 01-Rate-Limiting.md | Fundamentals of Rate Limiting |
| 02-Rate-Limiting-Algorithms.md | Comparison of Rate Limiting Algorithms |
| 03-Token-Bucket.md | Complete Token Bucket Guide |
| 04-Project-Architecture.md | Module Architecture |
| 05-Request-Lifecycle.md | Complete Request Flow |
| 06-Implementation.md | Class-by-Class Explanation |
| 07-Design-Decisions.md | Engineering Decisions |
| 08-Thread-Safety.md | Concurrency Model |
| 09-Performance-Analysis.md | Complexity and Scalability |
| 10-Production-Evolution.md | Scaling to Distributed Systems |
| 11-HLD-Guide.md | High-Level Design |
| 12-LLD-Guide.md | Low-Level Design |
| 13-Interview-Questions.md | Interview Preparation |

---

# References

- RFC 6585 — HTTP 429 Too Many Requests
- Token Bucket Algorithm
- Spring MVC Interceptor
- Java Concurrency Utilities
- Effective Java
- Designing Data-Intensive Applications

---

# License

This module is intended for educational purposes and can be extended for production use.