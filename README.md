# 🌌 DreamAtlas Backend

## About

**DreamAtlas Backend** is a Spring Boot REST API that powers the DreamAtlas application.

It provides secure, user-scoped APIs for:

* 📖 Dream journaling
* 🎙️ AI-assisted dream transcription and extraction
* 🧠 Weekly AI dream insights
* 😴 Sleep analytics
* ❤️ Heart-rate data
* 🔗 Google Health / Google Fit integration
* 📊 BigQuery health-data ingestion
* 🔐 Firebase-based authentication

The backend uses **Google Cloud Firestore** as its primary application datastore and **Google Cloud BigQuery** for health-data storage and analytics.

Gemini 2.5 Flash, running through Vertex AI, is used for AI-assisted dream processing and weekly insight generation.

---

# 🛠️ Tech Stack

| Technology                             | Purpose                             |
| -------------------------------------- | ----------------------------------- |
| ☕ **Java 21**                          | Backend programming language        |
| 🍃 **Spring Boot 3.3.5**               | REST API framework                  |
| 📦 **Maven**                           | Build and dependency management     |
| 🔥 **Firebase Authentication**         | User authentication                 |
| 🗄️ **Google Cloud Firestore**         | Primary application datastore       |
| 📊 **Google Cloud BigQuery**           | Health data storage and analytics   |
| 🤖 **Gemini 2.5 Flash**                | Dream transcription and AI analysis |
| ☁️ **Vertex AI**                       | Gemini model platform               |
| ❤️ **Google Health / Google Fit APIs** | Sleep and heart-rate data           |
| 🔑 **Google OAuth 2.0**                | Health account connection           |
| 📚 **OpenAPI 3 / SpringDoc**           | API documentation                   |
| 🩺 **Spring Boot Actuator**            | Application health monitoring       |

---

# 📋 Prerequisites

Before running the backend locally, install:

* ☕ **Java 21 / JDK 21**
* 📦 **Maven 3.9+**
* ☁️ **Google Cloud CLI (`gcloud`)**
* 🔥 A configured **Firebase project**
* 🗄️ **Google Cloud Firestore** in Native mode
* 📊 **Google Cloud BigQuery**
* 🤖 **Vertex AI**
* 🔑 **Google OAuth 2.0 credentials** for Google Health / Google Fit

Your Google Cloud project should have the required APIs and services enabled.

---

# ⚡ Quick Setup

## 1. Clone the Repository

```bash
git clone <repository-url>
cd <repository-directory>
```

If you are working on a specific branch:

```bash
git checkout <branch-name>
```

---

## 2. Configure Environment Variables

Create a `.env` file in the project root:

```bash
touch .env
```

Add your configuration:

```env
# Google Health / Fit OAuth
GOOGLE_HEALTH_CLIENT_ID="your-client-id.apps.googleusercontent.com"
GOOGLE_HEALTH_CLIENT_SECRET="your-client-secret"

# Google Cloud
GOOGLE_CLOUD_PROJECT="your-google-cloud-project-id"
GOOGLE_CLOUD_PROJECT_ID="your-google-cloud-project-id"

# Firestore
GOOGLE_CLOUD_DATABASE_ID="(default)"

# BigQuery
GOOGLE_CLOUD_BIGQUERY_DATASET="dream_atlas_health"

# Google Health OAuth callback
GOOGLE_HEALTH_REDIRECT_URI="http://localhost:8080/api/health/google/callback"

# CORS
APP_CORS_ALLOWED_ORIGINS="http://localhost:8081,http://localhost:3000"
```

An `.env.example` template is included in the repository.

> **Never commit `.env` or other files containing secrets.**

---

## 3. Authenticate with Google Cloud

Authenticate your local Google Cloud environment:

```bash
gcloud auth login
gcloud auth application-default login
```

Set your Google Cloud project:

```bash
export GOOGLE_CLOUD_PROJECT="<your-gcp-project-id>"
export GOOGLE_CLOUD_LOCATION="global"
export GOOGLE_GENAI_USE_VERTEXAI="true"
```

Configure the quota project:

```bash
gcloud auth application-default set-quota-project <your-gcp-project-id>
```

Application Default Credentials allow the backend's Google Cloud integrations to authenticate without hardcoding service-account keys.

---

## 4. Build the Application

