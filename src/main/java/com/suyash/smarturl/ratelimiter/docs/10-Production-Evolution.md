# Production Evolution

## Table of Contents

1. Introduction
2. Current Architecture
3. Current Limitations
4. Evolution Roadmap
5. Stage 1 – Single JVM (Current)
6. Stage 2 – Multiple Application Instances
7. Stage 3 – Distributed Bucket Storage
8. Stage 4 – API Gateway Integration
9. Stage 5 – Monitoring & Observability
10. Stage 6 – Dynamic Configuration
11. Stage 7 – Advanced Rate Limiting
12. Stage 8 – Enterprise Architecture
13. Production Best Practices
14. Common Pitfalls
15. Final Architecture
16. Summary

---

# Introduction

The current implementation is an **in-memory Token Bucket rate limiter** designed for a single Spring Boot application.

It is ideal for:

- Learning
- Local development
- Internal tools
- Small deployments
- Single-instance applications

However, production systems often consist of multiple application instances running behind a load balancer.

This document explains how the current implementation can evolve into a production-grade distributed rate limiting system.

---

# Current Architecture

Current implementation

```
               Client
                  │
                  ▼
          Spring Boot Application
                  │
                  ▼
          ConcurrentHashMap
                  │
                  ▼
          TokenBucket Objects
```

Each application instance owns its own buckets.

---

## Advantages

- Extremely fast
- No network latency
- Very low memory overhead
- Easy debugging
- Simple implementation

---

## Limitations

- Single JVM only
- Buckets disappear after restart
- Cannot enforce global limits
- No synchronization across instances

---

# Current Limitations

Suppose we deploy

```
Application A

Application B
```

behind

```
Load Balancer
```

```
          Client

             │

      Load Balancer

       │          │

       ▼          ▼

   Instance A   Instance B
```

If the client sends

```
20 Requests
```

the load balancer may distribute

```
10

↓

Instance A

10

↓

Instance B
```

Each instance has its own bucket.

The client effectively receives

```
20 Requests
```

instead of

```
10 Requests
```

This is called **per-instance rate limiting**, not **global rate limiting**.

---

# Evolution Roadmap

The implementation can evolve in several stages.

```
Single JVM

↓

Multiple JVMs

↓

Redis Storage

↓

API Gateway

↓

Metrics

↓

Dynamic Configuration

↓

Enterprise Rate Limiting
```

Each stage builds upon the previous one.

---

# Stage 1 – Single JVM (Current)

Current implementation

```
HTTP Request

↓

Interceptor

↓

ConcurrentHashMap

↓

TokenBucket
```

Characteristics

- Fastest possible lookup
- No external dependencies
- Simple deployment
- O(1) operations

Best suited for

- Development
- Small applications
- Internal APIs

---

# Stage 2 – Multiple Application Instances

Architecture

```
                Client

                   │

             Load Balancer

          ┌────────┴────────┐

          ▼                 ▼

     Spring Boot A     Spring Boot B

          │                 │

     Local Buckets     Local Buckets
```

Benefits

- Higher availability
- Horizontal scaling
- Better fault tolerance

Problem

Buckets are independent.

Clients can exceed the intended global limit.

---

# Stage 3 – Distributed Bucket Storage

Replace

```
ConcurrentHashMap
```

with

```
Redis
```

Architecture

```
               Client

                  │

           Load Balancer

          ┌────────┴────────┐

          ▼                 ▼

     Application A     Application B

           │                │

           └──────┬─────────┘

                  ▼

                Redis

                  │

             Token Buckets
```

Benefits

- Global rate limiting
- Shared bucket state
- Survives application restart
- Supports horizontal scaling

---

## Why Redis?

Redis offers

- Extremely fast in-memory operations
- Atomic commands
- Expiration support
- High availability
- Replication
- Clustering

Most production rate limiters rely on Redis or similar distributed stores.

---

# Stage 4 – API Gateway Integration

Instead of rate limiting inside every service,

it can be moved to an API Gateway.

Architecture

```
Internet

↓

API Gateway

↓

Rate Limiter

↓

Microservices
```

Advantages

- Single enforcement point
- No duplicated code
- Consistent limits
- Better security

Popular gateways

- Spring Cloud Gateway
- Kong
- Envoy
- NGINX
- AWS API Gateway

---

# Stage 5 – Monitoring & Observability

A production system should expose operational metrics.

Useful metrics include:

- Active buckets
- Total requests
- Allowed requests
- Rejected requests
- Average remaining tokens
- Cleanup executions
- Bucket creation rate

