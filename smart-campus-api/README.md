# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures — University of Westminster
**Coursework weighting:** 60 % of final grade
**Framework:** JAX-RS (Jersey 2.41) on Tomcat 9 · Java 8 · Maven

A RESTful web service that manages physical **Rooms** on a university campus
and the **Sensors** deployed inside them (temperature, CO₂, occupancy, …).
Every sensor keeps a full **historical log of readings**, exposed as a nested
sub-resource. Error handling, filtering, and request / response observability
are built in.

---

## 1. API Design Overview

### Resource model

```
Room   1 ──── *  Sensor   1 ──── *  SensorReading
```

A `Room` contains many `Sensor`s (tracked by the `sensorIds` list on the
room). Each `Sensor` owns a historical list of `SensorReading`s, exposed
through a sub-resource locator at `/sensors/{id}/readings`.

### URI map

| Verb     | Path                                     | Purpose                                                                   |
| -------- | ---------------------------------------- | ------------------------------------------------------------------------- |
| `GET`    | `/api/v1`                                | Discovery document (HATEOAS links, version, contact, stats)               |
| `GET`    | `/api/v1/rooms`                          | List every room                                                           |
| `POST`   | `/api/v1/rooms`                          | Create a room → **201 Created + Location header**                         |
| `GET`    | `/api/v1/rooms/{roomId}`                 | Fetch a single room                                                       |
| `DELETE` | `/api/v1/rooms/{roomId}`                 | Decommission a room (**409 Conflict** if it still has sensors)            |
| `GET`    | `/api/v1/sensors`                        | List every sensor · supports optional `?type=` filter                     |
| `POST`   | `/api/v1/sensors`                        | Register a sensor (**422** if `roomId` does not exist)                    |
| `GET`    | `/api/v1/sensors/{sensorId}`             | Fetch a single sensor                                                     |
| `PUT`    | `/api/v1/sensors/{sensorId}`             | Update sensor status / type (used to flip to MAINTENANCE for the 403 demo)|
| `GET`    | `/api/v1/sensors/{sensorId}/readings`    | Reading history for this sensor                                           |
| `POST`   | `/api/v1/sensors/{sensorId}/readings`    | Append a reading (**403** if sensor is in MAINTENANCE; updates `currentValue`) |

### HTTP status codes used

| Code  | Meaning                        | Where it fires                                                  |
| ----- | ------------------------------ | --------------------------------------------------------------- |
| `200` | OK                             | Successful GET / DELETE / PUT                                   |
| `201` | Created                        | Successful POST of a room, sensor, or reading                   |
| `400` | Bad Request                    | Missing required fields in the payload                          |
| `403` | Forbidden                      | Posting a reading to a MAINTENANCE sensor                       |
| `404` | Not Found                      | Unknown room / sensor ID on GET / DELETE                        |
| `409` | Conflict                       | Duplicate ID, or deleting a room that still has sensors         |
| `415` | Unsupported Media Type         | Client sends non-JSON body (enforced by `@Consumes`)            |
| `422` | Unprocessable Entity           | JSON is valid but refers to a non-existent `roomId`             |
| `500` | Internal Server Error          | Catch-all for unexpected runtime errors (no stack trace leaked) |

### Data storage

The coursework forbids databases, so everything lives in a process-wide
singleton `DataStore` built on `ConcurrentHashMap`s and synchronized lists.
Data is seeded on startup and lost on restart.

### Seed data

| ID         | Type          | Status        | Room     |
| ---------- | ------------- | ------------- | -------- |
| `TEMP-001` | Temperature   | `ACTIVE`      | LIB-301  |
| `CO2-001`  | CO2           | `ACTIVE`      | LAB-101  |
| `OCC-001`  | Occupancy     | `MAINTENANCE` | LAB-101  |

| Room ID   | Name                 | Capacity | Sensors           |
| --------- | -------------------- | -------- | ----------------- |
| `LIB-301` | Library Quiet Study  | 50       | TEMP-001          |
| `LAB-101` | Computer Science Lab | 30       | CO2-001, OCC-001  |
| `CR-205`  | Conference Room 205  | 20       | *(none — safe to DELETE)* |

`OCC-001` is deliberately `MAINTENANCE` so the 403-Forbidden flow is one
click away in the video demo. `CR-205` is deliberately empty so the
“successful DELETE” path is also one click away.

---

## 2. Build & Run

### Prerequisites

