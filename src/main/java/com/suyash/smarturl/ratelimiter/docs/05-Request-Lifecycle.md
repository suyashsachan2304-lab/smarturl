# Request Lifecycle

## Table of Contents

1. Introduction
2. Why Understand the Request Lifecycle?
3. End-to-End Request Flow
4. Step 1 – Client Sends Request
5. Step 2 – Spring Boot Receives Request
6. Step 3 – RateLimitInterceptor Executes
7. Step 4 – Client Identification
8. Step 5 – RateLimiterService
9. Step 6 – Bucket Retrieval
10. Step 7 – Lazy Refill
11. Step 8 – Token Consumption
12. Step 9 – Decision Making
13. Step 10 – Controller Execution
14. Exception Flow
15. Sequence Diagram
16. Performance Analysis
17. Complete Lifecycle Example
18. Summary

---

# Introduction

Every HTTP request entering the application follows a predefined lifecycle before reaching the business logic.

The rate limiter is intentionally placed **before the controller** so that expensive operations such as:

- Database Queries
- Cache Lookups
- External API Calls
- Business Logic

are skipped if the client has already exceeded its request quota.

This significantly reduces unnecessary resource consumption.

---

# Why Understand the Request Lifecycle?

Understanding the request lifecycle helps answer questions such as:

- Where is rate limiting enforced?
- When are tokens consumed?
- How is a client identified?
- What happens when no tokens remain?
- When is HTTP 429 returned?
- Which classes participate in processing a request?

---

# End-to-End Request Flow

```
                Client
                   │
                   ▼
        Spring DispatcherServlet
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
          (Lazy Refill)
                   │
                   ▼
         Consume One Token
                   │
          ┌────────┴────────┐
          │                 │
          ▼                 ▼
      Token Exists      No Token
          │                 │
          ▼                 ▼
 Controller          HTTP 429 Response
```

---

# Step 1 – Client Sends Request

Example

```
GET /api/products
```

The client may be

- Browser
- Mobile App
- Backend Service
- API Gateway

The request first reaches Spring Boot.

---

# Step 2 – Spring Boot Receives Request

Spring Boot routes every request through

```
DispatcherServlet
```

The DispatcherServlet executes registered interceptors before invoking the controller.

```
Client

↓

DispatcherServlet

↓

Interceptors

↓

Controller
```

Our rate limiter is registered as one of these interceptors.

---

# Step 3 – RateLimitInterceptor Executes

The interceptor acts as the entry point of the rate limiter.

Responsibilities:

- Intercept request
- Identify client
- Invoke rate limiter
- Stop request if limit exceeded

Conceptually:

```java
preHandle(request)
```

↓

```
RateLimiterService.allowRequest(...)
```

If allowed

↓

Continue

Else

↓

Throw exception

---

# Step 4 – Client Identification

Before applying limits, the system determines **who** is making the request.

Examples

```
IP Address
```

```
API Key
```

```
JWT User ID
```

```
Customer ID
```

The `ClientIdentifier` component abstracts this logic.

```
HTTP Request

↓

ClientIdentifier

↓

client-123
```

The returned identifier becomes the key used to retrieve the bucket.

---

# Step 5 – RateLimiterService

The interceptor delegates all rate-limiting logic to the service layer.

```
Interceptor

↓

RateLimiterService

↓

Decision
```

Advantages

- Business logic remains separate
- Easy testing
- Easy replacement of algorithms

---

# Step 6 – Bucket Retrieval

The service requests the client's bucket from the `BucketStore`.

```
BucketStore

↓

ConcurrentHashMap

↓

Client ID

↓

Bucket
```

Example

```
client-1

↓

Bucket A
```

If no bucket exists

↓

Create one

↓

Store it

↓

Return bucket

Each client owns exactly one bucket.

---

# Step 7 – Lazy Refill

This project uses **lazy refill**.

Instead of continuously adding tokens,

tokens are regenerated only when a request arrives.

Current bucket

```
Capacity = 20

Tokens = 5
```

Suppose

```
30 seconds

have passed
```

The refill logic calculates

```
Tokens to Generate
```

Updates bucket

```
5

↓

10
```

No background thread is required.

Benefits

- Lower CPU usage
- Simpler design
- Better scalability

---

# Step 8 – Token Consumption

After refilling,

one token is consumed.

Example

```
Available Tokens

8
```

↓

Consume

↓

```
7
```

Request continues.

If available tokens are

```
0
```

↓

Request cannot proceed.

---

# Step 9 – Decision Making

The bucket now determines the final outcome.

```
Tokens > 0

↓

Allow
```

or

```
Tokens == 0

↓

Reject
```

Decision Tree

```
            Request
               │
               ▼
         Refill Bucket
               │
               ▼
      Tokens Available?
         │          │
       YES          NO
         │          │
         ▼          ▼
 Consume Token   Reject Request
         │
         ▼
 Continue
```

---

# Step 10 – Controller Execution

If a token was successfully consumed,

processing continues normally.

```
Rate Limiter

↓

Controller

↓

Service

↓

Repository

↓

Database
```

The controller remains completely unaware that rate limiting occurred.

---

# Exception Flow

If the bucket is empty,

the service throws

```
RateLimitExceededException
```

Flow

```
Bucket

↓

No Tokens

↓

Throw Exception

↓

GlobalExceptionHandler

↓

HTTP 429
```

Typical response

```http
HTTP/1.1 429 Too Many Requests

{
    "status":429,
    "message":"Rate limit exceeded"
}
```

---

# Sequence Diagram

```
Client
   │
   ▼
DispatcherServlet
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
   ├── Lazy Refill
   │
   ├── Consume Token
   │
   ▼
Decision
   │
 ┌─┴─────────────┐
 │               │
 ▼               ▼
Controller     Exception
 │               │
 ▼               ▼
Response     HTTP 429
```

---

# Performance Analysis

Every request performs only a few constant-time operations.

| Operation | Complexity |
|-----------|-----------:|
| Identify Client | O(1) |
| Find Bucket | O(1) |
| Lazy Refill | O(1) |
| Consume Token | O(1) |
| Decision | O(1) |

Overall request-processing overhead is **O(1)**.

Memory usage depends only on the number of active clients.

---

# Complete Lifecycle Example

Configuration

```
Capacity = 5

Refill = 1 Token / Second
```

### Request 1

```
Tokens

5

↓

4

↓

Allowed
```

---

### Request 2

```
4

↓

3

↓

Allowed
```

---

### Request 3

```
3

↓

2

↓

Allowed
```

---

### Wait 3 Seconds

```
Generated

3 Tokens
```

Bucket

```
2

↓

5
```

(capacity reached)

---

### Five More Requests

```
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
```

All allowed.

---

### Next Request

```
0 Tokens

↓

Rejected

↓

HTTP 429
```

---

# Summary

Every request follows the same predictable lifecycle:

1. Request reaches Spring Boot.
2. `RateLimitInterceptor` intercepts it.
3. `ClientIdentifier` extracts the client identity.
4. `RateLimiterService` processes the request.
5. `BucketStore` retrieves or creates the client's bucket.
6. `TokenBucket` performs lazy refill.
7. One token is consumed if available.
8. If successful, the request reaches the controller.
9. Otherwise, a `RateLimitExceededException` is thrown and translated into **HTTP 429 Too Many Requests**.

This design keeps the controller independent of rate-limiting concerns while ensuring that abusive requests are rejected as early as possible in the request pipeline.

---
