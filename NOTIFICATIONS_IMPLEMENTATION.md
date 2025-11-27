# Benachrichtigungsfunktion für Tracker-Schwellenwerte

## Überblick
Diese Implementierung fügt automatische Benachrichtigungen hinzu, die ausgelöst werden, wenn Tracker-Daten die konfigurierten Warn- oder Fehler-Schwellenwerte überschreiten.

## Implementierte Komponenten

### 1. NotificationService (Neu)
**Datei:** `data/src/main/java/com/samco/trackandgraph/data/notifications/NotificationService.kt`

```kotlin
interface NotificationService {
    suspend fun checkAndNotifyThreshold(dataPoint: DataPoint, tracker: Tracker)
}
```

**Funktionalität:**
- Überprüft automatisch, wenn ein neuer Datenpunkt eingefügt wird
- Vergleicht den Wert mit `warningThreshold` und `errorThreshold`
- Sendet entsprechende Android Notification mit Tracker-Name und Uhrzeit

**Notification-Typen:**
- **ERROR**: Wird angezeigt wenn `dataPoint.value >= tracker.errorThreshold`
  - Icon: Alert-Dialog Icon
  - Priorität: LOW
  - Zeigt: Tracker-Name und Uhrzeit (HH:mm:ss)
  
- **WARNING**: Wird angezeigt wenn `dataPoint.value >= tracker.warningThreshold`
  - Icon: Info-Dialog Icon  
  - Priorität: LOW
  - Zeigt: Tracker-Name und Uhrzeit (HH:mm:ss)

### 2. DataInteractorImpl - Angepasst
**Datei:** `data/src/main/java/com/samco/trackandgraph/data/interactor/DataInteractorImpl.kt`

**Änderungen:**
- `NotificationService` als Dependency injiziert
- `insertDataPoint()` Methode erweitert:
  ```kotlin
  val tracker = dao.getTrackerByFeatureId(dataPoint.featureId)
  if (tracker != null) {
      val trackerDto = Tracker.fromTrackerWithFeature(tracker)
      notificationService.checkAndNotifyThreshold(dataPoint, trackerDto)
  }
  ```
- `insertDataPoints()` Methode erweitert: Prüft alle eingefügten Datenpunkte

### 3. DataModule - DI-Konfiguration
**Datei:** `data/src/main/java/com/samco/trackandgraph/data/di/DataModule.kt`

**Hinzugefügt:**
```kotlin
@Provides
internal fun getNotificationService(impl: NotificationServiceImpl): NotificationService = impl
```

### 4. TrackAndGraphApplication - Notification Channel
**Datei:** `app/src/main/java/com/samco/trackandgraph/TrackAndGraphApplication.kt`

**Änderungen:**
- Erstellt Notification Channel beim App-Start
- Android O+ kompatibel
- Channel ID: `"tracker_thresholds"`
- Channel Name: `"Tracker Schwellenwert-Benachrichtigungen"`

### 5. AndroidManifest.xml
**Status:** ✅ Bereits vorhanden
- Permission `android.permission.POST_NOTIFICATIONS` ist bereits konfiguriert

## Wie es funktioniert

### Workflow:
1. Benutzer fügt einen neuen Datenpunkt für einen Tracker hinzu
2. Der Datenpunkt wird in die Datenbank eingefügt
3. `notificationService.checkAndNotifyThreshold()` wird aufgerufen
4. Service prüft, ob der Wert die Schwellenwerte überschreitet:
   - Wenn `value >= errorThreshold` → Fehler-Benachrichtigung
   - Sonst wenn `value >= warningThreshold` → Warn-Benachrichtigung
   - Sonst → Keine Benachrichtigung
5. Android zeigt die entsprechende Notification an

### Notification-IDs:
- **Fehler:** `10000 + trackerId`
- **Warnung:** `20000 + trackerId`

Dies stellt sicher, dass jeder Tracker und jeder Schwellentyp eindeutige Benachrichtigungen hat.

## Verwendung

### Tracker mit Schwellenwerten erstellen:
Wenn Sie einen Tracker erstellen oder aktualisieren, können Sie die Schwellenwerte setzen:
- `warningThreshold`: Wert, ab dem eine Warnung angezeigt wird
- `errorThreshold`: Wert, ab dem eine Fehler-Benachrichtigung angezeigt wird

Standardwert: `-1.0` (Schwellenwert deaktiviert)

### Beispiele:
```kotlin
// Tracker mit Schwellenwerten
val tracker = Tracker(
    id = 1,
    name = "Körpertemperatur",
    warningThreshold = 37.5,  // Warnung ab 37.5°C
    errorThreshold = 38.5     // Fehler ab 38.5°C
)

// Datenpunkt mit Wert 38.8°C würde eine Fehler-Benachrichtigung auslösen
val dataPoint = DataPoint(
    featureId = tracker.featureId,
    value = 38.8,
    timestamp = OffsetDateTime.now(),
    label = ""
)
```

## Sicherheit & Berechtigungen

✅ **Runtime Permission Check:**
- Für Android 13+ wird die `POST_NOTIFICATIONS` Permission zur Laufzeit überprüft
- Benachrichtigungen werden nur gesendet, wenn die Permission erteilt wurde

✅ **Error Handling:**
- Alle Exceptions werden abgefangen und geloggt
- Ein Exception in der Notification-Logik verhindert nicht das Einfügen des Datenpunktes

## Wichtige Anmerkungen

### Threshold-Vergleich:
- Der Vergleich erfolgt mit `>=` (größer oder gleich)
- Wenn **beide** Schwellenwerte überschritten sind (Error > Warning), wird nur die Fehler-Benachrichtigung gesendet

### Datenbankunterstützung:
Die Felder sind bereits in der Datenbank-Schema vorhanden:
- `warningThreshold` (Standard: -1.0)
- `errorThreshold` (Standard: -1.0)

### Kompatibilität:
- ✅ Android 4.0+ (Notification API)
- ✅ Android 8.0+ (Notification Channels)
- ✅ Android 13+ (Runtime Permissions)

## Testing

Um zu testen:
1. Erstellen Sie einen Tracker
2. Öffnen Sie die Tracker-Einstellungen und setzen Sie `warningThreshold` = 5.0 und `errorThreshold` = 10.0
3. Fügen Sie einen Datenpunkt mit Wert 7.0 hinzu → Warnung wird angezeigt
4. Fügen Sie einen Datenpunkt mit Wert 12.0 hinzu → Fehler wird angezeigt
5. Überprüfen Sie die Benachrichtigungen im Android Notification Center

## Verbesserungsmöglichkeiten (optional, für die Zukunft)

- [ ] Sound/Vibration für Error-Benachrichtigungen
- [ ] Benutzer-Setting zum Stummschalten von Benachrichtigungen
- [ ] Benachrichtigungshistorie anzeigen
- [ ] Customizable Threshold-Namen (z.B. "Kritisch" statt "Fehler")
- [ ] Wiederholte Benachrichtigungen für kontinuierliche Überschreitungen