```bash
mvn clean compile
```

---

## 5. Start the Server

```bash
mvn spring-boot:run
```

The backend runs by default at:

```text
http://localhost:8080
```

API routes are available under:

```text
/api
```

---

# 🔌 API Endpoints

All user-scoped endpoints require Firebase authentication.

The API is organized into the following areas:

```text
/api/users/{userId}/days
/api/users/{userId}/days/{date}/dreams
/api/users/{userId}/dreams
/api/users/{userId}/insights
/api/users/{userId}/sleep
/api/health
```

---

## 📅 Day Logs

**Base route:**

```text
/api/users/{userId}/days
```

| Method   | Endpoint                                  | Description                     |
| -------- | ----------------------------------------- | ------------------------------- |
| `PUT`    | `/api/users/{userId}/days/{date}`         | Create or update a day log      |
| `GET`    | `/api/users/{userId}/days/{date}`         | Get a day log                   |
| `GET`    | `/api/users/{userId}/days`                | List day logs                   |
| `DELETE` | `/api/users/{userId}/days/{date}`         | Delete a day log and its dreams |
| `GET`    | `/api/users/{userId}/days/{date}/details` | Get day details and dreams      |

Dates use:

```text
yyyy-MM-dd
```

---

## 🌙 Dreams

**Base route:**

```text
/api/users/{userId}/days/{date}/dreams
```

| Method   | Endpoint              | Description                 |
| -------- | --------------------- | --------------------------- |
| `POST`   | `/dreams`             | Create a dream manually     |
| `POST`   | `/dreams/ai`          | AI audio-to-dream ingestion |
| `PUT`    | `/dreams/order`       | Reorder dreams              |
| `GET`    | `/dreams`             | List dreams                 |
| `GET`    | `/dreams/{dreamId}`   | Get a dream                 |
| `PUT`    | `/dreams/{dreamId}`   | Update a dream              |
| `DELETE` | `/dreams/{dreamId}`   | Delete a dream              |
| `GET`    | `/dreams/tags/recent` | Get recently used tags      |

### AI Dream Ingestion

```text
POST /api/users/{userId}/days/{date}/dreams/ai
```

Accepts an audio recording using `multipart/form-data`.

The backend sends the recording to Gemini 2.5 Flash to:

1. Transcribe the recording
2. Identify structured dream content
3. Extract dream information
4. Return structured dream data to the application

---

## 🏷️ Dream Search & Tags

```text
/api/users/{userId}/dreams
```

| Method | Endpoint       | Description              |
| ------ | -------------- | ------------------------ |
| `GET`  | `/tags/recent` | Get recently used tags   |
| `GET`  | `/search`      | Search and filter dreams |

Supported search filters include:

```text
text
mood
dreamType
tag
```

---

## 🧠 Weekly AI Insights

```text
/api/users/{userId}/insights
```

| Method | Endpoint          | Description                       |
| ------ | ----------------- | --------------------------------- |
| `GET`  | `/weekly`         | Fetch or generate weekly insights |
| `POST` | `/weekly/refresh` | Force-regenerate weekly insights  |

Weekly insights use Gemini to analyze a user's dream journals and generate:

* Recurring themes
* Weekly reflections
* Emotional patterns
* Jungian-inspired interpretations

---

## 😴 Sleep Analytics

```text
/api/users/{userId}/sleep
```

| Method | Endpoint       | Description                         |
| ------ | -------------- | ----------------------------------- |
| `GET`  | `/sleep`       | Retrieve detailed sleep sessions    |
| `GET`  | `/sleep/stats` | Retrieve aggregate sleep statistics |

Date ranges can be provided using:

```text
startDate=yyyy-MM-dd
endDate=yyyy-MM-dd
```

---

## ❤️ Google Health / Fit

```text
/api/health
```

| Method | Endpoint                 | Description                      |
| ------ | ------------------------ | -------------------------------- |
| `GET`  | `/google/connect`        | Start Google OAuth               |
| `GET`  | `/google/callback`       | OAuth callback                   |
| `GET`  | `/google/identity`       | Get linked Google account        |
| `GET`  | `/google/sleep/raw`      | Retrieve raw sleep data          |
| `GET`  | `/google/heart-rate/raw` | Retrieve raw heart-rate data     |
| `GET`  | `/google/sleep-health`   | Retrieve normalized sleep data   |
| `POST` | `/google/ingest`         | Ingest health data into BigQuery |

