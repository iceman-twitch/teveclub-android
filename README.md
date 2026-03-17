# TeveClub Helper — Projektváz

Ez egy egyszerű Android projektváz Kotlin + Jetpack Compose + Room + WorkManager + EncryptedSharedPreferences példákkal.

Hogyan buildeld Windows 10-en (Android Studio használata ajánlott):

1. Nyisd meg a projektet Android Studio-ban (`File -> Open` -> mappa: a projekt gyökere).
2. Telepítsd a hiányzó SDK komponenseket, ha kéri az Android Studio.
3. Debug APK build:

```powershell
cd d:\Github\teveclub-android
.\gradlew.bat assembleDebug
```

4. Telepítés csatlakoztatott eszközre:

```powershell
.\gradlew.bat installDebug
```

Megjegyzés: a Gradle wrapper és az Android Studio általában kezeli a Gradle verziót. Ha problémák merülnek fel, használd az Android Studio beépített "Sync Project" és "Build" funkcióit.