* **JDK 8+** (tested with JDK 8 and 11)
* **Maven 3.6+**
* **Apache Tomcat 9.x**  (Tomcat 10 uses the `jakarta.*` namespace — this
  project targets `javax.*`, so it needs Tomcat 9)

### Step-by-step

1. **Clone the repository**

   ```bash
   git clone https://github.com/<your-username>/smart-campus-api.git
   cd smart-campus-api
   ```

2. **Package the application**

   ```bash
   mvn clean package
   ```

   A WAR file appears at `target/smart-campus-api.war`.

3. **Deploy to Tomcat 9**

   Copy the WAR into Tomcat's `webapps/` directory:

   ```bash
   cp target/smart-campus-api.war $CATALINA_HOME/webapps/
   ```

4. **Start Tomcat**

   ```bash
   $CATALINA_HOME/bin/startup.sh     # macOS / Linux
   %CATALINA_HOME%\bin\startup.bat   # Windows
   ```

5. **Verify the service is up**

   ```bash
   curl http://localhost:8080/smart-campus-api/api/v1
   ```

   You should see the discovery JSON document.

### Running from NetBeans

Right-click the project → *Run*. NetBeans will build the WAR, deploy it to
the bundled Tomcat, and open a browser at the context root. Navigate to
`/smart-campus-api/api/v1` to hit the discovery endpoint.

---

## 3. Sample `curl` Commands

Below are seven sample commands covering every major interaction point.
Replace the host / port if your Tomcat runs elsewhere.

### 3.1 — Discovery

```bash
curl -i http://localhost:8080/smart-campus-api/api/v1
```

### 3.2 — List every room

```bash
curl -i http://localhost:8080/smart-campus-api/api/v1/rooms
```

### 3.3 — Create a new room (expect `201 Created` + `Location` header)

```bash
curl -i -X POST \
     -H "Content-Type: application/json" \
     -d '{"id":"LAB-202","name":"Networking Lab","capacity":25}' \
     http://localhost:8080/smart-campus-api/api/v1/rooms
```

### 3.4 — Register a sensor (valid roomId)

```bash
curl -i -X POST \
     -H "Content-Type: application/json" \
     -d '{"id":"HUM-001","type":"Humidity","status":"ACTIVE","currentValue":45.0,"roomId":"LAB-202"}' \
     http://localhost:8080/smart-campus-api/api/v1/sensors
```

### 3.5 — Register a sensor with a non-existent roomId (expect `422`)

```bash
curl -i -X POST \
     -H "Content-Type: application/json" \
     -d '{"id":"BAD-001","type":"Temperature","status":"ACTIVE","roomId":"DOES-NOT-EXIST"}' \
     http://localhost:8080/smart-campus-api/api/v1/sensors
```

### 3.6 — Filter sensors by type

```bash
curl -i "http://localhost:8080/smart-campus-api/api/v1/sensors?type=Temperature"
```

### 3.7 — Post a reading to a MAINTENANCE sensor (expect `403 Forbidden`)

```bash
curl -i -X POST \
     -H "Content-Type: application/json" \
     -d '{"value":17.3}' \
     http://localhost:8080/smart-campus-api/api/v1/sensors/OCC-001/readings
```

### 3.8 — Delete a room that still has sensors (expect `409 Conflict`)

```bash
curl -i -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```

### 3.9 — Delete an empty room (expect `200`)

```bash
curl -i -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/CR-205
```

---

## 4. Report — Answers to the Conceptual Questions

> The coursework allocates **20 %** of every task's marks to the written
> answers to the question in that task. They are all collected here.

### Part 1.1 — JAX-RS Resource lifecycle and data consistency

By default, a JAX-RS runtime such as Jersey treats every resource class as
**request-scoped**. A brand new instance of `RoomResource`, `SensorResource`
etc. is created for every incoming HTTP request and discarded as soon as
the response has been written. The runtime can be told to use a singleton
by registering the class differently (e.g. via `getSingletons()`), but that
is not what we do here.

This per-request model has three important consequences for state:

1. **Any field on a resource class is wiped between requests.** We cannot
   therefore store the room registry as a field on `RoomResource` — it
   would vanish the instant the response flushes. The application data has
   to live somewhere that outlives individual requests.
2. **Concurrent requests execute on different threads against potentially
   shared data.** Two threads can be simultaneously inside `POST /sensors`
   adding entries to the same backing collection. An ordinary `HashMap`
   does not tolerate this — concurrent modifications can produce lost
   writes, infinite loops (in some Java versions), or even corrupt the
   table's internal bucket structure.
