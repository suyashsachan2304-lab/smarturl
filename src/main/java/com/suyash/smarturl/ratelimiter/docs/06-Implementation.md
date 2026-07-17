# Implementation Guide

## Table of Contents

1. Introduction
2. Implementation Overview
3. Component Interaction
4. Bucket Interface
5. TokenBucket
6. BucketRefillStrategy
7. BucketStore
8. RateLimiterService
9. TokenBucketRateLimiterService
10. ClientIdentifier
11. RateLimitInterceptor
12. Thread Safety
13. Configuration Flow
14. Cleanup Process
15. Design Decisions
16. End-to-End Flow
17. Summary

---

# Introduction

This document explains how the Token Bucket rate limiter is implemented in this project.

Unlike the previous documents that focused on theory, this guide maps every important class directly to its implementation.

The implementation has been designed with the following goals:

- Thread safety
- High performance
- Low memory usage
- Spring Boot integration
- Production-ready design
- Easy extensibility

---

# Implementation Overview

The implementation consists of several collaborating components.

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
      TokenBucketRateLimiterService
                      │
                      ▼
             BucketStore
                      │
                      ▼
              TokenBucket
                      │
         ┌────────────┴────────────┐
         │                         │
         ▼                         ▼
     Allow Request          Reject Request
```

Each class owns exactly one responsibility.

---

# Component Interaction

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
          │
          ▼
BucketRefillStrategy
```

Notice that business logic never appears inside the interceptor.

---

# Bucket Interface

The `Bucket` interface defines the contract that every bucket implementation must follow.

Responsibilities:

- Consume tokens
- Report remaining tokens
- Calculate retry time
- Determine expiration

Current methods:

```java
tryConsume()

getRemainingTokens()

getRetryAfterSeconds()

isExpired()
```

### Why use an interface?

This makes the implementation extensible.

Future implementations could include:

- Sliding Window Bucket
- Redis Bucket
- Distributed Bucket
- Leaky Bucket

without changing the service layer.

---

# TokenBucket

`TokenBucket` is the heart of the project.

It maintains the state for one client.

### Stored State

```
capacity

availableTokens

refillTokens

refillDuration

bucketExpiry

lastRefillTimestamp

lastAccessTimestamp
```

---

## AtomicLong

Available tokens are stored using

```java
AtomicLong
```

Advantages:

- Lock-free reads
- Atomic updates
- Very low overhead
- Safe visibility across threads

---

## ReentrantLock

Although `AtomicLong` provides atomic operations, refilling the bucket requires updating multiple fields together.

The implementation protects these operations using

```java
ReentrantLock
```

The lock ensures that

- refill calculation
- token update
- timestamp update

occur as one atomic operation.

Without locking, multiple threads could refill the bucket simultaneously and produce incorrect token counts.

---

## Lazy Refill

This implementation does **not** use a background thread.

Instead,

```
Request

↓

refill()

↓

consume()
```

Every request first calls

```java
refill()
```

Only if enough time has elapsed are new tokens generated.

Advantages:

- Lower CPU usage
- No scheduled refill thread
- Simpler architecture
- Better scalability

---

## Nanosecond Precision

The implementation uses

```java
System.nanoTime()
```

instead of

```java
System.currentTimeMillis()
```

Benefits:

- Monotonic time source
- Not affected by system clock changes
- Higher precision
- Better elapsed-time calculations

---

# BucketRefillStrategy

Instead of embedding refill calculations inside `TokenBucket`, the logic is extracted into a dedicated utility class.

Responsibilities:

- Calculate elapsed refill cycles
- Calculate new token count

Main methods:

```java
calculateRefillCycles()

refillTokens()
```

### Benefits

- Single responsibility
- Stateless implementation
- Easy unit testing
- Reusable logic
- Cleaner TokenBucket class

---

# BucketStore

`BucketStore` manages all client buckets.

Internally it uses

```java
ConcurrentHashMap<String, TokenBucket>
```

Structure

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

---

## computeIfAbsent()

Instead of

```java
if(bucket == null)
```

the implementation uses

```java
computeIfAbsent()
```

Advantages:

- Atomic creation
- No race conditions
- Cleaner code
- Better concurrency

---

## Additional Responsibilities

BucketStore also provides

```
remove()

removeExpiredBuckets()

size()

getBuckets()
```

making it suitable for monitoring and cleanup.

---

# RateLimiterService

This interface separates business logic from implementation.

Current responsibilities:

```
allowRequest()

getRetryAfter()

getRemainingTokens()

cleanup()
```

