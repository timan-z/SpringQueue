# SpringQueue

A lightweight, concurrent job queue system built in **Java (w/ Spring Boot)**, featuring retry logic, configurable worker pools, RESTful API endpoints, and a **React/TypeScript dashboard** for real-time monitoring and control. (The latter is optional, you can interact with the backend purely with Postman or a similar tool, if desired).

This project demonstrates **production-style concurrency patterns** — leveraging **ExecutorService**, **BlockingQueue**, and **Spring Boot’s dependency injection** — while providing a clear, extensible architecture for asynchronous task processing.

(To be frank, this is primarily a **learning project** (for Spring Boot), modeled after real-world job/task queue systems such as **Celery**, **Sidekiq**, and **RabbitMQ**. It’s intentionally blunt and skeletal in design, existing to simulate the internal mechanics of these tools).

---

## Features

- **Concurrent job processing** using a fixed worker pool maanged by `ExecutorService`
- **Producer–Consumer pattern** for safe, asynchronous task execution.
- **Configurable retry logic** with maximum attempt limits and simulated failures
- **Multiple simulated job types** (e.g., `email`, `report` (generation), `data cleanup`, etc.)
- **Thread-safe in-memory state tracking** via `ConcurrentHashMap`
- **Lifecycle management** with `@PreDestroy` for graceful worker shutdown
- **RESTful API** for enqueueing, retrieving, retrying, and deleting jobs
- **Frontend dashboard** built in React + TypeScript for real-time system interaction, job visibility, and control

---

## Skills & Concepts Demonstrated

- **Java Concurrency** – Thread pools, synchronization, and safe shared state management
- **ExecutorService & Runnable** – Idiomatic Java concurrency management (instead of manual `Thread` loops as in my GoQueue project)
- **Producer–Consumer Pattern** – Decoupled producers submit jobs to a managed worker pool
- **Spring Boot Fundamentals** – REST API design, dependency injection, and service lifecycle
- **Task Lifecycle Management** – Status tracking (`QUEUED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`)
- **Retry Strategies** – Re-enqueue failed jobs with capped retry counts
- **Application Lifecycle** – Using `@Service`, `@RestController`, and `@PreDestroy` for safe shutdowns
* **Frontend Integration** – Connecting a Spring Boot backend to a modern React/TypeScript frontend (done this before with Node.js, Go, and so on, but worth noting I suppose).

---

## Architecture

```
          ┌──────────────────────┐
          │   Frontend (React)   │
          │  TypeScript Dashboard│
          └────────┬─────────────┘
                   │ REST API calls
                   ▼
          ┌─────────────────────┐
          │   Producer Layer    │ (Spring REST Controller)
          └──────┬──────────────┘
                 │ Enqueue / Retry / Delete
                 ▼
          ┌─────────────────────┐
          │   Queue Service     │ (In-Memory Store)
          │   Job State Tracker │ (ConcurrentHashMap)
          └──────┬──────────────┘
                 │
         ┌───────▼────────┐
         │ Worker Pool     │ (ExecutorService)
         │ Runnable Tasks  │
         └──────┬─────────┘
                │ process jobs
                ▼
         ┌───────────────┐
         │ Task Lifecycle│
         │ QUEUED → IN_PROGRESS → COMPLETED / FAILED
         └───────────────┘
```

## Project Structure

```
springqueue/
├── springqueue-backend/
│   ├── src/main/java/com/springqbackend/springqueue/
│   │   ├── SpringQueueApplication.java
│   │   ├── controller/ProducerController.java
│   │   ├── service/QueueService.java
│   │   ├── runtime/Worker.java
│   │   ├── models/Task.java
│   │   └── enums/TaskStatus.java
│   └── pom.xml
│
└── springqueue-frontend/
    ├── src/
    │   ├── App.tsx
    │   ├── utility/api.ts
    │   ├── utility/types.ts
    │   ├── components/
    │   │   ├── JobsList.tsx
    │   │   ├── JobDisplay.tsx
    │   │   └── LoadingSpinner.tsx
    │   └── App.css
    ├── package.json
    └── vite.config.ts
```

---

## Development Journey