The OAuth callback is the exception to the normal Firebase authentication requirement because it completes the Google OAuth flow.

---

## 🧪 Diagnostics

```text
POST /api/test/audio
```

Tests audio-to-dream extraction directly against Gemini.

```text
GET /actuator/health
```

Returns the application's health status.

---

# 🏗️ Architecture

DreamAtlas follows a layered architecture separating HTTP handling, business logic, persistence, authentication, and external integrations.

```text
                         ┌─────────────────────┐
                         │   DreamAtlas App    │
                         │  React Native/Expo  │
                         └──────────┬──────────┘
                                    │
                             REST / HTTPS
                                    │
                                    ▼
                    ┌──────────────────────────────┐
                    │     DreamAtlas Backend       │
                    │        Spring Boot           │
                    │                              │
                    │ Controllers                  │
                    │       ↓                      │
                    │ Services                     │
                    │       ↓                      │
                    │ Repositories                 │
                    └───────┬──────────┬───────────┘
                            │          │
                            ▼          ▼
                    ┌────────────┐  ┌────────────┐
                    │ Firestore │  │  BigQuery  │
                    │            │  │            │
                    │ Dreams     │  │ Sleep      │
                    │ Day Logs   │  │ Stages     │
                    │ Insights   │  │ Heart Rate │
                    └────────────┘  └──────┬─────┘
                                           │
                                           ▲
                                  ┌────────┴────────┐
                                  │ Google Health   │
                                  │ / Google Fit    │
                                  └─────────────────┘

                         ┌─────────────────────┐
                         │      Vertex AI      │
                         │  Gemini 2.5 Flash   │
                         │                     │
                         │ Audio → Dreams      │
                         │ Weekly Insights     │
                         └─────────────────────┘
```

---

# 📂 Code / Project Structure

```text
src/
└── main/
    ├── java/com/example/dreamjournal/
    │
    ├── config/
    │   ├── BigQueryConfig.java
    │   ├── FirebaseAdminConfig.java
    │   ├── FirestoreConfig.java
    │   ├── OpenApiConfig.java
    │   └── WebConfig.java
    │
    ├── controller/
    │   ├── DayLogController.java
    │   ├── DreamController.java
    │   ├── DreamTagsController.java
    │   ├── GeminiController.java
    │   └── WeeklyInsightController.java
    │
    ├── dto/
    │   ├── DayDetailsResponse.java
    │   ├── DayLogRequest.java
    │   ├── DayLogResponse.java
    │   ├── DreamRequest.java
    │   ├── DreamResponse.java
    │   ├── GeminiDreamResponse.java
    │   ├── GeminiWeeklyInsightResponse.java
    │   ├── WeeklyInsightData.java
    │   ├── WeeklyInsightResponse.java
    │   └── ...
    │
    ├── exception/
    │   ├── FirestoreOperationException.java
    │   ├── GlobalExceptionHandler.java
    │   └── ResourceNotFoundException.java
    │
    ├── health/
    │   ├── controller/
    │   ├── model/
    │   ├── repository/
    │   └── service/
    │
    ├── model/
    │   ├── DayLog.java
    │   └── Dream.java
    │
    ├── repository/
    │   ├── DayLogRepository.java
    │   ├── DreamRepository.java
    │   ├── WeeklyInsightRepository.java
    │   └── firestore/
    │
    ├── security/
    │   ├── FirebaseAuthenticationFilter.java
    │   └── FirebaseUser.java
    │
    └── service/
        ├── DateParser.java
        ├── DayLogService.java
        ├── DreamService.java
        ├── GeminiService.java
        ├── RequestGuards.java
        └── WeeklyInsightService.java

    resources/
    ├── application-example.properties
    └── application.properties
```

---

# 🧩 Package Responsibilities

### `controller/`

Contains REST API endpoints.

Controllers handle incoming HTTP requests and delegate business operations to services.

---

### `service/`

Contains the application's business logic.

Examples:

```text
DayLogService
DreamService
GeminiService
WeeklyInsightService
```

The health subsystem contains its own services for Google Health integration, validation, ingestion, mapping, and sleep analytics.

---

### `repository/`

