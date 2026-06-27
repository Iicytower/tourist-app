# TASK-06: Moduł `data` — klient OpenTripMap

## Cel
Implementacja klienta Ktor dla OpenTripMap API: wyszukiwanie atrakcji turystycznych po współrzędnych, promieniu i kategoriach.

## Zależności
- TASK-03 (modele domenowe `SearchParams`, `Attraction`)
- TASK-02 (mapowanie kategorii → kinds)

## Zakres

### 1. DTO — odpowiedź API (`data/remote/opentripmap/dto/`)

OpenTripMap zwraca listę obiektów przez endpoint `/radius`:

**`OtmAttractionDto.kt`**
```kotlin
@Serializable
data class OtmAttractionDto(
    val xid: String,
    val name: String,
    val dist: Double,              // odległość w metrach
    val rate: Int,
    val kinds: String,             // przecinkami, np. "castles,fortifications"
    val point: OtmPoint
)

@Serializable
data class OtmPoint(
    val lon: Double,
    val lat: Double
)
```

Endpoint szczegółów (pojedynczy obiekt, `/xid/{xid}`):
**`OtmAttractionDetailDto.kt`**
```kotlin
@Serializable
data class OtmAttractionDetailDto(
    val xid: String,
    val name: String,
    val kinds: String,
    val point: OtmPoint,
    val wikipedia: String? = null,   // link do Wikipedii jeśli istnieje
    val wikidata: String? = null,
    val preview: OtmPreview? = null,
    val info: OtmInfo? = null
)

@Serializable
data class OtmInfo(val descr: String? = null)

@Serializable
data class OtmPreview(val source: String? = null)
```

### 2. Mapper (`data/remote/opentripmap/mapper/OtmMapper.kt`)

- `OtmAttractionDto.toDomain(userLat: Double, userLon: Double): Attraction`
  - `category` — mapowanie odwrotne: `kinds` z API → najbardziej pasujący `AttractionCategory`. Podejście: sprawdzaj każde kind z odpowiedzi przeciwko mapowaniu z `CategoryKindsMapping`, wybierz kategorię z największą liczbą trafień
  - `distanceKm` = `dist / 1000.0` (API zwraca metry)
  - `isInMyList = false`, `isFromLastSearch = true` (nowe wyniki wyszukiwania)
  - `description = null`, `descriptionSources = emptyList()`

### 3. Klient HTTP (`data/remote/opentripmap/OpenTripMapClient.kt`)

```kotlin
class OpenTripMapClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://api.opentripmap.com/0.1/en/places"
    // Klucz API z ustawień — wstrzykiwany przez konstruktor lub pobierany z SettingsRepository

    suspend fun searchAttractions(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        kinds: String
    ): Result<List<OtmAttractionDto>>

    suspend fun getAttractionDetail(xid: String): Result<OtmAttractionDetailDto>
}
```

Endpoint wyszukiwania: `GET /radius?radius={m}&lon={lon}&lat={lat}&kinds={kinds}&format=json&limit=100&apikey={key}`
Endpoint szczegółów: `GET /xid/{xid}?apikey={key}`

Obsługa błędów HTTP (4xx, 5xx) → `Result.failure` z komunikatem.
Timeout: 30 sekund.

### 4. Konfiguracja Ktor HttpClient (`data/remote/HttpClientProvider.kt`)

Jeden współdzielony `HttpClient` dla całej aplikacji (singleton w Koin):

```kotlin
fun createHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) = Timber.tag("Ktor").d(message)
        }
        level = LogLevel.BODY   // tylko na DEBUG
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
    }
}
```

### 5. Koin module (`data/remote/opentripmap/OpenTripMapModule.kt`)

```kotlin
val openTripMapModule = module {
    single { OpenTripMapClient(get()) }
}
```

### 6. Integracja z `RoomAttractionRepository` (rozszerzenie TASK-04)

`RoomAttractionRepository.searchAttractions(params: SearchParams)`:
1. Mapuj `params.categories` → `kinds` przez `CategoryKindsMapping.toKindsParam()`
2. Wywołaj `OpenTripMapClient.searchAttractions(lat, lon, radiusMeters, kinds)`
3. Mapuj DTOs → `Attraction` (domain models)
4. Wywołaj sekwencję zapisu nowego wyszukiwania (clearFlag → deleteOrphans → upsertAll)
5. Zwróć listę jako `Result<List<Attraction>>`

## Testy

**`data/test/`**:

- `OtmMapperTest`:
  - Mapowanie `kinds` → `AttractionCategory` (różne kombinacje)
  - Konwersja metrów → km
  - Poprawne mapowanie pól DTO → domain model

- `OpenTripMapClientTest` (MockK + MockEngine Ktor):
  - Udane zapytanie → `Result.success` z listą DTO
  - 401 (błędny klucz) → `Result.failure`
  - Timeout → `Result.failure`
  - Odpowiedź 200 z pustą listą → `Result.success(emptyList())`

## Weryfikacja ukończenia
- `./gradlew :data:test` przechodzi
- Zapytanie do prawdziwego OpenTripMap API (ręczne) zwraca wyniki
- Mapowanie kategorii pokrywa wszystkie 10 kategorii biznesowych
