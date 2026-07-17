# Thread Safety

## Table of Contents

1. Introduction
2. Why Thread Safety Matters
3. Understanding Concurrency
4. Race Conditions
5. Thread Safety Goals
6. Concurrency Challenges in Rate Limiting
7. Thread Safety Mechanisms Used
8. ConcurrentHashMap
9. AtomicLong
10. ReentrantLock
11. Volatile Fields
12. Why Multiple Synchronization Mechanisms?
13. Request Processing Under Concurrency
14. What Could Go Wrong Without Thread Safety?
15. Performance Considerations
16. Alternative Approaches
17. Best Practices
18. Summary

---

# Introduction

A rate limiter is shared by **all incoming HTTP requests**.

In a production Spring Boot application, hundreds or even thousands of requests may arrive simultaneously.

If multiple threads attempt to update the same bucket at the same time, the bucket can enter an inconsistent state unless proper synchronization is used.

Thread safety ensures that every request observes a correct and consistent bucket state, even under heavy concurrent load.

---

# Why Thread Safety Matters

Consider a bucket with only **one remaining token**.

```
Bucket

Remaining Tokens = 1
```

Now imagine two requests arriving at exactly the same time.

```
Request A

Request B
```

Without synchronization:

```
Thread A reads

1 Token

----------------

Thread B reads

1 Token

----------------

Both consume

1 Token

----------------

Both requests succeed
```

The result:

```
Remaining Tokens = -1
```

This violates the configured rate limit.

Only **one** request should have succeeded.

---

# Understanding Concurrency

Spring Boot handles incoming requests using a thread pool.

```
                Client Requests

     A     B     C     D     E

            │
            ▼

      Spring Thread Pool

     Thread-1
     Thread-2
     Thread-3
     Thread-4
```

Each request is processed independently.

However, multiple threads may reference **the same client bucket**.

```
Thread-1

↓

Client A Bucket

↑

Thread-2
```

That shared state must be protected.

---

# Race Conditions

A race condition occurs when the correctness of a program depends on the order in which multiple threads execute.

Example:

```
Tokens = 5

Thread A

Reads 5

------------

Thread B

Reads 5

------------

Thread A

Stores 4

------------

Thread B

Stores 4
```

Expected

```
3 Tokens
```

Actual

```
4 Tokens
```

One update is lost.

---

# Thread Safety Goals

The implementation should guarantee:

- Correct token count
- No duplicate token consumption
- Accurate refill calculations
- No corrupted bucket state
- High throughput
- Minimal lock contention

---

# Concurrency Challenges in Rate Limiting

A single request performs several operations.

```
Read Timestamp

↓

Calculate Elapsed Time

↓

Generate Tokens

↓

Update Bucket

↓

Consume Token

↓

Update Access Time
```

These operations must appear as **one atomic transaction** to other threads.

Otherwise,

another thread may observe a partially updated bucket.

---

# Thread Safety Mechanisms Used

This implementation combines several synchronization techniques.

| Component | Purpose |
|-----------|---------|
| ConcurrentHashMap | Thread-safe bucket storage |
| AtomicLong | Atomic token counter |
| ReentrantLock | Protect refill and consume operations |
| volatile fields | Visibility of timestamps |

Each mechanism solves a different problem.

---

# ConcurrentHashMap

All buckets are stored in

```java
ConcurrentHashMap<String, TokenBucket>
```

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

Advantages:

- Thread-safe
- High concurrency
- Lock-free reads
- Fine-grained locking
- Excellent scalability

---

## Why Not HashMap?

A normal `HashMap` is not thread-safe.

Concurrent updates can lead to:

- Lost entries
- Corrupted internal structure
- Infinite loops (historically in older JDKs)
- Unexpected exceptions

---

## computeIfAbsent()

Bucket creation uses

```java
computeIfAbsent()
```

Instead of

```java
if(bucket == null)
```

This ensures that two threads cannot accidentally create two buckets for the same client.

---

# AtomicLong

The available token count is stored using

```java
AtomicLong
```

Advantages

- Atomic increment/decrement
- Lock-free reads
- Efficient CAS (Compare-And-Set) operations
- Low overhead

Example

```
Tokens = 10

↓

decrementAndGet()

↓

9
```

