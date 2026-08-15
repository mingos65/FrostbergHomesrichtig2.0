# FrostbergHomes

Eigenes Homes-Plugin für Paper 1.21.x (Java 21) - kein Essentials nötig.
Nummerierte Homes, farbenfrohe Chat-Nachrichten, Warmup-Countdown im Chat,
Cooldown, Safe-Teleport und optionale Sound-/Partikel-/Title-Effekte.
Kompatibel mit LuckPerms, Vault, PlaceholderAPI, PlotSquared, Multiverse und
TAB (nur `softdepend`, keine Pflicht-Abhängigkeit).

## Voraussetzungen

- Java 21 (JDK)
- Maven 3.9 oder neuer
- Ein Paper-1.21.x-Server zum Testen

## Bauen (kompilieren)

Im Projektordner (dort, wo `pom.xml` liegt):

```bash
mvn clean package
```

Die fertige Datei liegt danach unter:

```
target/FrostbergHomes-1.0.0.jar
```

Diese `.jar` in den `plugins`-Ordner deines Paper-Servers kopieren und den
Server (neu)starten.

## Erstinstallation

Beim ersten Start legt das Plugin automatisch an:

- `plugins/FrostbergHomes/config.yml` - Einstellungen, Effekte, Nachrichten
- `plugins/FrostbergHomes/playerdata/<UUID>.yml` - pro Spieler eine Datei,
  sobald er sein erstes Home setzt

## Befehle

| Befehl                 | Beschreibung                          | Permission     |
|-------------------------|----------------------------------------|----------------|
| `/set home`             | Setzt Home #1 an der aktuellen Position | `homes.set`    |
| `/set home <nr>`        | Setzt Home #nr (überschreibt falls vorhanden) | `homes.set` |
| `/home`                 | Teleportiert zu Home #1                | `homes.use`    |
| `/home <nr>`             | Teleportiert zu Home #nr               | `homes.use`    |
| `/delete home`          | Löscht Home #1                         | `homes.delete` |
| `/delete home <nr>`     | Löscht Home #nr                        | `homes.delete` |
| `/homes`                | Zeigt alle eigenen Homes an            | `homes.list`   |
| `/homes reload`         | Lädt die config.yml neu                | `homes.reload` |

Alle Befehle unterstützen Tab-Complete (z.B. Vorschläge für vorhandene
Home-Nummern bei `/home` und `/delete home`).

## Permissions - Übersicht

| Permission                | Standard | Bedeutung                                      |
|----------------------------|----------|-------------------------------------------------|
| `homes.set`                | true     | `/set home` nutzen                              |
| `homes.use`                 | true     | `/home` nutzen                                  |
| `homes.delete`              | true     | `/delete home` nutzen                           |
| `homes.list`                | true     | `/homes` nutzen                                 |
| `homes.reload`              | op       | `/homes reload` nutzen                          |
| `homes.bypass.cooldown`     | op       | Ignoriert den Teleport-Cooldown                 |
| `homes.bypass.warmup`       | op       | Ignoriert den Warmup-Countdown (sofortiger TP)  |
| `homes.limit.1` … `homes.limit.10` | false | Erlaubt die jeweilige Anzahl an Homes    |
| `homes.limit.unlimited`     | false    | Unbegrenzt viele Homes                          |
| `homes.*`                   | false    | Sammel-Permission für alles Obige               |

Besitzt ein Spieler keine `homes.limit.*`-Permission, greift der Fallback-Wert
`settings.default-home-limit` aus der `config.yml` (Standard: `1`).
Ist mehr als eine `homes.limit.<n>`-Permission gesetzt, zählt die höchste Zahl.

### Beispiel-Setup mit LuckPerms

```
# Standard-Gruppe: Grundfunktionen + 1 Home
/lp group default permission set homes.set true
/lp group default permission set homes.use true
/lp group default permission set homes.delete true
/lp group default permission set homes.list true
/lp group default permission set homes.limit.1 true

# VIP-Gruppe: 5 Homes
/lp group vip permission set homes.limit.5 true

# MVP-Gruppe: 10 Homes, kein Cooldown
/lp group mvp permission set homes.limit.10 true
/lp group mvp permission set homes.bypass.cooldown true

# Admin-Gruppe: alles
/lp group admin permission set homes.* true
```

## Konfiguration (`config.yml`)

Die Datei ist in drei Bereiche gegliedert:

- **settings** - Prefix, Cooldown, Warmup-Dauer, `cancel-warmup-on-move`,
  Safe-Teleport an/aus, Fallback-Home-Limit
- **effects** - Sounds (Warmup-Start, Teleport), Partikel (Typ + Menge),
  Title beim Ankommen (Fade-in/Stay/Fade-out), jeweils einzeln an-/abschaltbar
- **messages** - alle Chat-Texte, unterstützen `&`-Farbcodes sowie Platzhalter
  wie `%prefix%`, `%nr%`, `%world%`, `%x%`/`%y%`/`%z%`, `%count%`, `%limit%`,
  `%seconds%` (je nach Nachricht - siehe Kommentare in der Datei)

Änderungen werden mit `/homes reload` ohne Neustart übernommen.

## Kompatibilität

FrostbergHomes bindet LuckPerms, Vault, PlaceholderAPI, PlotSquared,
Multiverse-Core und TAB nur als `softdepend` ein (siehe `plugin.yml`): Das
Plugin lädt nach ihnen, falls sie installiert sind, benötigt sie aber nicht
zwingend. Home-Limits funktionieren über normale Bukkit-Permissions und damit
mit jedem Permission-Plugin, das diese unterstützt (LuckPerms empfohlen).

## Projektstruktur

```
frostberg-homes/
├── pom.xml
└── src/main/
    ├── java/de/frostberg/homes/
    │   ├── FrostbergHomes.java          (Hauptklasse)
    │   ├── commands/
    │   │   ├── SetHomeCommand.java      (/set home [nr])
    │   │   ├── DeleteHomeCommand.java   (/delete home [nr])
    │   │   ├── HomeCommand.java         (/home [nr] - Warmup, Cooldown, Safe-TP)
    │   │   └── HomesCommand.java        (/homes, /homes reload)
    │   ├── listener/
    │   │   └── PlayerDataListener.java  (Laden/Entladen bei Join/Quit)
    │   ├── manager/
    │   │   └── HomeManager.java         (YAML-Speicherung, Limits, Cooldown)
    │   ├── model/
    │   │   └── Home.java                (Datenmodell eines Homes)
    │   └── util/
    │       └── MessageUtil.java         (Farbcodes, %prefix%-Ersetzung)
    └── resources/
        ├── plugin.yml
        └── config.yml
```

## Bekannte Grenzen

- Der Safe-Teleport ist eine einfache eigene Implementierung (kein PaperLib):
  Er sucht spaltenweise ab der Home-Höhe zuerst nach oben, dann nach unten
  nach zwei freien, ungefährlichen Blöcken über festem Untergrund.
- PlaceholderAPI-Platzhalter (z.B. `%frostberghomes_count%`) sind nicht
  Teil dieser Version - das Plugin lädt lediglich kompatibel nach PAPI.