Architecture

```
Application

↓

Micrometer

↓

Prometheus

↓

Grafana Dashboard
```

Example dashboard

```
Allowed Requests

Rejected Requests

Average Latency

429 Responses

Active Buckets
```

---

# Stage 6 – Dynamic Configuration

Current implementation

```
application.properties
```

↓

Restart Required

A production system may instead load limits dynamically.

```
Configuration Server

↓

Application

↓

Refresh

↓

New Limits
```

Benefits

- No redeployment
- Runtime updates
- Environment-specific policies

Possible sources

- Spring Cloud Config
- Consul
- ZooKeeper
- Kubernetes ConfigMaps
- Database-backed configuration

---

# Stage 7 – Advanced Rate Limiting

Production systems often require multiple policies.

## Per Endpoint

```
/login

↓

5 Requests / Minute

----------------

/search

↓

100 Requests / Minute
```

---

## Per User Role

```
Anonymous

↓

20 Requests

----------------

Premium

↓

500 Requests

----------------

Administrator

↓

Unlimited
```

---

## Per API Key

Each API key maintains its own bucket.

```
API Key A

↓

Bucket A

----------------

API Key B

↓

Bucket B
```

---

## Weighted Requests

Not every request needs to consume one token.

Example

```
Simple GET

↓

1 Token

----------------

Large Export

↓

10 Tokens
```

---

# Stage 8 – Enterprise Architecture

A mature deployment might look like this:

```
                    Internet

                        │

                 CDN / WAF

                        │

                  Load Balancer

                        │

                 API Gateway

                        │

              Authentication

                        │

          Distributed Rate Limiter

                        │

                    Redis Cluster

                        │

              Spring Boot Services

                        │

             Database / Cache / Queue
```

This architecture supports:

- Millions of users
- Multiple regions
- Horizontal scaling
- High availability
- Centralized enforcement

---

# Production Best Practices

### Use Distributed Storage

Use Redis for multiple application instances.

---

### Monitor 429 Responses

A sudden increase may indicate:

- Abuse
- Misconfigured limits
- Increased traffic

---

### Configure Sensible Limits

Avoid limits that are

- Too strict
- Too permissive

Tune based on actual usage.

---

### Expire Inactive Buckets

Remove stale buckets to control memory usage.

---

### Protect Critical APIs

Endpoints such as

```
/login

/payment

/checkout
```

should generally have stricter limits than read-only endpoints.

---

### Combine with Authentication

Authenticated users should be identified by stable identities such as user ID or API key instead of relying solely on IP addresses.

---

# Common Pitfalls

## Per-Instance Buckets

Independent in-memory buckets do not provide global rate limiting.

---

## Missing Cleanup

Unused buckets remain in memory indefinitely.

---

## Trusting Spoofed Headers

Only honor `X-Forwarded-For` when requests come through trusted reverse proxies.

---

## Using System Clock Incorrectly

Elapsed-time calculations should use a monotonic clock such as `System.nanoTime()`.

---

## Global Locking

Avoid a single application-wide lock.

Per-bucket locking provides much better scalability.

---

# Final Architecture

A fully evolved production system may resemble:

```
                    Client

                       │

                 CDN / WAF

                       │

               Load Balancer

                       │

                API Gateway

                       │

           Authentication Service

                       │

           Distributed Rate Limiter

                       │

                 Redis Cluster

                       │

          Spring Boot Microservices

             │        │        │

             ▼        ▼        ▼

         Service A Service B Service C

                       │

                 Kafka / Database
```

Characteristics

- Global rate limiting
- Horizontal scalability
- Fault tolerance
- Centralized policy management
- Shared bucket storage
- Rich observability

---

# Summary

The current implementation is intentionally designed as a solid foundation rather than a complete enterprise platform.

It evolves naturally through the following stages:

| Stage | Architecture | Primary Benefit |
|--------|--------------|-----------------|
| 1 | In-Memory Token Bucket | Simplicity and speed |
| 2 | Multiple JVMs | Horizontal scaling |
| 3 | Redis-backed Buckets | Global rate limiting |
| 4 | API Gateway | Centralized enforcement |
| 5 | Monitoring & Metrics | Operational visibility |
| 6 | Dynamic Configuration | Runtime policy changes |
| 7 | Advanced Policies | Fine-grained control |
| 8 | Enterprise Platform | Large-scale production readiness |

By keeping responsibilities modular and depending on abstractions such as `RateLimiterService`, the current codebase can adopt each enhancement incrementally without requiring a complete redesign.

---
