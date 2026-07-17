# Rate Limiting Algorithms

## Table of Contents

1. Introduction
2. Algorithm Comparison
3. Fixed Window Counter
4. Sliding Window Log
5. Sliding Window Counter
6. Leaky Bucket
7. Token Bucket
8. Algorithm Comparison Table
9. Which Algorithm Should You Choose?
10. Why This Project Uses Token Bucket
11. Interview Questions
12. Summary

---

# Introduction

Rate limiting can be implemented using multiple algorithms.

Each algorithm has different trade-offs regarding:

- Memory Usage
- CPU Cost
- Burst Handling
- Accuracy
- Scalability
- Ease of Implementation

There is no universally "best" algorithm.

The correct choice depends on the application's requirements.

---

# Quick Comparison

| Algorithm | Burst Support | Accuracy | Memory | Complexity | Production Usage |
|------------|--------------|----------|---------|------------|-----------------|
| Fixed Window | ❌ | Medium | Low | O(1) | Medium |
| Sliding Window Log | ✅ | Very High | High | O(log n) | Medium |
| Sliding Window Counter | Partial | High | Low | O(1) | High |
| Leaky Bucket | ❌ | High | Low | O(1) | High |
| Token Bucket | ✅ | High | Low | O(1) | Very High |

---

# 1. Fixed Window Counter

## Idea

Requests are counted within a fixed time window.

Example

```
Limit = 10 requests/minute

Window

12:00 → 12:01
```

Every request increments a counter.

```
Counter

1

2

3

...

10
```

Request number

```
11
```

↓

Rejected

At

```
12:01
```

Counter resets.

---

## Working

```
Request

↓

Current Window

↓

Increment Counter

↓

Counter > Limit ?

↓

Reject / Allow
```

---

## Advantages

- Extremely simple
- Fast
- O(1)
- Very low memory

---

## Disadvantages

Boundary problem.

Example

```
12:00:59

10 Requests
```

Immediately followed by

```
12:01:00

10 Requests
```

Client effectively sends

```
20 Requests

within

2 Seconds
```

Although the limit is 10/minute.

---

## Production Usage

Good for

- Internal APIs
- Simple dashboards
- Low traffic systems

Not ideal for public APIs.

---

# 2. Sliding Window Log

## Idea

Instead of counting requests,

store the timestamp of every request.

Example

```
Request Times

10:00:01

10:00:05

10:00:20

10:00:32
```

Before accepting a request

remove expired timestamps.

Count remaining timestamps.

---

## Working

```
Request

↓

Remove Old Entries

↓

Count Remaining

↓

Limit Exceeded?

↓

Reject / Allow
```

---

## Advantages

- Very accurate
- No boundary issue
- Fair request distribution

---

## Disadvantages

Stores every request.

Heavy memory usage.

Example

```
1 Million Users

×

100 timestamps

=

100 Million timestamps
```

---

## Complexity

Insertion

```
O(log n)
```

Cleanup

```
O(log n)
```

Memory

```
O(Request Count)
```

---

## Production Usage

Suitable for

- Financial APIs
- Banking
- Authentication

Not suitable for extremely high traffic.

---

# 3. Sliding Window Counter

## Idea

Combines two windows.

Instead of storing every request,

it stores

```
Current Window Count

+

Previous Window Count
```

and calculates a weighted average.

---

## Working

```
Previous Window

↓

Current Window

↓

Weighted Count

↓

Compare with Limit
```

---

## Advantages

- Low memory
- High accuracy
- Fast

---

## Disadvantages

Slightly more complex than Fixed Window.

Still approximates usage.

---

## Production Usage

Very common in API Gateways.

---

# 4. Leaky Bucket

## Idea

Imagine a bucket with a hole.

```
Incoming Requests

↓↓↓↓↓↓↓↓↓↓↓

Bucket

↓

↓

↓

Leak

↓

Constant Rate
```

Requests enter quickly.

Requests leave slowly.

---

## Advantages

Smooth traffic.

Predictable output.

Protects downstream systems.

---

## Disadvantages

Cannot support bursts.

