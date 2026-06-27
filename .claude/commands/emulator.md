Uruchamia emulator Android. Najpierw wylistuj dostępne AVD, potem uruchom wybrany.

Lista dostępnych AVD:
```bash
ANDROID_AVD_HOME=~/.config/.android/avd emulator -list-avds
```

Uruchomienie (zastąp NAME nazwą AVD):
```bash
ANDROID_AVD_HOME=~/.config/.android/avd emulator -avd NAME -no-audio -no-boot-anim
```
