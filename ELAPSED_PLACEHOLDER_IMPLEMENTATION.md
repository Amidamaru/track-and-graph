## Implementierung des {{elapsed}} Platzhalters - FERTIG ✅

Der `{{elapsed}}` Platzhalter wurde erfolgreich in das Benachrichtigungssystem implementiert!

### Was wurde hinzugefügt:

#### 1. **Neue Funktion: `calculateElapsedTime()`**
- Berechnet die Zeit zwischen dem aktuellen und dem vorherigen Datenpunkt
- Formatiert das Ergebnis als benutzerfreundlichen String (z.B. "2h 15m", "45m", "< 1m")

#### 2. **Erweiterte Template-Ersetzung: `replaceTemplatePlaceholders()`**
- Ersetzt alle verfügbaren Platzhalter:
  - `{{name}}` - Tracker-Name
  - `{{value}}` - Datenpunkt-Wert
  - `{{time}}` - Zeitstempel (HH:mm:ss)
  - `{{elapsed}}` - **NEU!** Zeit seit letztem Datenpunkt
  - `{{label}}` - Datenpunkt-Label
  - `{{note}}` - Datenpunkt-Notiz
  - `{{errorThreshold}}` - Fehler-Schwellenwert
  - `{{warningThreshold}}` - Warn-Schwellenwert

#### 3. **DataInteractor erweitert**
- `getDataPointsForFeatureSync()` Methode hinzugefügt
- Ermöglicht Zugriff auf alle Datenpunkte eines Trackers (sortiert nach Datum, neueste zuerst)

### Verwendungsbeispiele:

#### Standard-Templates (mit {{elapsed}}):
```
Fehler: "Tracker: {{name}}\nZeit: {{time}}\nWert: {{value}}\nVerstrichene Zeit: {{elapsed}}\nFehler-Schwelle: {{errorThreshold}}"

Warnung: "Tracker: {{name}}\nZeit: {{time}}\nWert: {{value}}\nVerstrichene Zeit: {{elapsed}}\nWarn-Schwelle: {{warningThreshold}}"
```

#### Benutzerdefinierte Templates:
```
Titel: "🔥 {{name}} kritisch! ({{elapsed}} seit letztem Wert)"
Körper: "Achtung! {{name}} hat {{value}} erreicht nach {{elapsed}} Zeit. Schwelle: {{errorThreshold}}"
```

### Beispiel-Ausgabe:
Wenn ein Benutzer alle 30 Minuten seinen Blutdruck misst und der Wert über die Schwelle steigt:

**Benachrichtigung:**
```
Titel: "⚠️ Blutdruck kritisch! (30m seit letztem Wert)"
Text: "Tracker: Blutdruck
Zeit: 14:32:15
Wert: 160
Verstrichene Zeit: 30m
Fehler-Schwelle: 150"
```

### Technische Details:
- **Zeitmessung:** Berechnet die Differenz zwischen `current.epochMilli` und `previous.epochMilli`
- **Formatierung:** Stunden und Minuten (z.B. "2h 15m") oder nur Minuten (z.B. "45m")
- **Fallback:** "< 1m" für sehr kurze Zeiten, "N/A" bei Fehlern
- **Performance:** Optimiert durch einmaligen Datenbankzugriff pro Benachrichtigung

### Dependency Injection Problem - GELÖST ✅

**Problem:** Zirkuläre Abhängigkeit zwischen `DataInteractor` und `NotificationService`
- `DataInteractorImpl` benötigt `NotificationService` 
- `NotificationServiceImpl` benötigt `DataInteractor` für `{{elapsed}}` Berechnung

**Lösung:** Direkter DAO-Zugriff statt DataInteractor
- `NotificationServiceImpl` verwendet jetzt direkt `TrackAndGraphDatabaseDao`
- `dao.getDataPointsForFeatureSync()` wird direkt aufgerufen
- Zirkuläre Abhängigkeit vermieden ✅

### Status: ✅ VOLLSTÄNDIG IMPLEMENTIERT UND EINSATZBEREIT

Der `{{elapsed}}` Platzhalter funktioniert jetzt vollständig in allen Benachrichtigungs-Templates ohne Dependency-Probleme!
