# TASK-04: Moduł `data` — baza danych Room

## Cel
Implementacja lokalnej bazy danych Room: encja, DAO, baza, migracja (destructive na MVP) i implementacja `AttractionRepository`.

## Zależności
- TASK-03 (interfejsy domenowe)
- TASK-02 (modele core)

## Zakres

### 1. Encja Room (`data/local/entity/AttractionEntity.kt`)

```kotlin
@Entity(tableName = "attractions")
data class AttractionEntity(
    @PrimaryKey val xid: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,           // nazwa enum AttractionCategory
    val isInMyList: Boolean,
    val dateAddedToList: Long?,
    val description: String?,
    val descriptionSources: String?,  // JSON: [{name, url}]
    val isFromLastSearch: Boolean
)
```

### 2. DAO (`data/local/dao/AttractionDao.kt`)

```kotlin
@Dao
interface AttractionDao {
    @Query("SELECT * FROM attractions WHERE isInMyList = 1 ORDER BY dateAddedToList DESC")
    fun getMyList(): Flow<List<AttractionEntity>>

    @Query("SELECT * FROM attractions WHERE xid = :xid")
    suspend fun getByXid(xid: String): AttractionEntity?

    @Query("SELECT * FROM attractions WHERE isFromLastSearch = 1")
    suspend fun getLastSearchResults(): List<AttractionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attraction: AttractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(attractions: List<AttractionEntity>)

    @Query("UPDATE attractions SET isFromLastSearch = 0 WHERE isFromLastSearch = 1")
    suspend fun clearLastSearchFlag()

    @Query("DELETE FROM attractions WHERE isInMyList = 0 AND description IS NULL AND isFromLastSearch = 0")
    suspend fun deleteOrphans()

    @Query("UPDATE attractions SET isInMyList = 1, dateAddedToList = :timestamp WHERE xid = :xid")
    suspend fun addToMyList(xid: String, timestamp: Long)

    @Query("UPDATE attractions SET isInMyList = 0, dateAddedToList = NULL WHERE xid = :xid")
    suspend fun removeFromMyList(xid: String)

    @Query("SELECT COUNT(*) FROM attractions WHERE isInMyList = 1")
    suspend fun getMyListCount(): Int

    @Query("UPDATE attractions SET description = :description, descriptionSources = :sources WHERE xid = :xid")
    suspend fun saveDescription(xid: String, description: String, sources: String)
}
```

### 3. Baza danych (`data/local/AppDatabase.kt`)

```kotlin
@Database(entities = [AttractionEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attractionDao(): AttractionDao
}
```

- `exportSchema = true` — schemat eksportowany do `schemas/` (dla przyszłych migracji przed produkcją)
- Konfiguracja Room: `fallbackToDestructiveMigration()` na MVP

### 4. Mapper (`data/local/mapper/AttractionMapper.kt`)

Funkcje rozszerzające:
- `AttractionEntity.toDomain(distanceKm: Double? = null): Attraction`
- `Attraction.toEntity(): AttractionEntity`

Serializacja `descriptionSources` do/z JSON przy użyciu `kotlinx.serialization`.

### 5. Implementacja repozytorium (`data/repository/RoomAttractionRepository.kt`)

Implementuje `AttractionRepository` z modułu `domain`.

**Logika nowego wyszukiwania** (wywoływana przez `saveSearchResults`):
1. `dao.clearLastSearchFlag()` — ustaw `isFromLastSearch = false` na starych
2. `dao.deleteOrphans()` — usuń rekordy bez żadnej przyczyny istnienia
3. `dao.upsertAll(newResults)` — wstaw nowe z `isFromLastSearch = true`

Wszystkie trzy kroki w jednej transakcji Room (`@Transaction`).

**`addToMyList`**: sprawdza count przez `dao.getMyListCount()` i jeśli ≥ 50 zwraca `Result.failure`. Nie duplikuje logiki — `AddToMyListUseCase` robi tę samą kontrolę, ale repozytorium jest defensywne.

**Obsługa błędów**: wszystkie metody suspend owijane w `runCatching { }.toResult()` lub analogicznie — błędy DB propagowane jako `Result.failure`.

### 6. Koin module (`data/local/DatabaseModule.kt`)

```kotlin
val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "wanderlist.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().attractionDao() }
}

val attractionRepositoryModule = module {
    single<AttractionRepository> { RoomAttractionRepository(get()) }
}
```

## Testy

**`data/test/`** — testy integracyjne z Room in-memory:

```kotlin
// Tworzone z Room.inMemoryDatabaseBuilder(...)
```

- `AttractionDaoTest`:
  - upsert i odczyt przez `getByXid`
  - `getMyList()` zwraca tylko elementy z `isInMyList = true`, posortowane
  - `clearLastSearchFlag()` → `isFromLastSearch` staje się `false` u wszystkich
  - `deleteOrphans()` usuwa tylko rekordy bez isInMyList, description i isFromLastSearch
  - `getMyListCount()` zwraca poprawną liczbę
  - Całkowita sekwencja nowego wyszukiwania: clearFlag → deleteOrphans → upsertAll

- `RoomAttractionRepositoryTest`:
  - `addToMyList` gdy lista ma 50 elementów → `Result.failure`
  - `saveDescription` zapisuje tekst i źródła, mapper poprawnie deserializuje JSON

## Weryfikacja ukończenia
- `./gradlew :data:test` przechodzi
- Schemat Room eksportowany do `data/schemas/`
- `RoomAttractionRepository` implementuje cały interfejs `AttractionRepository`
