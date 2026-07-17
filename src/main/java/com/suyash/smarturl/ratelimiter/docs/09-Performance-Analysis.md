# Performance Analysis

## Table of Contents

1. Introduction
2. Performance Goals
3. Time Complexity
4. Memory Complexity
5. Request Processing Cost
6. Scalability Analysis
7. Concurrent Performance
8. Lazy Refill Performance
9. Bucket Storage Performance
10. Cleanup Performance
11. Performance Bottlenecks
12. Benchmark Expectations
13. Comparison with Other Algorithms
14. JVM Considerations
15. Production Recommendations
16. Summary

---

# Introduction

A rate limiter sits in the critical path of **every incoming request**.

This means even a small inefficiency is multiplied across thousands or millions of requests.

For example,

```
1000 Requests/Second

↓

86 Million Requests/Day
```

If each request performs unnecessary work, CPU usage and latency increase significantly.

Therefore, the implementation is designed so that almost every operation executes in **constant time (O(1))**.

---

# Performance Goals

The implementation was designed with the following objectives:

- Constant-time request processing
- Low memory footprint
- Minimal synchronization overhead
- High concurrency
- No unnecessary background processing
- Predictable latency
- Easy horizontal scalability

---

# Time Complexity

## Request Processing

Each request performs the following operations:

| Operation | Complexity |
|-----------|-----------:|
| Identify Client | O(1) |
| Lookup Bucket | O(1) |
| Lazy Refill Calculation | O(1) |
| Consume Token | O(1) |
| Return Decision | O(1) |

Overall complexity

```
O(1)
```

The processing time remains constant regardless of:

- Number of requests
- Number of buckets
- Application uptime

---

## Bucket Creation

A new bucket is created only once per client.

```
ConcurrentHashMap

↓

computeIfAbsent()

↓

Create Bucket
```

Complexity

```
O(1)
```

---

## Token Consumption

The bucket already exists.

```
Lookup Bucket

↓

Consume Token

↓

Return Result
```

Complexity

```
O(1)
```

No loops or request history traversal is required.

---

# Memory Complexity

Each active client owns one bucket.

```
Client A

↓

Bucket A

----------------

Client B

↓

Bucket B
```

Therefore,

```
Memory

=

O(Number of Active Clients)
```

Unlike Sliding Window Log, memory usage **does not increase with request count**.

---

## Example

```
100 Clients

↓

100 Buckets
```

```
10,000 Clients

↓

10,000 Buckets
```

Even if each client sends millions of requests, the number of buckets remains unchanged.

---

# Request Processing Cost

Each request performs only a small number of operations.

```
Receive Request

↓

Extract Client

↓

Lookup Bucket

↓

Lazy Refill

↓

Consume Token

↓

Return
```

No database queries.

No network calls.

No disk I/O.

Everything occurs in memory.

---

# Scalability Analysis

## Single JVM

Current architecture

```
Application

↓

ConcurrentHashMap

↓

Buckets
```

Suitable for:

- Development
- Small services
- Internal APIs
- Single-instance deployments

---

## Multiple JVMs

```
Load Balancer

↓

Instance A

↓

Instance B

↓

Instance C
```

Each instance maintains its own buckets.

Result

```
Rate Limit

is enforced

per instance
```

For true global rate limiting,

a shared data store such as Redis is required.

---

# Concurrent Performance

The implementation is optimized for concurrent access.

### Bucket Storage

```
ConcurrentHashMap
```

provides

- Lock-free reads
- Fine-grained synchronization
- High throughput

---

### Bucket Locking

Each bucket owns its own lock.

```
Client A

↓

Lock A

----------------

Client B

↓

Lock B
```

Only requests for the **same client** compete for the same lock.

Different clients execute in parallel.

---

## Contention

Suppose

```
1000 Clients

↓

1000 Buckets
```

Most requests acquire different locks.

Contention remains very low.

---

# Lazy Refill Performance

A common implementation strategy is to refill every bucket periodically.

```
Every Second

↓

Refill All Buckets
```

This wastes CPU cycles for inactive clients.