Because callers depend on the interface,

the implementation can be replaced without modifying the interceptor.

---

# TokenBucketRateLimiterService

This is the primary business layer.

Responsibilities:

1. Read configuration
2. Retrieve bucket
3. Consume token
4. Return decision
5. Cleanup expired buckets

Request flow

```
Client ID

↓

BucketStore

↓

TokenBucket

↓

tryConsume()

↓

Return Result
```

Notice that this class never performs token calculations itself.

All bucket logic remains inside `TokenBucket`.

---

## Configuration Driven

Instead of hardcoding values,

the service retrieves configuration from

```
RateLimitProperties
```

Example

```
Capacity

Refill Tokens

Refill Duration

Bucket Expiry
```

This keeps the implementation flexible.

---

# ClientIdentifier

Every client needs a unique bucket.

The `ClientIdentifier` component is responsible for generating that identifier.

Current strategy:

```
X-Forwarded-For

↓

Remote IP
```

If

```
trustProxy = true
```

the implementation first checks

```
X-Forwarded-For
```

Otherwise

```
request.getRemoteAddr()
```

is used.

Future extensions may support

- JWT Subject
- API Key
- OAuth Client ID
- User ID

without modifying the interceptor.

---

# RateLimitInterceptor

The interceptor integrates the rate limiter into Spring MVC.

Execution order

```
Incoming Request

↓

preHandle()

↓

Identify Client

↓

Check Rate Limit

↓

Allow / Reject
```

---

## Excluded Paths

Before checking the bucket,

the interceptor verifies whether the request matches an excluded path.

Example

```
/actuator/**

/swagger-ui/**

/v3/api-docs/**
```

Excluded endpoints bypass rate limiting.

---

## Successful Request

If allowed

```
Consume Token

↓

Add Header

↓

Continue
```

Current implementation adds

```
X-RateLimit-Remaining
```

to every successful response.

---

## Rejected Request

When the bucket is empty

```
Retry-After

↓

RateLimitExceededException

↓

HTTP 429
```

The client immediately knows how long to wait before retrying.

---

# Thread Safety

The implementation combines several concurrency mechanisms.

| Component | Purpose |
|-----------|---------|
| ConcurrentHashMap | Concurrent bucket storage |
| AtomicLong | Atomic token count |
| volatile fields | Timestamp visibility |
| ReentrantLock | Atomic refill + consume |

This combination ensures correctness while keeping contention localized to each bucket.

Multiple clients can be processed in parallel because each client owns an independent bucket.

---

# Configuration Flow

Application startup

```
application.properties

↓

RateLimitProperties

↓

Spring Bean

↓

TokenBucketRateLimiterService

↓

TokenBucket Creation
```

Changing the configuration requires no code changes.

---

# Cleanup Process

Inactive buckets should not remain in memory forever.

The implementation provides

```
removeExpiredBuckets()
```

which removes buckets whose inactivity exceeds the configured expiration period.

Flow

```
Bucket

↓

Inactive

↓

Expired

↓

Removed
```

This prevents memory usage from continuously increasing.

---

# Design Decisions

## Why AtomicLong?

Efficient atomic updates for token counts.

---

## Why ReentrantLock?

Refilling updates multiple variables together and must be atomic.

---

## Why ConcurrentHashMap?

Allows multiple clients to access different buckets concurrently.

---

## Why Lazy Refill?

Reduces CPU usage by avoiding scheduled refill tasks.

---

## Why System.nanoTime()?

Provides accurate elapsed-time calculations independent of system clock changes.

---

## Why Interfaces?

Makes the algorithm replaceable without affecting callers.

---

## Why Separate ClientIdentifier?

Authentication strategy and rate limiting remain independent.

---

# End-to-End Flow

```
HTTP Request

↓

RateLimitInterceptor

↓

ClientIdentifier

↓

TokenBucketRateLimiterService

↓

BucketStore

↓

TokenBucket

↓

Lazy Refill

↓

Consume Token

↓

Decision

↓

Controller

OR

HTTP 429
```

---

# Summary

The implementation combines simplicity with production-oriented design.

Key implementation characteristics include:

- Token Bucket algorithm
- Lazy refill strategy
- Nanosecond precision timing
- One bucket per client
- `ConcurrentHashMap` for bucket storage
- `AtomicLong` for token count
- `ReentrantLock` for atomic refill and consume
- Interface-based service layer
- Spring Boot interceptor integration
- Automatic cleanup support
- Externalized configuration
- HTTP 429 responses with `Retry-After` and `X-RateLimit-Remaining` headers
