# Dream Journal API

Spring Boot REST API for sleep day logs and dreams, backed by Google Cloud Firestore.

## Requirements

- Java 21 or newer
- Maven 3.9+
- Google Cloud project with Firestore enabled, or the Firestore emulator for local testing

## Configuration

Copy the example file and set values for your environment:

```bash
cp src/main/resources/application-example.properties src/main/resources/application-local.properties
```

Important settings:

```properties
google.cloud.project-id=your-google-cloud-project-id
app.cors.allowed-origins=http://localhost:8081,http://localhost:3000
```

The app uses Google Application Default Credentials in normal mode. It never expects a committed service-account JSON file.

## Google Cloud Setup

1. Create or choose a Google Cloud project.
2. Enable Firestore in Native mode.
3. Set the project ID:

```bash
export GOOGLE_CLOUD_PROJECT_ID=your-google-cloud-project-id
```

4. Authenticate locally:

```bash
gcloud auth application-default login
```

## Firestore Emulator

For local development or integration tests:

```bash
gcloud components install cloud-firestore-emulator
gcloud beta emulators firestore start --host-port=localhost:8080
export FIRESTORE_EMULATOR_HOST=localhost:8080
export GOOGLE_CLOUD_PROJECT_ID=dream-journal-local
```

When `FIRESTORE_EMULATOR_HOST` is set, the application uses the emulator without production Firestore credentials.

## Run

```bash
mvn spring-boot:run
```

The API is served from `/api`. Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

Health check:

```text
GET /actuator/health
```

## Test

Run unit and MVC validation tests:

```bash
mvn test
```

Firestore repository integration tests are included and run only when `FIRESTORE_EMULATOR_HOST` is present:

```bash
export FIRESTORE_EMULATOR_HOST=localhost:8080
export GOOGLE_CLOUD_PROJECT_ID=dream-journal-test
mvn test
```

## API Summary

- `PUT /api/users/{userId}/days/{date}` creates or updates a day log.
- `GET /api/users/{userId}/days/{date}` returns one day log.
- `GET /api/users/{userId}/days?from=yyyy-MM-dd&to=yyyy-MM-dd` lists day logs descending by date.
- `DELETE /api/users/{userId}/days/{date}` deletes the day and its dreams.
- `POST /api/users/{userId}/days/{date}/dreams` creates a dream.
- `GET /api/users/{userId}/days/{date}/dreams` lists dreams by `sortOrder`, then `createdAt`.
- `GET /api/users/{userId}/days/{date}/dreams/{dreamId}` returns one dream.
- `PUT /api/users/{userId}/days/{date}/dreams/{dreamId}` updates one dream.
- `DELETE /api/users/{userId}/days/{date}/dreams/{dreamId}` deletes one dream.
- `GET /api/users/{userId}/days/{date}/details` returns sleep plus dreams for a mobile-friendly day view.

Dates must use `yyyy-MM-dd`. Tags are stored as native Firestore arrays and returned as `[]` when absent.