No explicit synchronization is required for simple token updates.

---

# ReentrantLock

Updating a bucket involves more than changing a single number.

The implementation must update:

- Available tokens
- Last refill timestamp
- Last access timestamp

These values are logically connected.

The implementation protects this critical section using

```java
ReentrantLock
```

Flow

```
Acquire Lock

↓

Refill Bucket

↓

Consume Token

↓

Update Timestamp

↓

Release Lock
```

This guarantees that another thread never observes a partially updated bucket.

---

## Why Not Synchronize Everything?

Locking the entire application would severely reduce throughput.

Instead,

each bucket owns its own lock.

```
Bucket A

Lock A

----------------

Bucket B

Lock B

----------------

Bucket C

Lock C
```

Requests for different clients proceed in parallel.

Only requests targeting the **same bucket** compete for the same lock.

This greatly improves scalability.

---

# Volatile Fields

Some timestamps are declared as

```java
volatile
```

Purpose:

Ensure that updates performed by one thread become immediately visible to other threads.

Without `volatile`,

one thread may continue using stale timestamp values from its CPU cache.

---

# Why Multiple Synchronization Mechanisms?

A common interview question is:

> Why not just use one synchronization mechanism?

Because each mechanism solves a different problem.

| Mechanism | Solves |
|-----------|--------|
| ConcurrentHashMap | Concurrent bucket storage |
| AtomicLong | Atomic numeric operations |
| ReentrantLock | Multi-variable consistency |
| volatile | Memory visibility |

Using only one mechanism would either be insufficient or unnecessarily expensive.

---

# Request Processing Under Concurrency

Suppose three requests arrive simultaneously.

```
Request A

Request B

Request C
```

Execution

```
Find Bucket

↓

Acquire Bucket Lock

↓

Lazy Refill

↓

Consume Token

↓

Release Lock
```

The requests are processed safely.

Requests for **other clients** continue without waiting.

---

# What Could Go Wrong Without Thread Safety?

## Double Token Consumption

```
1 Token

↓

2 Requests

↓

2 Allowed
```

Incorrect.

---

## Lost Updates

```
5 Tokens

↓

Thread A

↓

4

----------------

Thread B

↓

4
```

Should be

```
3
```

---

## Negative Token Count

Without synchronization,

multiple threads may decrement simultaneously.

```
Tokens

0

↓

-1

↓

-2
```

Impossible in a correct implementation.

---

## Incorrect Retry Time

If refill timestamps become inconsistent,

clients may receive incorrect

```
Retry-After
```

values.

---

## Corrupted Bucket State

One thread may refill while another consumes,

producing an invalid combination of

- Tokens
- Refill timestamp
- Last access time

---

# Performance Considerations

Thread safety always introduces some overhead.

The implementation minimizes this by:

- Locking only individual buckets
- Using lock-free reads where possible
- Using `ConcurrentHashMap`
- Avoiding global synchronization
- Performing lazy refill instead of scheduled updates

Result

```
High Throughput

+

Correctness

+

Scalability
```

---

# Alternative Approaches

## synchronized

Simple but less flexible.

Suitable for smaller applications.

---

## ReadWriteLock

Useful when reads greatly outnumber writes.

Not necessary here because every request modifies the bucket.

---

## StampedLock

Offers optimistic reads.

Adds complexity without significant benefit for this use case.

---

## Lock-Free Design

Possible using advanced CAS algorithms.

Much more difficult to implement correctly.

Not justified for this project.

---

# Best Practices

When implementing concurrent rate limiters:

- Protect shared mutable state.
- Keep critical sections as small as possible.
- Avoid global locks.
- Prefer concurrent collections.
- Use monotonic clocks for elapsed-time calculations.
- Test under concurrent load.
- Minimize unnecessary synchronization.

---

# Summary

This implementation achieves thread safety through a combination of carefully selected concurrency primitives.

Key characteristics include:

- `ConcurrentHashMap` for concurrent bucket storage
- `AtomicLong` for efficient token management
- `ReentrantLock` to protect refill and consume operations
- `volatile` fields for memory visibility
- Per-bucket locking to maximize throughput
- No global synchronization bottlenecks

The result is a rate limiter that remains correct under concurrent access while maintaining high performance and scalability.

---
