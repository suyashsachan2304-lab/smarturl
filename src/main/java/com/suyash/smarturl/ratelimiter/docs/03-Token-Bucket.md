# Token Bucket Algorithm

## Table of Contents

1. Introduction
2. What is a Token Bucket?
3. Core Concepts
4. How the Algorithm Works
5. Token Lifecycle
6. Burst Traffic
7. Lazy Refill Strategy
8. Mathematical Model
9. Step-by-Step Example
10. Advantages
11. Limitations
12. Time Complexity
13. Memory Complexity
14. Concurrency Considerations
15. Edge Cases
16. Production Usage
17. Mapping to This Project
18. Summary

---

# Introduction

The **Token Bucket Algorithm** is one of the most widely used rate limiting algorithms in modern distributed systems.

It provides an excellent balance between:

- Performance
- Memory usage
- Fairness
- Scalability
- User experience

Unlike Fixed Window or Leaky Bucket, Token Bucket allows **short bursts of traffic** while still enforcing an average request rate over time.

This flexibility makes it the preferred choice for API gateways, cloud platforms, and microservices.

---

# What is a Token Bucket?

Imagine a bucket that holds a fixed number of tokens.

```
      +----------------+
      | ● ● ● ● ● ● ● |
      | ● ● ● ● ● ● ● |
      +----------------+
```

Every API request requires **one token**.

If a token is available:

```
Request
   │
Consume Token
   │
Allowed
```

If no tokens remain:

```
Request
   │
No Token
   │
Rejected (HTTP 429)
```

Tokens are automatically added back to the bucket over time.

---

# Core Concepts

## Bucket Capacity

The maximum number of tokens that can be stored.

Example

```
Capacity = 20
```

Even if the application is idle for hours,

the bucket never stores more than

```
20 Tokens
```

---

## Tokens

Each token represents permission to process exactly one request.

```
5 Tokens

↓

5 Requests Allowed
```

---

## Refill Rate

Determines how quickly tokens return.

Example

```
10 Tokens

Every Minute
```

If the bucket becomes empty,

after one minute,

```
10 Tokens

will be available again.
```

---

## Current Token Count

The bucket continuously tracks the number of available tokens.

Example

```
Capacity = 10

Current = 7
```

After one request

```
Current = 6
```

---

# How the Algorithm Works

```
Incoming Request

↓

Calculate New Tokens

↓

Update Bucket

↓

Tokens Available?

↓

YES
↓

Consume Token

↓

Allow Request

----------------

NO

↓

Reject Request
```

Notice that the bucket is updated **before** checking availability.

---

# Token Lifecycle

A token continuously moves through four states.

```
Generated

↓

Stored

↓

Consumed

↓

Generated Again
```

Example

```
Bucket Capacity = 5

Tokens

5

↓

4

↓

3

↓

2

↓

1

↓

0

↓

Refill

↓

5
```

---

# Burst Traffic

One of the biggest advantages of Token Bucket is burst support.

Suppose

```
Capacity = 10
```

No requests arrive for several minutes.

The bucket becomes completely full.

```
10 Tokens
```

Now the client suddenly sends

```
10 Requests
```

All requests succeed immediately.

```
10

↓

9

↓

8

↓

...

↓

0
```

This improves user experience because occasional spikes are allowed.

---

## Why Burst Support Matters

Examples:

- User refreshes a dashboard
- Mobile application reconnects
- Browser loads multiple resources
- Checkout workflow triggers multiple APIs

Burst traffic is normal behavior.

Token Bucket handles it naturally.

---

# Lazy Refill Strategy

This project uses **lazy refill**.

Instead of continuously refilling buckets using a background thread,

tokens are regenerated **only when a request arrives**.

Example

Current Time

```
10:00
```

Bucket

```
5 Tokens
```

No requests occur for 10 minutes.

Nothing happens.

At

```
10:10
```

a request arrives.

Only then does the application calculate

```
Elapsed Time

↓

Tokens to Add

↓

Updated Bucket
```

Advantages

- No refill scheduler
- Less CPU usage
- Simpler implementation
- Better scalability

This is exactly how the `TokenBucket` implementation in this project works.

---

# Mathematical Model

Assume

```
Capacity = C

Current Tokens = T

Refill Rate = R Tokens/Second

Elapsed Time = Δt
```

New tokens generated

