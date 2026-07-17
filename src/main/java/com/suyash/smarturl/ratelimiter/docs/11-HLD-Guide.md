# High-Level Design (HLD) Guide

## Table of Contents

1. Introduction
2. Problem Statement
3. Functional Requirements
4. Non-Functional Requirements
5. High-Level Architecture
6. Major Components
7. Request Flow
8. Data Flow
9. Component Responsibilities
10. Scalability Considerations
11. Availability & Reliability
12. Performance Characteristics
13. Security Considerations
14. Design Trade-offs
15. Production Evolution
16. Interview Discussion Points
17. Summary

---

# Introduction

High-Level Design (HLD) focuses on the overall architecture of a system.

It answers questions like:

- What are the major components?
- How do they communicate?
- Where should responsibilities be placed?
- How will the system scale?
- What are the trade-offs?

Unlike Low-Level Design (LLD), HLD does **not** focus on class implementations or methods. Instead, it provides a bird's-eye view of the system.

---

# Problem Statement

Design a rate limiter for a Spring Boot application that:

- Limits the number of requests per client.
- Supports burst traffic.
- Rejects excessive requests with HTTP 429.
- Is thread-safe.
- Is configurable.
- Can evolve into a distributed solution.

---

# Functional Requirements

The system should:

- Limit requests per client.
- Allow configurable bucket capacity.
- Allow configurable refill rate.
- Support burst traffic.
- Return **HTTP 429 Too Many Requests** when limits are exceeded.
- Include `Retry-After` information.
- Include remaining token information.
- Support excluding selected endpoints.
- Remove inactive buckets.

---

# Non-Functional Requirements

The system should be:

- Fast
- Thread-safe
- Highly concurrent
- Memory efficient
- Configurable
- Extensible
- Easy to test
- Easy to maintain

Performance goals:

- O(1) request processing
- O(1) bucket lookup
- O(Active Clients) memory usage

---

# High-Level Architecture

```
                  Client
                     │
                     ▼
            Spring Boot Application
                     │
                     ▼
         RateLimitInterceptor
                     │
                     ▼
          ClientIdentifier
                     │
                     ▼
      RateLimiterService Interface
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

Only one component is responsible for each concern.

---

# Major Components

## 1. RateLimitInterceptor

Responsibilities:

- Intercepts every request
- Extracts client identity
- Invokes rate limiter
- Rejects excessive traffic

---

## 2. ClientIdentifier

Responsible for identifying the client.

Possible identifiers:

- IP Address
- API Key
- JWT Subject
- User ID

---

## 3. RateLimiterService

Business abstraction.

Responsibilities:

- Validate requests
- Consume tokens
- Return rate limit status

Because it is an interface, future algorithms can be introduced without changing callers.

---

## 4. TokenBucketRateLimiterService

Concrete implementation of the rate limiter.

Responsibilities:

- Retrieve bucket
- Perform lazy refill
- Consume token
- Return allow/reject decision

---

## 5. BucketStore

Stores one bucket per client.

Current implementation:

```
ConcurrentHashMap

↓

Client ID

↓

TokenBucket
```

Future implementation:

```
Redis

↓

Distributed Bucket
```

---

## 6. TokenBucket

Maintains:

- Token count
- Capacity
- Refill timestamp
- Last access timestamp

Performs:

- Refill
- Consume
- Expiration check

---

## 7. Cleanup Scheduler

Removes inactive buckets.

Prevents unbounded memory growth.

---

# Request Flow

```
Client

↓

DispatcherServlet

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

Consume Token

↓

Allow / Reject

↓

Controller
```

If no tokens remain:

```
HTTP 429
```

is returned before reaching the controller.

---

# Data Flow

```
Request

↓

Extract Client ID

↓

Find Bucket

↓

Lazy Refill

↓

Consume Token

↓

Update Bucket

↓

Return Decision
```

No database operations occur.

Everything remains in memory.

---

# Component Responsibilities

| Component | Responsibility |
|------------|----------------|
| Interceptor | Intercept HTTP requests |
| ClientIdentifier | Identify client |
| RateLimiterService | Business abstraction |
| TokenBucketRateLimiterService | Execute algorithm |
| BucketStore | Store client buckets |
| TokenBucket | Manage token lifecycle |
| Cleanup Scheduler | Remove inactive buckets |
| Configuration | Load external properties |

---

# Scalability Considerations

## Current

```
Single JVM

