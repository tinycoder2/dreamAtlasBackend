# Dream Journal Backend — Spring Boot + Google Firestore

## 1. Objective

Build a Spring Boot web application that exposes REST APIs for managing users’ sleep data and dreams, with Google Cloud Firestore as the persistent datastore.

The existing React Native application currently stores data using SQLite with two tables:

* `day_logs` — one sleep record per user/date
* `dreams_new` — zero or more dreams associated with a date

The backend must model the same logical relationships in Firestore while adding `userId` as the ownership boundary.

The implementation should be production-oriented, cleanly layered, testable, and straightforward for Codex to extend later.

---

## 2. Core Domain Model

A user can have multiple days of sleep/dream data.

For a given user and calendar date:

* There is at most one `day_log`.
* There can be zero or many dreams.
* Each dream belongs to exactly one user and one date.
* Dreams have an explicit display order using `sortOrder`.
* Tags are represented as an array of strings rather than a JSON string.

### Existing SQLite mapping

#### `day_logs`

```sql
CREATE TABLE day_logs (
    date TEXT PRIMARY KEY,
    sleep_hours REAL,
    sleep_quality TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
```

#### `dreams_new`

```sql
CREATE TABLE dreams_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    text TEXT NOT NULL,
    mood TEXT,
    dream_type TEXT,
    tags TEXT NOT NULL DEFAULT '[]',
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
```

---

# 3. Firestore Data Model

Use Firestore as the sole persistence layer.

Recommended structure:

```text
users/{userId}
    days/{yyyy-MM-dd}
        sleepHours
        sleepQuality
        createdAt
        updatedAt

        dreams/{dreamId}
            text
            mood
            dreamType
            tags
            sortOrder
            createdAt
            updatedAt
```

Example:

```text
users/user-123/days/2026-08-18
```

Document:

```json
{
  "sleepHours": 7.5,
  "sleepQuality": "GOOD",
  "createdAt": "2026-08-18T07:30:00Z",
  "updatedAt": "2026-08-18T07:30:00Z"
}
```

Dream subcollection:

```text
users/user-123/days/2026-08-18/dreams/dream-abc123
```

Document:

```json
{
  "text": "I was walking through a huge forest...",
  "mood": "PEACEFUL",
  "dreamType": "LUCID",
  "tags": ["forest", "walking", "nature"],
  "sortOrder": 0,
  "createdAt": "2026-08-18T07:45:00Z",
  "updatedAt": "2026-08-18T07:45:00Z"
}
```

## Firestore modeling requirements

1. `userId` is always part of the document path.
2. The date is the document ID for the day record.
3. Date format must be `yyyy-MM-dd`.
4. Each user can have at most one day document for a given date.
5. Dreams should use Firestore-generated document IDs unless there is a strong reason to use another ID strategy.
6. `tags` must be stored as a native Firestore array.
7. Timestamps should be stored as Firestore timestamps / Java `Instant` rather than formatted strings where possible.
8. The application must not expose Firestore-specific implementation details to API clients unless necessary.
9. Data belonging to one user must never be returned for a different user.

---

# 4. Technology Requirements

Use:

* Java 21
* Spring Boot 3.x
* Spring Web
* Spring Validation
* Google Cloud Firestore Java SDK
* Maven
* JUnit 5
* Mockito where appropriate
* Spring Boot Test

Suggested package structure:

```text
com.example.dreamjournal

    controller
    service
    repository
    model
    dto
    exception
    config
```

Use constructor injection throughout.

Do not use field injection.

---

# 5. API Design

Base path:

```text
/api
```

All endpoints are scoped by `userId`.

## 5.1 Day Log APIs

### Create or update a day log

```http
PUT /api/users/{userId}/days/{date}
```

Request:

```json
{
  "sleepHours": 7.5,
  "sleepQuality": "GOOD"
}
```

Behavior:

* Create the day document if it does not exist.
* Update the existing day document if it does exist.
* Preserve `createdAt` on update.
* Update `updatedAt` on every write.
* The operation must be idempotent.

Response:

```http
200 OK
```

```json
{
  "date": "2026-08-18",
  "sleepHours": 7.5,
  "sleepQuality": "GOOD",
  "createdAt": "2026-08-18T07:30:00Z",
  "updatedAt": "2026-08-18T08:10:00Z"
}
```

