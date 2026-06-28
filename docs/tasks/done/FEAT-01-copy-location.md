# FEAT-01: Przycisk "Kopiuj lokalizację" w szczegółach atrakcji

## Cel

Dodać przycisk w ekranie szczegółów atrakcji, który kopiuje geolokalizację punktu do schowka w formacie `"50.123456,19.654321"` (lat,lon). Po skopiowaniu wyświetlić krótkie potwierdzenie (Snackbar).

## Implementacja

### `AttractionDetailScreen.kt`

Poniżej przycisku "Nawiguj do" dodaj:

```kotlin
val clipboardManager = LocalClipboardManager.current

OutlinedButton(
    onClick = {
        clipboardManager.setText(
            AnnotatedString("${attraction.latitude},${attraction.longitude}")
        )
        // Potwierdzenie przez Snackbar (SnackbarHostState już istnieje w tym ekranie)
        scope.launch {
            snackbarHostState.showSnackbar("Skopiowano: ${attraction.latitude},${attraction.longitude}")
        }
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Kopiuj lokalizację")
}
```

Potrzebne importy:
```kotlin
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
```

`rememberCoroutineScope()` dodaj na poziomie composable (obok istniejącego `remember { SnackbarHostState() }`).

## Pliki do zmiany
- `feature-detail/src/.../ui/AttractionDetailScreen.kt`

## Weryfikacja
- Kliknięcie "Kopiuj lokalizację" → Snackbar z potwierdzeniem
- Wklejenie w dowolne pole tekstowe → widoczny format `"50.123456,19.654321"`