```
Generated = R × Δt
```

Updated bucket

```
New Tokens

=

min(

Capacity,

Current Tokens

+

Generated

)
```

The bucket never exceeds its configured capacity.

---

# Step-by-Step Example

Configuration

```
Capacity = 5

Refill = 1 Token/Second
```

### Initial State

```
Tokens = 5
```

---

### Request 1

```
5

↓

4
```

Allowed

---

### Request 2

```
4

↓

3
```

Allowed

---

### Request 3

```
3

↓

2
```

Allowed

---

### Wait 2 Seconds

Generated

```
2 Tokens
```

Bucket

```
2

↓

4
```

---

### Four More Requests

```
4

↓

3

↓

2

↓

1

↓

0
```

Allowed

---

### Next Request

```
0 Tokens
```

↓

Rejected

↓

HTTP 429

---

# Advantages

## Supports Burst Traffic

Allows temporary traffic spikes without violating the average rate.

---

## Constant Memory

Only one bucket per client.

No request history is stored.

---

## Fast Operations

Finding a bucket

```
O(1)
```

Consuming a token

```
O(1)
```

---

## Excellent Scalability

Suitable for

- REST APIs
- API Gateways
- Microservices
- Cloud Services

---

## Easy Configuration

Only a few parameters are required.

```
Capacity

Refill Rate
```

---

# Limitations

## Single JVM

An in-memory implementation only works inside one application instance.

For multiple instances,

a distributed data store is required.

---

## Time Accuracy

The algorithm depends on accurate elapsed-time calculations.

Applications should use a monotonic clock where possible to avoid issues caused by system time adjustments.

---

## Synchronization

Multiple threads may attempt to consume tokens simultaneously.

Without proper synchronization,

multiple requests could incorrectly consume the same token.

This project solves the problem using thread-safe bucket operations.

---

# Time Complexity

| Operation | Complexity |
|-----------|-----------:|
| Find Bucket | O(1) |
| Refill Bucket | O(1) |
| Consume Token | O(1) |
| Create Bucket | O(1) |

---

# Memory Complexity

One bucket is stored for each active client.

```
Memory

=

O(Number of Clients)
```

Example

```
1000 Clients

↓

1000 Buckets
```

Memory does **not** increase with the number of requests.

---

# Concurrency Considerations

Multiple requests may arrive simultaneously.

Example

```
Request A

Request B

Request C

↓

Same Bucket
```

Without synchronization

```
3 Threads

↓

Read

↓

5 Tokens

↓

All Consume

↓

Incorrect State
```

Thread safety ensures

```
Only One Thread

Updates

Bucket State

At A Time
```

In this project, bucket operations are protected to prevent race conditions while keeping contention localized to each bucket.

---

# Edge Cases

## Bucket Overflow

```
Capacity = 20

Current = 19

Generated = 5
```

Result

```
20

NOT

24
```

---

## Empty Bucket

```
Tokens = 0
```

↓

Reject Request

---

## Long Idle Period

Bucket remains idle.

Next request

↓

Lazy Refill

↓

Bucket becomes full

---

## Very High Request Rate

Requests exceeding available tokens are rejected until enough tokens have been regenerated.

---

# Production Usage

Token Bucket is widely used by:

- AWS API Gateway
- Envoy Proxy
- Kong Gateway
- Cloudflare
- Stripe
- Many Spring Boot services
- Microservice platforms

Its popularity comes from combining predictable performance with good user experience.

---

# Mapping to This Project

This project implements the Token Bucket algorithm using several collaborating components.

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

Lazy Refill

↓

Consume Token

↓

Allow / Reject
```

Implementation highlights:

- One bucket per client
- Configurable bucket capacity
- Configurable refill rate
- Lazy refill strategy
- Thread-safe token consumption
- Automatic cleanup of inactive buckets
- Spring Boot integration through an interceptor
- HTTP 429 responses when limits are exceeded

The implementation details of each class are explained in:

- `06-Implementation.md`
- `08-Thread-Safety.md`

---

# Summary

The Token Bucket algorithm is an efficient and production-proven rate limiting strategy.

Its key characteristics are:

- Supports burst traffic
- Constant memory usage
- O(1) request processing
- Lazy refill for efficiency
- Easy integration with Spring Boot
- Well suited for concurrent applications

These properties make it the ideal choice for the rate limiter implemented in this project.