---

### Get a day log

```http
GET /api/users/{userId}/days/{date}
```

Response:

```http
200 OK
```

Return the day log.

If no record exists:

```http
404 Not Found
```

---

### Delete a day log

```http
DELETE /api/users/{userId}/days/{date}
```

Behavior:

* Delete the day log.
* Also delete all dreams belonging to that day.
* The deletion must not leave orphaned dream records.

Response:

```http
204 No Content
```

If the day does not exist, return:

```http
404 Not Found
```

---

### List days for a user

```http
GET /api/users/{userId}/days
```

Optional query parameters:

```text
from=2026-08-01
to=2026-08-18
```

Return:

```json
[
  {
    "date": "2026-08-18",
    "sleepHours": 7.5,
    "sleepQuality": "GOOD",
    "createdAt": "2026-08-18T07:30:00Z",
    "updatedAt": "2026-08-18T08:10:00Z"
  }
]
```

Sort by date descending by default.

If pagination is implemented, use Firestore cursor-based pagination rather than offset pagination.

---

# 6. Dream APIs

## 6.1 Create a dream

```http
POST /api/users/{userId}/days/{date}/dreams
```

Request:

```json
{
  "text": "I was walking through a huge forest...",
  "mood": "PEACEFUL",
  "dreamType": "LUCID",
  "tags": ["forest", "walking", "nature"],
  "sortOrder": 0
}
```

Behavior:

* Generate a unique Firestore document ID.
* Set `createdAt`.
* Set `updatedAt`.
* Associate the dream with the specified user and date.
* If no day log exists, allow the dream to be created anyway unless business rules explicitly prohibit it.

Response:

```http
201 Created
```

```json
{
  "id": "dream-abc123",
  "date": "2026-08-18",
  "text": "I was walking through a huge forest...",
  "mood": "PEACEFUL",
  "dreamType": "LUCID",
  "tags": ["forest", "walking", "nature"],
  "sortOrder": 0,
  "createdAt": "2026-08-18T07:45:00Z",
  "updatedAt": "2026-08-18T07:45:00Z"
}
```

---

## 6.2 Get a dream

```http
GET /api/users/{userId}/days/{date}/dreams/{dreamId}
```

Response:

```http
200 OK
```

Return the requested dream.

If the dream does not exist or does not belong to that user/date:

```http
404 Not Found
```

---

## 6.3 Update a dream

```http
PUT /api/users/{userId}/days/{date}/dreams/{dreamId}
```

Request:

```json
{
  "text": "Updated dream text",
  "mood": "CURIOUS",
  "dreamType": "NORMAL",
  "tags": ["forest", "updated"],
  "sortOrder": 1
}
```

Behavior:

* Preserve `id`.
* Preserve `createdAt`.
* Update `updatedAt`.
* Replace the mutable fields supplied in the request.

Response:

```http
200 OK
```

---

## 6.4 Delete a dream

```http
DELETE /api/users/{userId}/days/{date}/dreams/{dreamId}
```

Response:

```http
204 No Content
```

Return `404 Not Found` when the dream does not exist.

---

## 6.5 List dreams for a date

```http
GET /api/users/{userId}/days/{date}/dreams
```

Return all dreams for that date.

Sort by:

1. `sortOrder` ascending
2. `createdAt` ascending as a deterministic tie breaker

Example:

```json
[
  {
    "id": "dream-1",
    "date": "2026-08-18",
    "text": "Dream one",
    "mood": "HAPPY",
    "dreamType": "NORMAL",
    "tags": ["home"],
    "sortOrder": 0,
    "createdAt": "2026-08-18T07:30:00Z",
    "updatedAt": "2026-08-18T07:30:00Z"
  },
  {
    "id": "dream-2",
    "date": "2026-08-18",
    "text": "Dream two",
    "mood": "STRANGE",
    "dreamType": "LUCID",
    "tags": ["flying"],
    "sortOrder": 1,
    "createdAt": "2026-08-18T07:40:00Z",
    "updatedAt": "2026-08-18T07:40:00Z"
  }
]
```

---

# 7. Combined Day View

Add an endpoint that is convenient for the mobile client:

```http
GET /api/users/{userId}/days/{date}/details
```