3. **List mutations are just as risky.** Appending to an `ArrayList` from
   two threads can lose elements or throw `ArrayIndexOutOfBoundsException`.

Our `DataStore` addresses these points by (a) being a process-wide
singleton obtained through `DataStore.getInstance()`, (b) using
`ConcurrentHashMap` for the top-level room / sensor / readings maps, and
(c) wrapping the per-sensor reading lists in `Collections.synchronizedList`.
Critical read-modify-write sequences on a room's `sensorIds` list are
further guarded by an explicit `synchronized` block, because
`synchronizedList` only synchronizes individual method calls, not
compound operations.

### Part 1.2 — Why HATEOAS is a hallmark of advanced REST

HATEOAS (*Hypermedia As The Engine Of Application State*) is the idea
that each response should tell the client what it can do next by
embedding links to related resources, rather than the client knowing
those paths ahead of time from static documentation.

Benefits for client developers:

* **Self-describing responses.** A freshly created sensor comes back
  with links to `self`, `readings` and `all-sensors`. The client does
  not have to concatenate strings like `"/api/v1/sensors/" + id +
  "/readings"` — it just follows the link, which is much harder to get
  wrong.
* **Loose coupling of URL structure.** If the team later decides to
  rename `/readings` to `/measurements`, existing clients that follow
  the server-supplied link keep working; clients that hard-coded the
  URL break.
* **Discoverability for generic tooling.** Tools such as Postman, HAL
  browsers or auto-generated SDKs can traverse the API interactively
  because they find the next step inside the payload rather than in a
  PDF.
* **Versioning room.** When `v2` adds new capabilities, the discovery
  document can advertise extra links that old clients simply ignore.

Static documentation still has its place — OpenAPI, Swagger — but it
ages: the doc may claim an endpoint exists that production has removed.
Links inside live responses cannot lie, because they *are* the live
responses.

### Part 2.1 — IDs-only vs full-object responses

Returning **full objects** for every room in `GET /rooms` means the
client gets everything in one round trip: no follow-up calls to fetch
names, capacities, etc. That is great for small collections and for
clients that render tables. The cost is network bandwidth — every row
includes fields the client may not care about — and server-side
serialization work that scales with the number of rooms.

Returning **IDs only** (e.g. `["LIB-301","LAB-101","CR-205"]`) is
cheap on the wire but pushes an N+1 problem onto the client: to render
anything useful, it must call `GET /rooms/{id}` once per ID, which
multiplies HTTP overhead, connection pressure and total latency.

The pragmatic middle ground is:

* Return **summary objects** in list endpoints (the essential fields:
  id, name, capacity — no heavy nested children).
* Let clients fetch the full object with a detail call when they need
  it.
* Support server-side filtering (as we do with `?type=`) so clients
  fetch a scoped slice rather than the whole table.

For this coursework, the list is small and the collection endpoint
returns full objects, which is the simpler and more demo-friendly
choice.

### Part 2.2 — Is DELETE idempotent here?

**Yes — in the sense that REST cares about.** An HTTP method is
idempotent if performing it *n* times leaves the server in the same
state as performing it once. Our `DELETE /rooms/{id}` meets that:

* First call on an existing room: removes the room, returns `200 OK`.
* Second call on the same ID: the room is already gone, so the server
  makes no further change and returns `404 Not Found`.
* Third, fourth, nth call: identical to the second — no change.

The **response code** differs between the first call and the rest, but
that is not what idempotency means. Idempotency is about server state,
not response-code stability. After any number of repeats the server
state is the same: the room is absent.

There is one subtlety. If a client retries a DELETE because it did not
receive a response, receiving a `404` on the retry is slightly
confusing — the client might suspect its original request failed,
when in fact it succeeded. Some APIs choose to return `204 No Content`
for "already gone" to paper over this. We follow the stricter
convention and surface `404` because it communicates what the client
actually sees: the resource is not there.

### Part 3.1 — Consequences of a Content-Type mismatch

We annotate `POST /sensors` with `@Consumes(MediaType.APPLICATION_JSON)`.
If a client sends the same body but sets `Content-Type: text/plain` or
`application/xml`, JAX-RS performs a content-negotiation check *before*
our resource method is ever invoked:

1. Jersey looks for a registered `MessageBodyReader` that can convert
   the declared media type into a `Sensor` object.
2. Because `@Consumes` restricts this method to `application/json`,
   no matching reader is found for the mismatched type.