This project began as a **Go → Java learning experiment**, reimagining my earlier project [GoQueue](https://github.com/timan-z/GoTaskQueue) in Java/Spring Boot.

### Phase 1: One-to-One Translation

I first **reimplemented my GoQueue logic in Java**, preserving structure and concurrency flow:

- Shared in-memory queue (using `BlockingQueue`)
- Worker goroutine equivalents (`Thread` loops)
- Manual retry and re-enqueue logic

This phase solidified my understanding of how **Go’s goroutines and channels** translate into **Java’s threads and blocking queues**.

### Phase 2: Refactoring to Idiomatic Java

Once feature parity was reached—[a snapshot of which you can view here (my SpringBoot backend directly translating the Go backend's logic one-to-one)](https://github.com/timan-z/SpringQueue/tree/4e7d973c8a62d11e374c8326970fc39264f464b3)—I redesigned everything for idiomatic Java:

- Introduced **ExecutorService** for managed thread pools (making `BlockingQueue` redundant).
- Used **Spring Boot annotations** (`@Service`, `@RestController`) for clean architecture
- Added lifecycle management with `@PreDestroy`
- Modularized components into layers (Controller → Service → Runtime)

The end result was a **clean, Spring-idiomatic system** that preserved Go’s concurrency spirit while embracing Java’s ecosystem.

---

## API Endpoints

| Method   | Endpoint               | Description                   |
| -------- | ---------------------- | ----------------------------- |
| `POST`   | `/api/enqueue`         | Enqueue a new job             |
| `GET`    | `/api/jobs`            | View all jobs                 |
| `GET`    | `/api/jobs/{id}`       | View a specific job           |
| `POST`   | `/api/jobs/{id}/retry` | Retry a job (creates a clone) |
| `DELETE` | `/api/jobs/{id}`       | Delete a job                  |
| `POST`   | `/api/clear`           | Clear all jobs                |

---

## Example Job Types
- **email** → Simulates sending an email (2s)
- **report** → Simulates report generation (5s)
- **data-cleanup** → Cleans up mock data (3s)
- **newsletter** → Simulates batch email sending (4s)
- **sms** → Short task, 1s execution
- **takes-long** → Long-running job (10s)
- **fail** → Fails until retries are exhausted
- **fail-absolute** → Always fails, even after max retries

---

## Getting Started

### Prerequisites

* [Java 21+ (LTS)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
* [Maven](https://maven.apache.org/download.cgi)
* [Node.js 18+](https://nodejs.org/en/)

### Clone & Install

```bash
git clone https://github.com/timan-z/springqueue.git
```

#### Backend Setup

```bash
cd springqueue/springqueue-backend
mvn clean install
./mvnw spring-boot:run
```

Default API URL: **[http://localhost:8080](http://localhost:8080)** (for now, haven't deployed this anywhere yet).

#### Frontend Setup

```bash
cd springqueue/springqueue-frontend
npm install
npm run dev
```

Set your backend URL in `.env`:

```env
VITE_API_BASE=http://localhost:8080
```

---

## Frontend Integration

The **React + TypeScript Dashboard** acts as a live controller and viewer for the backend queue system.

### Core Capabilities:

- **Enqueue new jobs** with a custom payload and job type
- **View all active jobs** and their live statuses
- **Inspect individual jobs** by ID
- **Retry failed jobs** and re-enqueue them
- **Clear the queue** with one click
- **Dynamic visibility controls** for lists and detailed displays

### Tech Highlights:

- Built with **React + TypeScript** and **Vite**
- API abstraction layer in `utility/api.ts`
- Modular UI with components like `JobsList`, `JobDisplay`, and `LoadingSpinner`
- Live refresh via simple async re-fetch patterns (no WebSockets — just REST polling)
- Fully responsive and minimal UI styled via CSS

### Frontend–Backend Interaction:

```
Frontend (React)
   │
   ├─ [POST] /api/enqueue       → Adds new job to queue
   ├─ [GET]  /api/jobs          → Fetches all jobs
   ├─ [GET]  /api/jobs/:id      → Fetches one job
   ├─ [POST] /api/jobs/:id/retry→ Retries failed job
   └─ [POST] /api/clear         → Clears queue
        │
        ▼
Backend (Spring Boot)
   └─ Handles concurrency + task lifecycle
```

---

## What I Learned

- How **Go and Java handle concurrency differently**
- Building **REST APIs** in Spring Boot using dependency injection and annotation-driven design
- Managing **thread safety**, **retry logic**, and **task state transitions**
- Structuring code cleanly across **Controller–Service–Worker** layers
- Integrating a **frontend React app** with a Spring Boot backend
- Designing UI/UX for a live-updating dashboard
- Conceptual clarity around **Producer–Consumer systems** and **background processing architecture** (admittedly glossed over a lot of the architectural theory stuff when making GoQueue).

---

## Hosting

TO BE HOSTED ON NETLIFY AND RAILWAY

---

## Why This Project

This project marks my **transition towards Java/Spring Boot focus**, designed to strengthen:
- My backend development fundamentals
- My grasp of Java’s concurrency model
- My practical experience with **Spring Boot REST APIs**
- My ability to build **end-to-end systems** — backend to frontend

It’s both a **learning exercise** and a **showcase project**: a hands-on demonstration of concurrency, clean architecture, and full-stack integration.

---

## Technical Comparison: GoQueue vs SpringQueue

| Concept                      | Go Implementation (`GoQueue`)                                     | Java Implementation (`SpringQueue`)                                                  | Takeaway                                                                                                                      |
| ---------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- |
| **Concurrency Model**        | Uses **goroutines** (lightweight, managed by Go runtime)          | Uses **threads** managed by **ExecutorService**                                      | Go’s model is simpler and implicit; Java requires more explicit management but offers rich control through `Executors`.       |
| **Worker Pool**              | Goroutines listening on a shared `chan *Task`                     | Fixed thread pool executing `Runnable` tasks                                         | ExecutorService provides robust lifecycle control (`shutdown`, `awaitTermination`), while Go’s simplicity favors flexibility. |
| **Task Queue**               | Go **channels** for message passing                               | Java **BlockingQueue** (`LinkedBlockingQueue`) for producer–consumer synchronization | Both implement the same pattern; channels are built-in to Go, whereas Java uses a well-tested library abstraction.            |
| **State Management**         | Map with `sync.Mutex` locks for thread safety                     | `ConcurrentHashMap` provides built-in thread safety                                  | Java’s concurrency utilities simplify synchronization — fewer manual locks needed.                                            |
| **Error Handling / Retries** | Custom retry loop in goroutines                                   | Retry logic encapsulated in each `Worker`’s `run()` method                           | Java’s object-oriented structure makes retry strategies easier to encapsulate and extend.                                     |
| **Application Lifecycle**    | Graceful shutdown via context cancellation (`context.WithCancel`) | Lifecycle hooks via `@PreDestroy` in Spring                                          | Spring’s lifecycle annotations integrate cleanly with the framework, handling shutdown automatically.                         |
| **API Framework**            | Built using **Gin**                                               | Built using **Spring Boot (REST Controller)**                                        | Spring Boot offers more structure and convention, ideal for larger, scalable systems.                                         |
| **Architecture Style**       | Procedural & lightweight                                          | Layered (Controller → Service → Worker)                                              | Java’s structure encourages separation of concerns and testability.                                                           |
| **Deployment**               | Single binary (`go build`)                                        | Maven-built JAR / Spring Boot app                                                    | Go favors simplicity; Java favors configurability and ecosystem support.                                                      |
| **Frontend Integration**     | React/TypeScript dashboard                                        | Same React/TypeScript dashboard adapted to new API endpoints                         | Demonstrates cross-language full-stack integration.                                                                           |

---

### Summary

Porting **GoQueue → SpringQueue** offered a deep comparative understanding of **concurrency paradigms** across languages:

- Go provides **minimal, elegant concurrency** out of the box — great for small systems.
- Java offers **explicit, powerful control** with extensive concurrency tools and lifecycle management.
- The **Producer–Consumer pattern** remains universal, regardless of language — only the primitives differ.
- Spring Boot introduces robust structure, dependency injection, and built-in lifecycle hooks, making it ideal for production-ready systems.

This transition sharpened my ability to:

- Translate **concurrency logic across paradigms**
- Apply **object-oriented principles** to task orchestration
- Design **modular, RESTful backends** in both Go and Java
- Integrate a **shared React frontend** across heterogeneous backends

---

## Legacy Implementation (Pre-Refactor)

The project originally used a **shared `BlockingQueue`** (`LinkedBlockingQueue<Task>`) to coordinate producers and consumers — a classic **queue-driven concurrency model** mirroring Go’s `chan *Task` approach from *GoQueue*.

In that version [which you can see here](https://github.com/timan-z/SpringQueue/tree/4e7d973c8a62d11e374c8326970fc39264f464b3):
- The **QueueService** stored and managed the central `BlockingQueue`.
- Workers continuously polled the queue for new jobs, executing them in a loop.
- Synchronization and blocking behavior were handled implicitly by the queue itself.

Later, this design was **refactored** to leverage **`ExecutorService`**, which provides:

- Cleaner **lifecycle management** (shutdown, termination, reuse).
- More **idiomatic concurrency control** in modern Java.
- Clearer separation of responsibilities between producers (API layer) and consumers (worker pool).

This evolution reflects a key learning outcome — transitioning from a simple manual concurrency model to a **robust, idiomatic, and production-ready Java implementation**.

--- 

## Author

**Developed by:** Timan Zheng
**Stack:** Java • Spring Boot • Maven • React • TypeScript • ExecutorService • REST API • Concurrency
**Inspiration:** My Go-based [GoQueue](https://github.com/timan-z/gotaskqueue) project and concurrency models in Celery / Sidekiq.