Response:

```json
{
  "date": "2026-08-18",
  "sleep": {
    "sleepHours": 7.5,
    "sleepQuality": "GOOD",
    "createdAt": "2026-08-18T07:30:00Z",
    "updatedAt": "2026-08-18T08:10:00Z"
  },
  "dreams": [
    {
      "id": "dream-1",
      "text": "Dream text",
      "mood": "PEACEFUL",
      "dreamType": "LUCID",
      "tags": ["forest"],
      "sortOrder": 0,
      "createdAt": "2026-08-18T07:45:00Z",
      "updatedAt": "2026-08-18T07:45:00Z"
    }
  ]
}
```

If the day log does not exist but dreams do, return the day with:

```json
"sleep": null
```

and the dreams collection populated.

If neither exists, return:

```http
404 Not Found
```

---

# 8. Data Types

## DayLog

```text
date          String / LocalDate
sleepHours    Double, nullable
sleepQuality  String, nullable
createdAt     Instant
updatedAt     Instant
```

## Dream

```text
id            String
date          LocalDate
text          String
mood          String, nullable
dreamType     String, nullable
tags          List<String>
sortOrder     Integer
createdAt     Instant
updatedAt     Instant
```

Do not use `java.util.Date` in the application domain model unless required by the Firestore SDK. Prefer `Instant`.

---

# 9. Validation Rules

### Date

* Must use `yyyy-MM-dd`.
* Reject invalid dates with `400 Bad Request`.

### Sleep hours

* Nullable.
* Must be greater than or equal to `0`.
* Maximum should be reasonable, such as `24`.

### Sleep quality

Initially treat this as a string rather than forcing an enum so the backend remains compatible with the current mobile application.

### Dream text

* Required.
* Must not be blank.
* Trim surrounding whitespace.
* Set a reasonable maximum length, e.g. 20,000 characters.

### Mood

Optional string.

### Dream type

Optional string.

### Tags

* Optional in request.
* Default to an empty array.
* Never return `null`; return `[]`.
* Remove duplicate tags.
* Preserve tag order unless there is a specific product requirement to sort them.

### sortOrder

* Default to `0`.
* Must be greater than or equal to `0`.

### userId

* Required in the URL.
* Reject blank user IDs.

---

# 10. API Error Format

All application errors should use a consistent response format.

Example:

```json
{
  "timestamp": "2026-08-18T16:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "sleepHours must be between 0 and 24",
  "path": "/api/users/user-123/days/2026-08-18"
}
```

Implement centralized exception handling using:

```text
@RestControllerAdvice
```

Handle at minimum:

* validation errors
* invalid date format
* resource not found
* Firestore failures
* unexpected server errors

Do not expose stack traces or internal Firestore exception details to clients.

---

# 11. Layering

Use the following architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Firestore
```

### Controller

Responsible for:

* HTTP request/response handling
* path variables
* query parameters
* request validation
* HTTP status codes

Controllers must not contain Firestore logic.

### Service

Responsible for:

* business rules
* timestamps
* create/update behavior
* orchestration of day and dream operations
* deletion of a day and its dreams

### Repository

Responsible for:

* Firestore reads/writes/deletes
* collection/document path construction
* Firestore query handling

Keep Firestore-specific code inside repository classes.

---

# 12. Repository Interfaces

Create repositories conceptually equivalent to:

```java
DayLogRepository
DreamRepository
```

Example operations:

```text
DayLogRepository
    findByUserIdAndDate(...)
    save(...)
    delete(...)
    findByUserId(...)
    
DreamRepository
    findByUserIdAndDate(...)
    findById(...)
    save(...)
    delete(...)
    deleteAllForDate(...)
