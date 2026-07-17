# Rate Limiting Fundamentals

## Table of Contents

1. Introduction
2. What is Rate Limiting?
3. Why Rate Limiting?
4. Problems Solved
5. Real-World Examples
6. How Rate Limiting Works
7. Where Rate Limiting is Applied
8. Client Identification
9. Rate Limit Policies
10. HTTP Status Codes
11. Best Practices
12. Common Challenges
13. How This Project Uses Rate Limiting
14. Key Takeaways

---

# Introduction

Rate limiting is a technique used to control the number of requests a client can make to a server within a specified period of time.

Instead of allowing unlimited requests, the server enforces predefined limits to protect backend resources and ensure fair usage among all clients.

This project implements a **Token Bucket Rate Limiter** that integrates with Spring Boot using an interceptor to validate every incoming request before it reaches the application logic.

---

# What is Rate Limiting?

Rate limiting is the process of restricting how frequently a client can access an API or service.

Example:

Suppose an API allows:

```
20 requests per minute
```

If a client sends

```
15 requests
```

✅ Allowed

If the client sends

```
21st request
```

❌ Rejected

The rejected request typically receives

```
HTTP 429 Too Many Requests
```

---

# Why Rate Limiting?

Without rate limiting, any client can overwhelm your server.

Example:

```
Client

↓

100000 Requests

↓

Spring Boot Application

↓

Database

↓

Server Crash
```

With rate limiting

```
Client

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

Benefits include:

- Protects servers from overload
- Prevents abuse
- Reduces infrastructure cost
- Improves application stability
- Ensures fair usage
- Protects backend services

---

# Problems Solved

## 1. API Abuse

Some users or bots continuously call APIs.

Without rate limiting:

```
Unlimited Requests

↓

High CPU Usage

↓

High Memory Usage

↓

Slow Response

↓

Application Failure
```

---

## 2. Brute Force Attacks

Example:

```
POST /login
```

An attacker continuously tries passwords.

Rate limiting slows the attacker dramatically.

---

## 3. DDoS Mitigation

Although rate limiting is not a complete DDoS solution, it provides the first layer of protection by rejecting excessive requests before they consume application resources.

---

## 4. Fair Usage

Imagine:

```
100 Users
```

One user sends

```
5000 requests
```

Without limits, that single user can degrade service for everyone else.

Rate limiting ensures all users receive a fair share of resources.

---

## 5. Infrastructure Cost

Every unnecessary request consumes:

- CPU
- Memory
- Network bandwidth
- Database connections

Rejecting abusive traffic early reduces operational costs.

---

# Real-World Examples

Almost every major platform uses rate limiting.

### GitHub

Limits API requests per authenticated user.

---

### Google Maps API

Charges and limits requests based on API keys.

---

### Stripe

Applies limits to protect payment infrastructure.

---

### AWS API Gateway

Supports configurable rate limits and burst limits.

---

### Cloudflare

Uses sophisticated rate limiting to mitigate abuse and attacks.

---

# How Rate Limiting Works

Every incoming request follows a simple lifecycle.

```
Incoming Request

↓

Identify Client

↓

Find Client Bucket

↓

Check Available Tokens

↓

Enough Tokens?

↓

YES → Allow Request

NO → Reject Request
```

In this project, this flow is implemented through a Spring MVC interceptor before the request reaches the controller.

---

# Where Rate Limiting is Applied

Rate limiting can be enforced at different layers.

## API Gateway

Example:

```
Internet

↓

Gateway

↓

Microservices
```

Suitable for distributed systems.

---

## Reverse Proxy

Examples:

- NGINX
- HAProxy

Useful for infrastructure-level protection.

---

## Application Layer

Example:

```
Spring Boot

↓

Interceptor

↓

Controller
```

This project follows the **application-layer approach**, making the implementation easy to understand, test, and extend.

---

# Client Identification

A rate limiter must know **who** is making the request.

Common identifiers include:

### IP Address

Example:

```
192.168.1.25
```

Simple but can affect users behind shared networks.

---

### User ID

```
User: 1024
```

Useful after authentication.

---

### API Key

```
X-API-Key
```

Common for public APIs.

---

### JWT Subject

```
sub = user123
```

Ideal for authenticated services.

---

### Combination

Some systems combine multiple identifiers, such as:

```
User ID + API Key
```

for finer-grained control.

---

# Rate Limit Policies

Different APIs require different policies.

Examples:

```
20 requests / minute
```

```
100 requests / hour
```

```
1000 requests / day
```

Some systems also differentiate by user roles.

Example:

| Role | Requests/Minute |
|------|----------------:|
| Guest | 20 |
| User | 100 |
| Premium | 1000 |
| Admin | Unlimited |

---

# HTTP Status Codes

When a client exceeds the limit:

```
HTTP 429 Too Many Requests
```

The response may include:

```
Retry-After: 30
```

This tells the client to wait before retrying.

Example response:

```http
HTTP/1.1 429 Too Many Requests

Retry-After: 30
```

---

# Best Practices

A production-ready rate limiter should:

- Be thread-safe
- Use efficient data structures
- Support burst traffic
- Minimize lock contention
- Avoid unnecessary memory growth
- Provide configurable limits
- Return meaningful error responses
- Expose metrics for monitoring

---

# Common Challenges

## Memory Growth

Each client may require its own bucket.

Inactive buckets should be cleaned periodically.

This project includes a cleanup scheduler for that purpose.

---

## Distributed Systems

An in-memory rate limiter works only within a single application instance.

Multiple instances require a shared store such as Redis.

---

## Clock Accuracy

Time calculations should use a monotonic clock where appropriate to avoid issues caused by system clock adjustments.

---

## High Concurrency

Many requests may arrive simultaneously.

The implementation must ensure bucket state remains consistent without becoming a performance bottleneck.

---

# How This Project Uses Rate Limiting

This project uses the **Token Bucket Algorithm**.

Request flow:

```
Client

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

Controller
```

Key implementation characteristics:

- One bucket per client
- Lazy token refill
- Thread-safe token consumption
- Automatic cleanup of inactive buckets
- Configurable limits through Spring Boot properties
- HTTP 429 responses when limits are exceeded

The detailed implementation is covered in later documents:

- `03-Token-Bucket.md`
- `06-Implementation.md`
- `08-Thread-Safety.md`

---

# Key Takeaways

- Rate limiting protects backend services from abuse and overload.
- It ensures fair resource usage across clients.
- Different algorithms exist, each with its own trade-offs.
- This project uses the Token Bucket algorithm because it balances efficiency with support for burst traffic.
- The implementation is designed to be extensible, thread-safe, and easy to integrate into Spring Boot applications.

---
