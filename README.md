# Carbon Intensity API

A Spring Boot REST API that fetches real-time energy generation data from the [UK Carbon Intensity API](https://carbonintensity.org.uk/) and provides endpoints for:

- Daily energy mix breakdown (today, tomorrow, day after tomorrow) with clean energy percentages
- Optimal EV charging window detection based on highest clean energy availability

## Tech Stack

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.15 |
| Maven | 3.9+ |
| JUnit 5 + Mockito + AssertJ | latest |
| Docker | 24+ |

## Quick Start

```bash
# Build & run
./mvnw spring-boot:run

# Or with Docker
docker build -t carbon-intensity-api .
docker run -p 8080:8080 carbon-intensity-api
```

The API will be available at `http://localhost:8080`.

## Endpoints

### GET /energy-mix

Returns a three-day energy generation forecast with average fuel mix percentages and clean energy share for each day.

```bash
curl http://localhost:8080/energy-mix
```

Response:
```json
{
  "today": {
    "date": "2026-06-17",
    "fuelMixAverages": {
      "biomass": 6.2,
      "coal": 1.8,
      "gas": 28.5,
      "nuclear": 15.3,
      "solar": 12.1,
      "wind": 18.7
    },
    "cleanEnergyPercent": 52.3
  },
  "tomorrow": { "..." },
  "dayAfterTomorrow": { "..." }
}
```

**Clean energy sources:** biomass, nuclear, hydro, wind, solar.

### GET /charging-window?hours={1-6}

Finds the optimal contiguous time window for EV charging over the next 2 days, maximizing the average share of clean energy.

```bash
curl "http://localhost:8080/charging-window?hours=3"
```

| Parameter | Type | Required | Range | Description |
|---|---|---|---|---|
| `hours` | int | yes | 1–6 | Desired charging window length in full hours |

Response:
```json
{
  "start": "2026-06-17T14:00:00",
  "end": "2026-06-17T17:00:00",
  "intervalCount": 6,
  "cleanEnergyPercent": 72.5
}
```

The window may span across day boundaries (e.g., starting at 23:30 and ending at 02:30).

### GET /health

Simple health check endpoint for monitoring and deployments.

```bash
curl http://localhost:8080/health
```

## Architecture

```
┌─────────────────────────────────────────────┐
│                Controllers                    │
│  EnergyMixController  ChargingWindowCtrl     │
└──────────┬───────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────┐
│                  Services                     │
│  EnergyMixService     ChargingWindowService   │
└──────────┬───────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────┐
│                  Provider                     │
│    CarbonIntensityIntervalProvider            │
└──────────┬───────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────┐
│              Client + Mapper                  │
│  CarbonIntensityClient   EnergyIntervalMapper │
└──────────┬───────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────┐
│         UK Carbon Intensity API               │
│    https://api.carbonintensity.org.uk         │
└──────────────────────────────────────────────┘
```

### Package Structure

```
com.niedzszym.carbon_intensity_api
├── client/           # External API HTTP client + DTOs
├── config/           # RestTemplate, CORS configuration
├── controller/       # REST controllers
├── dto/              # Response DTOs for API consumers
├── exception/        # Custom exceptions + global handler
├── mapper/           # External DTO → Domain model mapping
├── model/            # Domain models + enums
├── provider/         # Shared data provider (fetch + map intervals)
└── service/          # Business logic
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **Extracted `CarbonIntensityIntervalProvider`** | Avoids code duplication between services — both `EnergyMixService` and `ChargingWindowService` reuse the same data fetching logic. |
| **Fixed-size sliding window O(n)** | For a small range of window sizes (1–6h), iterating over each size with a fixed-size sliding window is simpler and just as performant as binary search on average. |
| **Java records** | All DTOs and domain models use records for immutability, conciseness, and automatic equals/hashCode. |
| **Constructor injection** | All Spring beans use explicit constructor injection — no field `@Autowired`, no Lombok. |
| **RestTemplate timeouts** | Connection timeout 5s, read timeout 10s — prevents indefinite thread blocking on external API calls. |
| **No Actuator** | Health check is a simple custom endpoint — avoids dependency bloat for a focused microservice. |

## Testing

### Run All Tests

```bash
./mvnw test
```

### Test Structure

| Layer | Test Class | Tests | Type |
|---|---|---|---|
| Client | `CarbonIntensityClientTest` | 3 | Unit (Mockito) |
| Mapper | `EnergyIntervalMapperTest` | 5 | Unit |
| Provider | `CarbonIntensityIntervalProviderTest` | 4 | Unit (Mockito) |
| Service | `EnergyMixServiceTest` | 4 | Unit (Mockito) |
| Service | `ChargingWindowServiceTest` | 6 | Unit (Mockito) |
| Controller | `EnergyMixControllerTest` | 5 | `@WebMvcTest` |
| Controller | `ChargingWindowControllerTest` | 7 | `@WebMvcTest` |
| Context | `CarbonIntensityApiApplicationTests` | 1 | `@SpringBootTest` |

**Total: 35 tests**

### Test Coverage

- Happy paths for both endpoints
- Edge cases: null/empty API responses, insufficient intervals, date sorting, sliding window boundaries
- Error scenarios: 503 on external API failure, 400 on invalid parameters
- JSON structure and content type validation

## Error Responses

All errors follow a consistent JSON structure:

```json
{
  "error": "External API unavailable",
  "message": "Carbon Intensity API returned no data for range: 2026-06-17T00:00Z to 2026-06-19T00:00Z"
}
```

| Scenario | HTTP Status |
|---|---|
| External API failure or no data | 503 Service Unavailable |
| Invalid parameter values | 400 Bad Request |
| Data parsing error | 500 Internal Server Error |