```

The service layer should depend on interfaces rather than directly constructing Firestore queries where possible.

---

# 13. Firestore Configuration

Support configuration through Spring properties/environment variables.

Example:

```properties
spring.application.name=dream-journal-api
google.cloud.project-id=${GOOGLE_CLOUD_PROJECT_ID}
```

The application should support standard Google Application Default Credentials.

Local development should work with:

```bash
gcloud auth application-default login
```

Do not hard-code credentials in the repository.

Do not commit service-account JSON files.

Provide a README section describing:

1. Google Cloud project setup
2. Firestore database creation
3. Authentication setup
4. Local credentials
5. Running the application

---

# 14. Firestore Emulator

Configure the application so local development and automated integration tests can use the Firestore emulator.

Support an environment/property such as:

```text
FIRESTORE_EMULATOR_HOST
```

When the emulator is configured, the application must not attempt to authenticate against production Firestore.

---

# 15. Timestamps

Use server/application-generated timestamps.

For create:

```text
createdAt = now
updatedAt = now
```

For update:

```text
createdAt = existing.createdAt
updatedAt = now
```

The client must not be able to arbitrarily set `createdAt` or `updatedAt`.

---

# 16. Day Deletion Semantics

Deleting:

```text
users/{userId}/days/{date}
```

must also delete:

```text
users/{userId}/days/{date}/dreams/*
```

Implement this safely.

Do not assume that deleting a Firestore parent document automatically deletes its subcollection; explicitly delete the dream documents.

For a large number of dreams, use batched writes / pagination as appropriate.

---

# 17. User Resource

For the first version, do not create a complex user-management system.

Treat `userId` as an externally supplied identifier.

The API should be designed so that authentication can later provide the user ID from a JWT/session rather than from the URL.

The architecture should therefore keep ownership logic in the service layer and avoid embedding assumptions about how the user is authenticated.

Authentication/authorization is out of scope for this initial implementation unless explicitly added later.

---

# 18. CORS

Allow the React Native/mobile client to call the API.

For local development, document how to configure allowed origins.

Do not use unrestricted CORS in production configuration.

---

# 19. Logging

Log:

* incoming operation type
* user ID
* date
* resource ID where applicable
* success/failure
* duration where practical

Do not log:

* full dream text
* sensitive user content
* credentials
* authentication tokens

Use structured logging where practical.

---

# 20. Testing Requirements

Implement unit and integration tests.

## Unit tests

Test at minimum:

### DayLogService

* create a new day
* update an existing day
* preserve `createdAt`
* update `updatedAt`
* get existing day
* get missing day
* delete day
* delete day with dreams

### DreamService

* create dream
* default empty tags
* update dream
* preserve `createdAt`
* delete dream
* get dream
* list dreams ordered by `sortOrder`
* deterministic ordering when `sortOrder` is equal

### Validation

Test:

* invalid date
* blank dream text
* invalid sleep hours
* negative sort order
* blank user ID

## Integration tests

Run against the Firestore emulator.

Cover:

* create day → retrieve day
* create dream → retrieve dream
* create multiple dreams → list ordered dreams
* update day
* update dream
* delete dream
* delete day and verify dreams are also removed
* multiple users cannot access each other's records

---

# 21. API Documentation

Use OpenAPI/Swagger.

Document:

* every endpoint
* request bodies
* response bodies
* validation constraints
* possible HTTP status codes
* example requests/responses

Expose Swagger UI in development.

---

# 22. Health Endpoint

Provide:

```http
GET /actuator/health
```

The application should report an appropriate health status.

Add Spring Boot Actuator.

---

# 23. Project Deliverables

Codex should produce:

1. Complete Spring Boot project.
2. Maven build configuration.
3. Firestore configuration.
4. Domain models.
5. Request/response DTOs.
6. Repository interfaces and Firestore implementations.
7. Service layer.
8. REST controllers.
9. Global exception handling.
10. Validation.
11. OpenAPI documentation.
12. Unit tests.
13. Firestore emulator integration tests.
14. README with setup and run instructions.
15. Example environment/configuration file.

---

# 24. Coding Standards

Follow these rules:

* Prefer immutable DTOs where practical.
* Use constructor injection.
* Keep controllers thin.
* Keep Firestore-specific APIs inside repositories.
* Avoid unnecessary abstractions.
* Use meaningful class and method names.
* Do not duplicate Firestore path construction logic.
* Avoid exposing Firestore `DocumentSnapshot` objects outside repository implementations.
* Do not use generic `Map<String, Object>` throughout the application when typed models are practical.
* Add JavaDoc only where it improves understanding; do not generate excessive comments.
* Fail fast on invalid configuration.
* Keep the implementation easy to extend.

---

# 25. Suggested Classes

A reasonable initial structure is:

```text
src/main/java/com/example/dreamjournal/

    DreamJournalApplication.java

    config/
        FirestoreConfig.java
        OpenApiConfig.java

    controller/
        DayLogController.java
        DreamController.java

    service/
        DayLogService.java
        DreamService.java

    repository/
        DayLogRepository.java
        DreamRepository.java
        firestore/
            FirestoreDayLogRepository.java
            FirestoreDreamRepository.java

    model/
        DayLog.java
        Dream.java

    dto/
        DayLogRequest.java
        DayLogResponse.java
        DreamRequest.java
        DreamResponse.java
        DayDetailsResponse.java
        ErrorResponse.java

    exception/
        ResourceNotFoundException.java
        GlobalExceptionHandler.java
```

---

# 26. REST API Summary

| Method | Endpoint                                           | Purpose               |
| ------ | -------------------------------------------------- | --------------------- |
| PUT    | `/api/users/{userId}/days/{date}`                  | Create/update day log |
| GET    | `/api/users/{userId}/days/{date}`                  | Get day log           |
| GET    | `/api/users/{userId}/days`                         | List day logs         |
| DELETE | `/api/users/{userId}/days/{date}`                  | Delete day + dreams   |
| POST   | `/api/users/{userId}/days/{date}/dreams`           | Create dream          |
| GET    | `/api/users/{userId}/days/{date}/dreams`           | List dreams           |
| GET    | `/api/users/{userId}/days/{date}/dreams/{dreamId}` | Get dream             |
| PUT    | `/api/users/{userId}/days/{date}/dreams/{dreamId}` | Update dream          |
| DELETE | `/api/users/{userId}/days/{date}/dreams/{dreamId}` | Delete dream          |
| GET    | `/api/users/{userId}/days/{date}/details`          | Get day + dreams      |
| GET    | `/actuator/health`                                 | Health check          |

---

# 27. Important Design Decision

The SQLite `dreams_new.id INTEGER AUTOINCREMENT` should **not** be carried over directly.

Use a Firestore-generated string document ID for dreams.

The API will expose:

```json
"id": "firestore-generated-id"
```

This avoids trying to reproduce relational auto-increment semantics in Firestore.

The SQLite `tags TEXT DEFAULT '[]'` should also be converted into a native Firestore array:

```json
"tags": []
```

rather than storing serialized JSON.

---

# 28. Non-Goals for Version 1

Do not implement the following unless required by a later specification:

* user registration
* login/signup
* OAuth
* JWT authentication
* password management
* social login
* analytics
* AI dream interpretation
* full-text dream search
* dream sharing
* comments/likes
* notifications
* image/audio attachments
* offline synchronization
* automatic conflict resolution

The goal of version 1 is a reliable CRUD backend for sleep/day logs and dreams.

---

# 29. Acceptance Criteria

The implementation is complete when:

1. The application starts successfully with Spring Boot.
2. Firestore credentials/configuration are externalized.
3. The Firestore emulator can be used for local development and tests.
4. A user can create/update/read/delete a day log.
5. A user can create/read/update/delete dreams for a day.
6. Multiple dreams can belong to the same date.
7. Dreams are returned in `sortOrder`.
8. Tags are stored as Firestore arrays.
9. `createdAt` is immutable after creation.
10. `updatedAt` changes on updates.
11. Deleting a day also deletes its dreams.
12. Missing records return `404`.
13. Invalid input returns `400` with a consistent error payload.
14. Data for one `userId` cannot accidentally be returned under another user ID.
15. Unit tests pass.
16. Firestore emulator integration tests pass.
17. Swagger/OpenAPI documentation is available.
18. README instructions allow a developer to run the application locally from a clean checkout.

---

# 30. Codex Implementation Guidance

Implement this specification incrementally.

Start with:

1. Spring Boot project and dependencies.
2. Firestore configuration.
3. Domain models and DTOs.
4. Firestore repositories.
5. Services.
6. REST controllers.
7. Validation and exception handling.
8. Tests.
9. OpenAPI.
10. README.

Before considering the implementation complete, run the full test suite and verify all CRUD flows against the Firestore emulator.

Prefer a simple, maintainable implementation over introducing unnecessary frameworks or abstractions.

Where this specification leaves implementation choices open, choose the simplest production-appropriate Spring Boot/Firestore solution and document the choice in the README.