3. Jersey short-circuits with HTTP **415 Unsupported Media Type** and
   an empty body. Our method never runs, which means our validation
   code never runs, which means the payload cannot silently corrupt
   the data store.

This is a good example of declarative behaviour — we don't have to
write any defensive parsing code; the runtime enforces the contract
for us.

### Part 3.2 — `@QueryParam` vs `@PathParam` for filtering

We implemented the sensor-type filter with `?type=CO2` rather than
`/sensors/type/CO2`. The query-parameter design is preferable for
three reasons:

1. **Paths should identify resources, not operations.**
   `/sensors` is "the collection of all sensors". Appending `/type/CO2`
   invents a resource called "type/CO2" that does not really exist —
   it is a *view* over the existing collection, not a new thing. A
   query parameter makes that relationship explicit: "same collection,
   filtered view".
2. **Query parameters compose.** We can add `?type=CO2&status=ACTIVE`
   or `?type=CO2&roomId=LAB-101` without designing new URL shapes. A
   path-segment design forces either an explosion of sub-paths or a
   clumsy single path such as `/sensors/type/CO2/status/ACTIVE`.
3. **Cache keys stay meaningful.** HTTP caches key on the full URL
   including the query string. `/sensors?type=CO2` and
   `/sensors?type=Temperature` sit in the cache as distinct entries,
   both under the `/sensors` collection. A path-based design would
   split that cache partition across unrelated paths.

Path parameters remain the right tool when the segment *identifies*
something — `/rooms/{roomId}`, `/sensors/{sensorId}/readings` — rather
than filtering a collection.

### Part 4.1 — Architectural benefits of the Sub-Resource Locator pattern

Rather than annotating a method
`@GET @Path("/{sensorId}/readings")` directly on `SensorResource`, we
declared a method `getReadingResource(String sensorId)` with just
`@Path` and no HTTP verb. JAX-RS recognises this as a **sub-resource
locator** and delegates routing of the nested path to the object that
method returns — in our case, a fresh `SensorReadingResource`.

The benefits of this pattern grow with project size:

* **Single responsibility.** `SensorResource` handles sensor-level
  concerns (CRUD, filtering). `SensorReadingResource` handles
  reading-level concerns (append, list, MAINTENANCE check). Neither
  class balloons into a god-object that knows about every nested
  resource.
* **Context injection via the constructor.** The sensor ID travels
  from the parent URL into the sub-resource's constructor, so the
  sub-resource has everything it needs without re-parsing the path.
  If we later add nested paths such as
  `/sensors/{id}/readings/{rid}/annotations`, we can give
  `SensorReadingResource` its own sub-resource locator.
* **Testability.** Each class can be unit-tested against a small
  dependency surface.
* **Reusability.** The same reading-resource class could be mounted
  under a different parent (say, "external feeds") by adding another
  locator — no duplicated logic.

Without this pattern, every nested path would live as another method
on one controller class, and the class would quickly exceed a thousand
lines with tangled control flow.

### Part 5.2 — Why HTTP 422 is more accurate than 404 here

The client is POSTing to a perfectly valid URL (`/api/v1/sensors`) with
a syntactically valid JSON payload. There is no missing resource in
the URL — the collection is right where it should be. The issue is
that one **field inside** the payload (`roomId`) references something
that does not exist.

* `404 Not Found` is about the request target (the URL). Using it here
  misleads clients into thinking `/api/v1/sensors` itself is gone.
* `400 Bad Request` is closer — it means "the request is malformed" —
  but 400 usually implies a syntactic problem: bad JSON, wrong type,
  schema violation.
* `422 Unprocessable Entity` (RFC 4918) is specifically defined for
  the case "the server understands the content type and syntax, but
  the request is semantically wrong". That is exactly our situation.

Using 422 is a small but real user-experience win for client
developers: combined with the JSON body our mapper emits, they get an
unambiguous signal that "the data is the problem, not the URL".

### Part 5.4 — Security risks of exposing stack traces

Without the global mapper, an unhandled `NullPointerException` would
travel all the way back to the client as an HTML error page
containing the full Java stack trace. That leaks information an
attacker can weaponise:

* **Framework and version fingerprinting.** Lines like
  `org.glassfish.jersey.server.ServerRuntime$2.run(ServerRuntime.java:256)`
  tell the attacker we run Jersey 2.x. They can then search the CVE
  databases for vulnerabilities tied to that exact version.
* **Internal package structure.** Class names such as
  `com.smartcampus.resource.SensorResource.createSensor` map out the
  internals of the service and hint at which endpoints exist, making
  targeted fuzzing easier.
