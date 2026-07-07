# E-Commerce Microservices Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-green)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://openjdk.java.net/)
[![Docker](https://img.shields.io/badge/Docker-Latest-blue)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A production-style Spring Boot microservices platform demonstrating cloud-native architecture, distributed systems patterns, and concurrent integration testing at scale.

## Project Overview

This is a **microservices-based e-commerce platform** built with Spring Boot, showcasing enterprise-grade patterns for distributed systems. The project demonstrates:

- **Distributed locking** using Redis to prevent overselling across multiple Kubernetes replicas
- **Concurrent testing** with 50+ simultaneous HTTP requests using JUnit 5 and advanced thread synchronization
- **Event-driven architecture** using Apache Kafka for asynchronous service communication
- **Service discovery** with Spring Cloud Eureka
- **API gateway** routing with Spring Cloud Gateway
- **Cloud-native deployment** on Kubernetes with Docker containerization

---

## Features

✨ **Core Architecture**
- Spring Boot Microservices (8 services)
- Spring Cloud Gateway with routing and load balancing
- Eureka Server for service discovery
- OpenFeign for inter-service REST communication

🔄 **Event Processing**
- Apache Kafka for event-driven messaging
- Kafka producers and consumers across services
- Asynchronous order processing and notifications

🔒 **Data & Caching**
- MySQL for persistent data storage
- Redis for caching and distributed locking (Redisson)
- Automatic cache invalidation

⚙️ **DevOps & Deployment**
- Docker containerization for all services
- Docker Compose for local orchestration
- Kubernetes deployment with multiple replicas
- ConfigMaps and Secrets support

🧪 **Testing & Quality**
- JUnit 5 integration tests (concurrent load testing)
- CountDownLatch synchronization without Thread.sleep()
- ExecutorService for thread pool management
- AtomicInteger for thread-safe counters
- 50+ concurrent requests validation

---

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Application                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    Port 9191 (NodePort)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway                                  │
│                  (Spring Cloud Gateway)                          │
└────────────────────────────┬────────────────────────────────────┘
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
    ┌─────────┐        ┌──────────┐      ┌──────────┐
    │ Product │        │  Order   │      │ Identity │
    │ Service │        │ Service  │      │ Service  │
    └────┬────┘        └────┬─────┘      └────┬─────┘
         │                  │                  │
         └──────────────────┼──────────────────┘
                  ┌─────────┼─────────┐
                  ▼         ▼         ▼
            ┌──────────────────────────┐
            │   Eureka Service         │
            │   Registry (Port 8761)   │
            └──────────────────────────┘
```

### Stock Service Kubernetes Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                          │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Stock Service (Deployment)                 │ │
│  │                 Replicas: 2                             │ │
│  │  ┌──────────────────┐      ┌──────────────────────┐   │ │
│  │  │  Stock Service   │      │  Stock Service       │   │ │
│  │  │    Pod 1         │      │    Pod 2             │   │ │
│  │  │ (Port 8080)      │      │ (Port 8080)          │   │ │
│  │  └────────┬─────────┘      └─────────┬────────────┘   │ │
│  │           │                          │                │ │
│  │           └──────────────┬───────────┘                │ │
│  │                          │                           │ │
│  └──────────────────────────┼───────────────────────────┘ │
│                             │                              │
│  ┌──────────────────────────▼──────────────────────────┐  │
│  │         Stock Service NodePort Service             │  │
│  │         (Port 30081 → 8080)                        │  │
│  └────────────────────────────────────────────────────┘  │
│                                                           │
│  ┌──────────────────────┐    ┌──────────────────────┐    │
│  │      Redis           │    │      MySQL           │    │
│  │   (Distributed Lock) │    │   (Stock Database)   │    │
│  └──────────────────────┘    └──────────────────────┘    │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Category | Technologies |
|----------|--------------|
| **Backend** | Spring Boot 3.3.3, Spring Cloud, Spring Data JPA, Spring Cloud Gateway |
| **Database** | MySQL 8.0, Redis 7.0 (Redisson for distributed locks) |
| **Messaging** | Apache Kafka, Zookeeper |
| **Communication** | OpenFeign, REST APIs |
| **Service Discovery** | Eureka Server (Spring Cloud Netflix) |
| **Monitoring** | Zipkin (distributed tracing) |
| **DevOps** | Docker, Docker Compose, Kubernetes |
| **Testing** | JUnit 5, Spring Boot Test, Awaitility |
| **Java** | JDK 17, Maven |

---

## Microservices

| Service | Port | Purpose | Tech Stack |
|---------|------|---------|-----------|
| API Gateway | 9191 | Request routing, load balancing | Spring Cloud Gateway |
| Eureka Server | 8761 | Service discovery & registration | Spring Cloud Netflix |
| Product Service | 8084 | Product catalog & inventory | Spring Boot, MySQL, Redis |
| Order Service | 8080 | Order processing & tracking | Spring Boot, MySQL, Kafka |
| Email Service | 8081 | Email notifications | Spring Boot, MySQL |
| Identity Service | 9898 | Authentication & authorization | Spring Boot, MySQL, Redis |
| Payment Service | 8085 | Payment processing | Spring Boot, MySQL, Zipkin |
| **Stock Service** | 8082 | Inventory with distributed locking | Spring Boot, MySQL, Redis, **JUnit 5 Tests** |

---

## Distributed Locking (Redis)

### The Overselling Problem

Without distributed locking, multiple concurrent requests can create race conditions:

```
Initial Stock = 5

Request 1: Read stock (5) → Check OK → Sleep...
Request 2: Read stock (5) → Check OK → Sleep...
...
Request 5: Read stock (5) → Check OK → Decrease to 4
Request 1: Decreases to 4 (WRONG! Should be checked again)

Result: Final stock = Negative (OVERSOLD)
```

### Redis Distributed Lock Solution

Using Redisson (Redis client for Java):

```java
String lockKey = "lock:product:" + productId;
RLock lock = redissonClient.getLock(lockKey);

try {
    lock.tryLock(5, 10, TimeUnit.SECONDS);  // Acquire lock
    // Only ONE request can execute here
    Stock current = stockRepository.findByProductId(productId);
    if (current.getQuantity() >= quantity) {
        current.setQuantity(current.getQuantity() - quantity);
        stockRepository.save(current);
    }
} finally {
    lock.unlock();  // Release lock
}
```

### Why Kubernetes Needs Distributed Locks

```
Kubernetes Cluster with 2 Stock Service Replicas:

  Pod 1                  Pod 2
  ┌────────────┐        ┌────────────┐
  │ Instance 1 │        │ Instance 2 │
  │ (Memory)   │        │ (Memory)   │
  └────────────┘        └────────────┘
       │                      │
       └──────────┬───────────┘
                  │
              Redis Lock
          (Shared across all pods)

Without Redis: Each pod has its own cache → Race conditions
With Redis: Single source of truth → No overselling
```

---

## Kubernetes Deployment

### Deployment Manifest

```yaml
# stock-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: stock-service
spec:
  replicas: 2  # Multiple instances
  selector:
    matchLabels:
      app: stock-service
  template:
    metadata:
      labels:
        app: stock-service
    spec:
      containers:
      - name: stock-service
        image: stock-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: stock-config
              key: db-url
```

### Service Manifest (NodePort)

```yaml
# stock-service-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: stock-service
spec:
  type: NodePort
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30081
  selector:
    app: stock-service
```

### Deployment Commands

```bash
# Apply deployment
kubectl apply -f stock-service-deployment.yaml

# Apply service
kubectl apply -f stock-service-service.yaml

# Verify deployment
kubectl get deployments
kubectl get pods
kubectl get svc

# Scale replicas
kubectl scale deployment stock-service --replicas=3

# View logs
kubectl logs -f deployment/stock-service

# Port forward (testing)
kubectl port-forward svc/stock-service 8080:8080
```

---

## Concurrent Integration Testing

### Why We Replaced Python Stress Test

**Before:** Python `stress_test.py` (simple load generation, no assertions)
**Now:** JUnit 5 integration tests (50 concurrent requests, full validation)

### Test Mechanism

```
Test Setup:
├─ Create ExecutorService with 10 threads
├─ Create CountDownLatch(1) for release signal
├─ Create CountDownLatch(50) for completion tracking
└─ Create AtomicInteger counters for results

Test Execution:
├─ Submit 50 tasks to executor
├─ All tasks block on startSignal.await()
├─ Release all threads simultaneously (startSignal.countDown())
├─ Each task makes HTTP POST to /api/v1/stock/decrease
├─ Increment success/failure counters
├─ Wait for completion (doneSignal.await())

Test Validation:
├─ Assert successCount == 5
├─ Assert failureCount == 45
└─ Assert finalStock == 0 (no overselling)
```

### Running Tests

```bash
cd stock-service

# Run all tests
mvn clean test

# Run specific test
mvn test -Dtest=StockConcurrencyIntegrationTest

# Run main concurrent test
mvn test -Dtest=StockConcurrencyIntegrationTest#testConcurrentStockDecrementWithDistributedLock

# Verbose output
mvn test -X
```

### Test Scenario

| Parameter | Value |
|-----------|-------|
| Initial Stock | 5 units |
| Concurrent Requests | 50 |
| Thread Pool | 10 threads |
| Request Type | POST /api/v1/stock/decrease?productId=9999&quantity=1 |
| Expected Successful | 5 (only available stock) |
| Expected Failed | 45 (insufficient stock, 400 response) |
| Final Stock | 0 (no overselling) |
| Execution Time | ~8-10 seconds |

---

## Running the Project

### Prerequisites

```bash
# Docker & Docker Compose
docker --version
docker-compose --version

# Java 17+
java -version

# Maven
mvn --version

# Kubernetes (for K8s deployment)
kubectl version
```

### Docker Compose (Local Development)

```bash
# Clone repository
git clone https://github.com/your-org/microservices.git
cd springboot-kafka-microservices

# Build and start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f stock-service

# Stop services
docker-compose down
```

### Maven Build

```bash
cd stock-service

# Build JAR
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Run locally
mvn spring-boot:run
```

### Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace ecommerce

# Deploy services
kubectl apply -f k8s/ -n ecommerce

# Verify deployment
kubectl get all -n ecommerce

# Access service
curl http://localhost:30081/api/v1/stock/1001
```

---

## API Examples

### Initialize Stock

```bash
curl -X POST "http://localhost:8082/api/v1/stock/init?productId=1001&quantity=100"
```

**Response:** `200 OK - Stock initialized successfully.`

### Get Stock

```bash
curl -X GET "http://localhost:8082/api/v1/stock/1001"
```

**Response:**
```json
{
  "id": 1,
  "productId": 1001,
  "quantity": 100
}
```

### Decrease Stock

```bash
curl -X POST "http://localhost:8082/api/v1/stock/decrease?productId=1001&quantity=5"
```

**Response (Success):** `200 OK - Stock decreased successfully.`
**Response (Failure):** `400 Bad Request - Insufficient stock! Remaining: 2`

---

## Project Structure

```
springboot-kafka-microservices/
├── api-gateway/              # Spring Cloud Gateway (Port 9191)
├── service-registry/         # Eureka Server (Port 8761)
├── product-service/          # Product management
├── order-service/            # Order processing
├── email-service/            # Email notifications
├── identity-service/         # Authentication
├── payment-service/          # Payment processing
├── stock-service/            # Inventory with distributed locking
│   ├── src/main/java/        # Application code
│   ├── src/test/java/        # JUnit 5 integration tests
│   ├── pom.xml              # Maven configuration
│   ├── Dockerfile           # Container image
│   └── QUICKSTART.md         # Test documentation
├── docker-compose.yml        # Local orchestration
├── k8s/                      # Kubernetes manifests
└── README.md                 # This file
```

---

## Future Improvements

### Scalability
- [ ] Horizontal Pod Autoscaler (HPA) based on CPU/Memory
- [ ] Event streaming with multiple Kafka topics per domain
- [ ] Caching layer optimization

### Observability
- [ ] Prometheus metrics and monitoring dashboards
- [ ] Grafana dashboards for visualization
- [ ] ELK Stack (Elasticsearch, Logstash, Kibana) for centralized logging
- [ ] Improved distributed tracing with Jaeger

### DevOps
- [ ] Helm Charts for standardized K8s deployments
- [ ] GitHub Actions CI/CD pipeline
- [ ] ArgoCD for GitOps deployments
- [ ] Istio service mesh for traffic management

### Testing
- [ ] Performance benchmarking with JMeter
- [ ] Chaos engineering tests
- [ ] Contract testing with Pact
- [ ] Security scanning in CI/CD

---

## Resume Highlights

This project demonstrates mastery of:

🏗️ **Microservices Architecture**
- Service decomposition and boundaries
- Inter-service communication (REST, Kafka)
- Service discovery with Eureka

🔄 **Distributed Systems**
- Distributed locking with Redis to prevent race conditions
- Handling eventual consistency
- Coordinating state across services

⚙️ **Concurrency**
- ExecutorService thread pools
- CountDownLatch for thread synchronization
- AtomicInteger for thread-safe counters
- No blocking delays (Thread.sleep() free)

☁️ **Cloud-Native**
- Kubernetes deployments with multiple replicas
- Docker containerization
- Configuration management (ConfigMaps)
- Service scaling and health checks

🧪 **Production Testing**
- JUnit 5 integration tests
- Concurrent load testing (50+ simultaneous requests)
- Comprehensive test assertions
- Test isolation and repeatability

🔒 **Enterprise Patterns**
- API Gateway routing and rate limiting
- Event-driven architecture (Kafka)
- Authentication and authorization
- Distributed tracing

---

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Redis Redisson](https://redisson.org/)
- [Apache Kafka](https://kafka.apache.org/)
- [Docker Documentation](https://docs.docker.com/)

## License

MIT License - See LICENSE file for details

---

**Status:** Production-Ready | **Last Updated:** July 2026
