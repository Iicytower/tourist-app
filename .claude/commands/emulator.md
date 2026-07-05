Uruchamia emulator Android i logi, żeby móc debugować w tej sesji. Najpierw wylistuj dostępne AVD, potem uruchom wybrany.

**Uwaga:** `ANDROID_AVD_HOME` musi wskazywać na `~/.config/.android/avd` — domyślny `~/.android/avd` jest pusty w tym środowisku, przez co `emulator -list-avds` bez tej zmiennej nie widzi żadnych AVD mimo że `avdmanager list avd` je pokazuje.

**Uwaga 2:** domyślny renderer GPU (`host`) crashuje w tym środowisku (`MESA: error dri3_alloc_render_buffer` / X error 11, proces qemu ginie po kilku sekundach bez żadnego okna). Zawsze uruchamiaj z `-gpu swiftshader_indirect` (software rendering).

1. Lista dostępnych AVD:
```bash
ANDROID_AVD_HOME=~/.config/.android/avd emulator -list-avds
```

2. Uruchomienie w tle (zastąp NAME nazwą AVD), z logiem do pliku w scratchpadzie:
```bash
export ANDROID_AVD_HOME=~/.config/.android/avd
nohup emulator -avd NAME -no-audio -no-boot-anim -gpu swiftshader_indirect > <scratchpad>/emulator.log 2>&1 &
disown
```

3. Poczekaj aż urządzenie się zarejestruje i dobootuje:
```bash
timeout 90 bash -c 'until adb devices | grep -q "device$"; do sleep 3; done'
timeout 90 bash -c 'until adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; do sleep 3; done'
```

4. Zainstaluj i uruchom aplikację:
```bash
./gradlew installDebug
adb shell am start -n com.iicytower.wanderlist/.MainActivity
```

5. Podłącz się do logów (przefiltrowane do relevantnych tagów + błędy), w tle do pliku żeby móc go czytać w trakcie ręcznego testowania:
```bash
adb logcat -c
nohup adb logcat "SearchVM:*" "CompositeSource:*" "Ktor:*" "AndroidRuntime:E" "*:S" > <scratchpad>/logcat.log 2>&1 &
disown
```
Dostosuj listę tagów do modułu, który akurat jest debugowany (np. dodaj `GenerateTripPlan:*`, `LlmAttractionQualityFilter:*` itd. — tagi Timbera używane w kodzie).
