# TASK-10: Moduł `data` — lokalizacja GPS

## Cel
Implementacja `LocationService` przez Android Location API (bez Google Play Services).

## Zależności
- TASK-03 (interfejs `LocationService`, model `Location` w `domain`)

## Zakres

### 1. Implementacja serwisu (`data/local/location/AndroidLocationService.kt`)

Implementuje `LocationService` z modułu `domain`.

```kotlin
class AndroidLocationService(
    private val context: Context
) : LocationService {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getCurrentLocation(): Result<Location> {
        // 1. Sprawdź uprawnienie — brak → Result.failure
        // 2. Użyj LocationManager.getLastKnownLocation() jeśli świeże (< 60s)
        // 3. Jeśli nieświeże — zarejestruj LocationListener przez requestLocationUpdates()
        //    z timeoutem 15 sekund (suspendCoroutine lub callbackFlow)
        // 4. Timeout → Result.failure("Nie udało się pobrać lokalizacji")
        // 5. Usuń listener po odbiorze lub timeoucie
    }
}
```

Provider kolejność prób:
1. `LocationManager.GPS_PROVIDER`
2. `LocationManager.NETWORK_PROVIDER` (backup gdy GPS niedostępny)

### 2. Koin module

```kotlin
val locationModule = module {
    single<LocationService> { AndroidLocationService(androidContext()) }
}
```

### 3. Uprawnienia w `app/AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

Żądanie uprawnień podczas pierwszego uruchomienia aplikacji (nie lazy) — obsługa w `MainActivity` lub dedykowanym ekranie onboarding. Szczegóły UI w TASK-01/TASK-16.

## Testy

`LocationService` jest trudny do testowania jednostkowego (zależy od Android Loopera i prawdziwego GPS). Brak automatycznych testów dla tego komponentu na MVP. Weryfikacja ręczna na emulatorze.

## Weryfikacja ukończenia
- `AndroidLocationService` implementuje `LocationService`
- Na emulatorze z symulowaną lokalizacją: `getCurrentLocation()` zwraca `Result.success` z koordynatami
- Brak uprawnienia: `getCurrentLocation()` zwraca `Result.failure`
- Brak zależności od Google Play Services (tylko `android.location.*`)