Defines the persistence layer.

The main repositories interact with Firestore, while the health subsystem contains repositories for BigQuery and health-ingestion state.

---

### `dto/`

Contains Data Transfer Objects used by the API.

DTOs define the request and response structures exposed to the frontend.

---

### `model/`

Contains the core domain models.

The main application models are:

```text
DayLog
Dream
```

Health-specific models live under:

```text
health/model/
```

---

### `security/`

Contains authentication and user identity handling.

`FirebaseAuthenticationFilter` validates Firebase ID tokens and establishes the authenticated Firebase user for each request.

---

### `exception/`

Contains application-specific exceptions and centralized exception handling.

---

### `health/`

Contains the complete health-data subsystem:

```text
health/
├── controller/
├── model/
├── repository/
└── service/
```

This keeps health ingestion and analytics logically separate from the core dream-journaling domain.

---

# 🔐 Authentication & Security

All `/api/*` requests, except the Google OAuth callback, require a valid Firebase ID token.

Clients send:

```http
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

The authentication flow is:

```text
Frontend
   │
   │ Firebase ID Token
   ▼
FirebaseAuthenticationFilter
   │
   │ Verify token
   ▼
FirebaseUser
   │
   │ Firebase UID
   ▼
Controller
   │
   ▼
User-scoped Service
   │
   ▼
User-owned Data
```

This ensures that API requests are associated with an authenticated Firebase user and that user-scoped resources are protected.

---

# 🗄️ Data Model

Dream and day-log data is stored in Firestore using a user-scoped hierarchy:

```text
users/{userId}
    └── days/{yyyy-MM-dd}
        ├── sleepHours
        ├── sleepQuality
        ├── createdAt
        ├── updatedAt
        │
        └── dreams/{dreamId}
            ├── text
            ├── mood
            ├── dreamType
            ├── tags
            ├── sortOrder
            ├── createdAt
            └── updatedAt
```

This structure keeps all dream data associated with its owning user.

---

# 📊 Health Data Pipeline

Health data follows a separate ingestion pipeline:

```text
Google Health / Fit
        │
        ▼
GoogleHealthService
        │
        ▼
GoogleHealthMapper
        │
        ▼
HealthDataValidator
        │
        ▼
HealthIngestionService
        │
        ├──────────────► Firestore
        │                 Ingestion State
        │
        ▼
BigQueryStorageWriter
        │
        ▼
Google Cloud BigQuery
        │
        ├── Sleep Sessions
        ├── Sleep Stages
        └── Heart Rate
```

The implementation lives under:

```text
health/
```

with dedicated controllers, models, repositories, and services.

---

# 🤖 Gemini Integration

Gemini 2.5 Flash is used for AI-powered functionality.

```text
Audio Recording
      │
      ▼
Gemini 2.5 Flash
      │
      ├── Transcription
      │
      └── Structured Dream Extraction
```

Gemini is also used for weekly dream analysis:

```text
Dream Journals
      │
      ▼
Gemini 2.5 Flash
      │
      ▼
Weekly Insights
      │
      ├── Recurring Themes
      ├── Reflections
      └── Emotional Patterns
```

AI functionality is primarily encapsulated in:

```text
service/GeminiService.java
```

---

# 🧪 Development & Testing

## Compile

```bash
mvn clean compile
```

## Run Tests

```bash
mvn test
```

## Run the Application

```bash
mvn spring-boot:run
```

---

# 📚 API Documentation

Once the backend is running, the API can be explored through Swagger UI.

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI Specification

```text
http://localhost:8080/v3/api-docs
```

### Application Health

```text
http://localhost:8080/actuator/health
```

---


# 🔒 Environment & Secrets

Never commit sensitive credentials to the repository.

Do not commit:

* `.env`
* Google OAuth client secrets
* Firebase service-account credentials
* Google Cloud service-account keys
* Private API keys
* OAuth refresh tokens
* Other authentication credentials

Use environment variables and Google Cloud Application Default Credentials instead.

---

# 🌌 DreamAtlas

**DreamAtlas connects dreams with sleep.**

The backend brings together:

```text
Dream Journaling
        +
AI / Gemini
        +
Firebase Authentication
        +
Firestore
        +
Google Health / Fit
        +
BigQuery
        ↓
A unified dream + sleep experience
```