Instead,

the current implementation performs

```
Request

↓

Refill Only That Bucket
```

Advantages

- No scheduler overhead
- No scanning inactive buckets
- Better CPU utilization
- Better scalability

---

# Bucket Storage Performance

Bucket lookup uses

```java
ConcurrentHashMap
```

Operations:

```
get()

put()

computeIfAbsent()
```

Average complexity

```
O(1)
```

Lookup performance remains stable even with thousands of buckets.

---

# Cleanup Performance

Inactive buckets are periodically removed.

```
Buckets

↓

Expired?

↓

Remove
```

Cleanup complexity

```
O(Number of Buckets)
```

However,

cleanup executes infrequently and outside the request path.

Therefore,

it does not affect request latency.

---

# Performance Bottlenecks

Although the implementation is efficient, every system has limits.

## Hot Clients

Suppose one client sends

```
50,000 Requests/Second
```

All requests target the same bucket.

```
Client

↓

One Bucket

↓

One Lock
```

Those requests serialize on the bucket lock.

This is expected and acceptable because rate limiting is inherently client-specific.

---

## Memory Growth

Without cleanup,

```
Millions of Clients

↓

Millions of Buckets
```

Memory usage would continuously increase.

The cleanup process prevents this.

---

## Distributed Systems

In-memory buckets cannot coordinate limits across multiple JVMs.

Future improvement

```
ConcurrentHashMap

↓

Redis
```

---

# Benchmark Expectations

Because every request performs only constant-time operations,

the implementation is expected to scale well.

Typical characteristics:

| Metric | Expected Behaviour |
|---------|-------------------|
| Request Complexity | O(1) |
| Memory Growth | O(Active Clients) |
| Lock Scope | Per Bucket |
| Database Access | None |
| Network Calls | None |
| Disk I/O | None |
| Average Latency Overhead | Very Low |

The exact throughput depends on:

- CPU
- JVM configuration
- Available memory
- Thread pool size
- Number of concurrent clients

---

# Comparison with Other Algorithms

| Algorithm | Request Time | Memory | Burst Support |
|------------|-------------:|-------:|--------------|
| Fixed Window | O(1) | O(Clients) | ❌ |
| Sliding Window Log | O(log n) | O(Requests) | ✅ |
| Sliding Window Counter | O(1) | O(Clients) | Partial |
| Leaky Bucket | O(1) | O(Clients) | ❌ |
| **Token Bucket** | **O(1)** | **O(Clients)** | ✅ |

Token Bucket provides an excellent balance between performance and flexibility.

---

# JVM Considerations

Several JVM features contribute to good performance.

## ConcurrentHashMap

Optimized for high concurrency.

---

## AtomicLong

Avoids unnecessary locking for numeric updates.

---

## ReentrantLock

Locks only the bucket being modified.

---

## System.nanoTime()

Provides efficient and accurate elapsed-time calculations.

---

## Garbage Collection

Expired bucket cleanup reduces long-lived object retention, helping the JVM reclaim memory more effectively over time.

---

# Production Recommendations

For production deployments:

### Enable Cleanup

Prevent stale buckets from consuming memory.

---

### Tune Bucket Capacity

Higher capacities allow larger bursts but consume slightly more memory.

---

### Configure Refill Rate Carefully

Choose values that match expected traffic patterns.

---

### Monitor Metrics

Track:

- Active buckets
- Rejected requests
- Remaining tokens
- Cleanup frequency
- Average request latency

---

### Use Redis for Clusters

When multiple application instances are deployed,

replace in-memory bucket storage with a shared datastore.

---

# Summary

The implementation is optimized for predictable, low-latency request processing.

Key performance characteristics include:

- O(1) request processing
- O(1) bucket lookup
- O(1) token consumption
- O(Active Clients) memory usage
- In-memory processing only
- Per-bucket locking for high concurrency
- Lazy refill to eliminate unnecessary background work
- Cleanup mechanism to control memory growth

These characteristics make the implementation suitable for high-throughput Spring Boot applications while keeping the design simple and maintainable.

---