Example

Suppose

```
100 Requests

arrive instantly
```

All must wait.

Even if the server is idle.

---

## Production Usage

Used in

- Network Routers
- Traffic shaping
- Telecom

---

# 5. Token Bucket

## Idea

Instead of storing requests,

store

```
Tokens
```

Each request consumes one token.

Tokens regenerate over time.

Example

```
Capacity = 10

Current Tokens = 10
```

Request

↓

Consume Token

↓

9 Tokens

↓

8 Tokens

↓

7 Tokens

↓

...

↓

0 Tokens

↓

Next Request

↓

Rejected
```

After refill

```
0

↓

2

↓

5

↓

10
```

---

## Working

```
Request

↓

Refill Tokens

↓

Consume Token

↓

Available?

↓

Allow

Reject
```

---

## Advantages

Supports burst traffic.

Very fast.

Very little memory.

Simple implementation.

Excellent scalability.

---

## Disadvantages

Requires refill calculations.

Needs synchronization in multithreaded applications.

---

## Complexity

Lookup

```
O(1)
```

Consume

```
O(1)
```

Memory

```
O(Number of Clients)
```

---

## Production Usage

Very common.

Examples

- AWS API Gateway
- Stripe
- Cloudflare
- Kong
- Envoy
- NGINX

---

# Comparison

| Property | Fixed | Sliding Log | Sliding Counter | Leaky | Token |
|-----------|-------|-------------|-----------------|--------|--------|
| Memory | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Accuracy | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Burst Support | ❌ | ✅ | Partial | ❌ | ✅ |
| CPU Cost | Low | High | Medium | Low | Low |
| Scalability | Good | Medium | Good | Good | Excellent |
| Complexity | Simple | High | Medium | Medium | Medium |

---

# Which Algorithm Should You Choose?

## Fixed Window

Use when

- Simplicity matters
- Internal services
- Low traffic

---

## Sliding Window Log

Use when

- Accuracy is critical
- Traffic is manageable

---

## Sliding Window Counter

Use when

- API Gateway
- Large traffic
- Better fairness

---

## Leaky Bucket

Use when

- Constant output rate required
- Network traffic shaping

---

## Token Bucket

Use when

- REST APIs
- Microservices
- Public APIs
- Burst traffic
- High scalability

---

# Why This Project Uses Token Bucket

This project implements the **Token Bucket Algorithm** because it provides the best balance between performance, memory usage, and user experience.

Reasons:

✅ O(1) operations

✅ Supports burst traffic

✅ Low memory usage

✅ Easy to integrate with Spring Boot

✅ Thread-safe implementation

✅ Suitable for millions of requests

In this project the flow is:

```
HTTP Request

↓

RateLimitInterceptor

↓

ClientIdentifier

↓

BucketStore

↓

TokenBucket

↓

Consume()

↓

Allow / Reject
```

The implementation also includes:

- Lazy refill
- Configurable bucket capacity
- Configurable refill rate
- Automatic cleanup scheduler
- Thread-safe token consumption

These implementation details are covered in:

- `03-Token-Bucket.md`
- `06-Implementation.md`
- `08-Thread-Safety.md`

---

# Interview Questions

### Beginner

- What is rate limiting?
- Which algorithms are commonly used?
- What is burst traffic?
- What is the difference between Leaky Bucket and Token Bucket?

### Intermediate

- Why is Fixed Window inaccurate?
- Why is Sliding Window Log memory intensive?
- Why is Token Bucket O(1)?
- Which algorithm would you choose for login APIs?

### Advanced

- How would you implement distributed Token Bucket?
- Why does Redis work well for Token Bucket?
- How would you scale Token Bucket across multiple application instances?
- How would you prevent race conditions in concurrent token consumption?

---

# Summary

There is no universally best rate-limiting algorithm.

Each algorithm is optimized for different trade-offs.

For this project, **Token Bucket** was selected because it:

- Supports burst traffic
- Maintains constant memory usage
- Provides O(1) request processing
- Is straightforward to integrate with Spring Boot
- Scales well from a single application instance to distributed systems