* **File-system paths.** On some servers, trace elements include
  absolute source-file paths, exposing username, deployment directory
  and OS.
* **Code flow hints.** The sequence of frames reveals the call graph.
  An attacker can infer where validation sits relative to persistence,
  which is useful for crafting injection or race-condition attacks.
* **Library inventory.** Frames from bundled dependencies
  (`com.fasterxml.jackson.*`, `org.hibernate.*` …) give a shopping
  list of libraries whose known CVEs might apply.

Our `GlobalExceptionMapper` defeats all of this: stack traces are
logged **server-side** for diagnostics, but the client sees only a
generic JSON envelope with code 500 and an opaque message.

### Part 5.5 — Why JAX-RS filters beat ad-hoc `Logger.info()` calls

Imagine adding `Logger.info("Received " + method + " " + uri);` to
every method of every resource class. It sounds fine until you look
at it a week later:

* **Every new method becomes a place to forget to log.** Observability
  of the API depends on the diligence of the developer, not on the
  platform.
* **Changes cascade.** If we want to log request headers too, we have
  to touch every resource method again. That is the definition of a
  cross-cutting concern — something that applies orthogonally to
  business logic.
* **Response logging is impossible at the method level.** The response
  is partially built by JAX-RS from the `Response` object; errors are
  generated by exception mappers. A filter sits *after* the mapper, so
  it sees the final status code no matter how it was produced.
* **Test noise.** Tests that exercise resource methods directly would
  have to mock the logger to avoid spam.

A single `@Provider` filter implementing `ContainerRequestFilter` and
`ContainerResponseFilter` solves all of this declaratively: write
once, apply everywhere, modify in one place. It is the same reasoning
that led to aspects in AOP or middleware in Express / ASP.NET — the
right place to encode "happens for every request" is *outside* the
handler, not inside it.

---

## 5. Project Layout

```
smart-campus-api/
├── pom.xml
├── README.md                          (this file)
└── src/main/
    ├── java/com/smartcampus/
    │   ├── application/
    │   │   ├── SmartCampusApplication.java  (@ApplicationPath, registrations)
    │   │   └── DataStore.java               (singleton in-memory store)
    │   ├── model/
    │   │   ├── Room.java
    │   │   ├── Sensor.java
    │   │   └── SensorReading.java
    │   ├── resource/
    │   │   ├── DiscoveryResource.java       (GET /api/v1)
    │   │   ├── RoomResource.java            (/rooms)
    │   │   ├── SensorResource.java          (/sensors + sub-resource locator)
    │   │   └── SensorReadingResource.java   (sub-resource, not registered directly)
    │   ├── exception/
    │   │   ├── RoomNotEmptyException.java
    │   │   ├── RoomNotEmptyExceptionMapper.java       (→ 409)
    │   │   ├── LinkedResourceNotFoundException.java
    │   │   ├── LinkedResourceNotFoundExceptionMapper.java (→ 422)
    │   │   ├── SensorUnavailableException.java
    │   │   ├── SensorUnavailableExceptionMapper.java   (→ 403)
    │   │   └── GlobalExceptionMapper.java              (→ 500 catch-all)
    │   └── filter/
    │       └── LoggingFilter.java           (request + response logging)
    └── webapp/WEB-INF/
        └── web.xml                          (Jersey servlet declaration)
```

---

## 6. Video demo — suggested walkthrough

A 10-minute demo path that hits every rubric cell:

1. **Discovery** — `GET /api/v1`
2. **List / create / fetch room** — show 201 and the **Location** header
3. **Delete empty room** (`CR-205`) vs **delete occupied room** (`LIB-301` → **409**)
4. **Register sensor** (valid roomId → 201) vs **register with bad roomId** (→ **422**)
5. **Filter sensors** with `?type=Temperature`, then `?type=CO2`
6. **Sub-resource navigation** — `GET /sensors/TEMP-001/readings`, then POST a reading, then GET again to show the new entry *and* that `TEMP-001`'s `currentValue` updated
7. **403 demo** — POST a reading to `OCC-001` (MAINTENANCE)
8. **500 demo** — (optional) deliberately trigger a 500 by an unexpected runtime error, show there is no stack trace in the response
9. **Logs** — show the Tomcat console printing `[SMART-CAMPUS][REQUEST]` / `[RESPONSE]` lines for every call

---

**Author:** *Verix Co.* · Student at the School of Computer Science and Engineering, University of Westminster
**Module leader:** Dr. Hamed Hamzeh