↓

ConcurrentHashMap
```

Suitable for:

- Development
- Internal APIs
- Small deployments

---

## Future

```
Load Balancer

↓

Multiple Spring Boot Instances

↓

Redis
```

Benefits:

- Shared bucket state
- Horizontal scaling
- Global rate limiting

---

# Availability & Reliability

Current implementation:

- No external dependencies
- No network latency
- Fast startup

Distributed implementation:

- Redis replication
- Redis clustering
- Multiple application instances
- Load balancing
- Failover support

---

# Performance Characteristics

| Operation | Complexity |
|-----------|-----------:|
| Client Identification | O(1) |
| Bucket Lookup | O(1) |
| Lazy Refill | O(1) |
| Token Consumption | O(1) |
| Decision | O(1) |

Memory:

```
O(Number of Active Clients)
```

---

# Security Considerations

## Client Identification

Only trust

```
X-Forwarded-For
```

when requests originate from trusted proxies.

---

## Abuse Prevention

Rate limiting protects against:

- Brute-force attacks
- API abuse
- Denial-of-service attempts
- Excessive resource consumption

---

## Configuration

Do not hardcode limits.

External configuration allows safer deployment-specific tuning.

---

# Design Trade-offs

| Decision | Benefit | Trade-off |
|-----------|----------|-----------|
| Token Bucket | Burst support | Refill calculation required |
| Lazy Refill | Low CPU usage | Refill during request |
| ConcurrentHashMap | High concurrency | JVM-local only |
| In-memory Storage | Fast | Not distributed |
| Interceptor | Clean integration | Spring MVC specific |
| One Bucket Per Client | Isolation | Memory grows with active clients |

---

# Production Evolution

The architecture naturally evolves.

```
Current

↓

In-Memory Buckets

↓

Redis

↓

API Gateway

↓

Monitoring

↓

Dynamic Configuration

↓

Enterprise Rate Limiter
```

Each stage requires minimal architectural changes because responsibilities are already separated.

---

# Interview Discussion Points

### Why Token Bucket?

- Burst support
- Constant memory
- O(1) operations
- Production proven

---

### Why an Interceptor?

- Runs before controllers
- Easy Spring integration
- Lightweight

---

### Why Lazy Refill?

- No scheduler
- Lower CPU usage
- Simpler implementation

---

### Why ConcurrentHashMap?

- Thread-safe
- Fine-grained locking
- Excellent concurrency

---

### Why One Bucket Per Client?

Independent limits.

Easy lookup.

Constant-time access.

---

### How would you scale this?

- Replace BucketStore with Redis.
- Introduce API Gateway.
- Share bucket state.
- Add monitoring.
- Add dynamic configuration.

---

### What happens when the application restarts?

Current implementation:

Buckets are lost because they are stored in memory.

Production solution:

Store bucket state in Redis.

---

### How would you support millions of users?

- Redis Cluster
- Load Balancer
- Horizontal scaling
- Distributed bucket storage
- Monitoring
- Gateway-based enforcement

---

# Typical HLD Interview Whiteboard

```
                    Client
                       │
                       ▼
               Load Balancer
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
 Spring Boot A               Spring Boot B
         │                           │
         └─────────────┬─────────────┘
                       ▼
               RateLimiterService
                       │
                       ▼
                   Redis Cluster
                       │
          Shared Token Buckets
```

If discussing the current project, replace Redis with:

```
ConcurrentHashMap
```

and explain how the design evolves.

---

# Summary

This project follows a clean layered architecture that separates HTTP handling, client identification, business logic, bucket storage, and token management.

Its key architectural strengths include:

- Clear separation of responsibilities
- Interface-driven design
- Thread-safe implementation
- O(1) request processing
- Configurable behavior
- Easy migration to distributed storage
- Production-ready evolution path

The HLD demonstrates how a simple in-memory implementation can serve as the foundation for a scalable enterprise rate limiting system.

---
