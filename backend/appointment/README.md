# Appointment API

The service uses a UTC in-memory H2 database by default. Replace the datasource
settings in `src/main/resources/application.properties` with your production
database settings before deployment.

## Endpoints

Create a provider:

```http
POST /api/providers
Content-Type: application/json

{"name":"Dr. Smith"}
```

Book an appointment. Timestamps must include a UTC offset or `Z`; they are
parsed as `Instant` and stored using UTC:

```http
POST /api/appointments
Content-Type: application/json

{
  "providerId": 1,
  "customerId": 23,
  "startsAt": "2030-04-01T10:00:00Z",
  "endsAt": "2030-04-01T10:30:00Z"
}
```

Retrieve or cancel:

```http
GET  /api/appointments/{id}
POST /api/appointments/{id}/cancel
```

Get all appointments for a provider (includes cancelled appointments, ordered
by start time):

```http
GET /api/providers/1/appointments
```

List active provider appointments that overlap a UTC time window. `from` is
inclusive and `to` is exclusive for adjacency purposes:

```http
GET /api/providers/1/appointments?from=2030-04-01T10:00:00Z&to=2030-04-01T12:00:00Z
```

The window must have `to` later than `from`; results are ordered by start time.
Cancelled appointments are not included.

Booking intervals are half-open (`[startsAt, endsAt)`), so adjacent bookings
are accepted. Active bookings are serialized per provider using a pessimistic
database lock before checking for overlap. Cancellation takes the same lock.
The lock protects concurrent requests across application instances when they
share a relational database that supports pessimistic row locking.

Run the backend tests with `mvnw.cmd test` on Windows.
