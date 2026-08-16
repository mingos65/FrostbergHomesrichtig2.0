# FrostbergHomes

Eigenes Homes-Plugin für Paper 1.21.x (Java 21) - kein Essentials nötig.
Nummerierte Homes, farbenfrohe Chat-Nachrichten, Warmup-Countdown im Chat,
Cooldown, Safe-Teleport und optionale Sound-/Partikel-/Title-Effekte. Dazu ein
TPA-System (`/tpa`, `/tpahere`) mit klickbaren Annehmen/Ablehnen-Buttons im
Chat, ein sofortiger Admin-Teleport (`/tp`, `/tphere`), ein Spawn-/Farmwelt-
System (`/spawn`, `/farmwelt`), ein komplettes Clan-System (`/clan`, `/cc`)
sowie ein Daily/Weekly/Monthly-Quest-System (`/quest`) mit eigenem GUI,
Streak- und Kategorie-Boni.
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
| `/tpa <spieler>`        | Fragt, ob du dich zu ihm teleportieren darfst | `tpa.use` |
| `/tpahere <spieler>`    | Fragt, ob er sich zu dir teleportieren soll | `tpa.use` |
| `/tpaccept`             | Nimmt die offene Anfrage an (auch per Klick) | `tpa.use` |
| `/tpdeny`               | Lehnt die offene Anfrage ab (auch per Klick) | `tpa.use` |
| `/tp <spieler>`         | Sofortiger Teleport zu einem Spieler, ohne Anfrage | `tpa.admin` |
| `/tphere <spieler>`     | Teleportiert einen Spieler sofort zu dir | `tpa.admin` |

Alle Befehle unterstützen Tab-Complete (z.B. Vorschläge für vorhandene
Home-Nummern bei `/home` und `/delete home`, Online-Spielernamen bei den
TPA-Befehlen).

Bei `/tpa`/`/tpahere` bekommt der Zielspieler eine anklickbare Chat-Zeile mit
`[Annehmen]`/`[Ablehnen]`-Buttons (funktioniert genauso wie das Eintippen von
`/tpaccept`/`/tpdeny`). Eine unbeantwortete Anfrage läuft nach
`settings.tpa-expiry-seconds` (Standard 60s) automatisch ab. Nach Annahme
läuft derselbe Warmup-Countdown wie bei `/home` (`settings.warmup-seconds`,
abbrechbar bei Bewegung), danach greift ein eigener, vom Homes-Cooldown
unabhängiger TPA-Cooldown (`settings.cooldown-seconds`).

## Quest-System

`/quest` (Alias `/quests`) öffnet ein GUI mit drei Kategorien: Täglich (1
Quest, Reset jeden Tag 00:00), Wöchentlich (3 Quests, Reset jeden Montag
00:00) und Monatlich (5 Quests, Reset am 1. jedes Monats 00:00). Pro Periode
wird die aktive Auswahl zufällig aus dem Pool in `quests.yml` gezogen - für
**alle Spieler gleich**, nicht pro Spieler. War der Server zum Reset-
Zeitpunkt offline, wird der Reset beim nächsten Start automatisch nachgeholt.

Unterstützte Quest-Typen: Blöcke abbauen/platzieren, Mobs töten, craften,
fischen, Strecke laufen, Tokens verdienen - alles zählt nur in der Farmwelt
(`settings.farm-world`). Belohnungen (Tokens/Gold) müssen manuell im GUI
abgeholt werden - genau wie der Kategorie-Bonus: erscheint dort als eigenes
Item, grau/gesperrt solange nicht alle Quests der Kategorie fertig sind, grün
und abholbar sobald alle fertig sind. Ein Rang-Multiplikator und ein
automatischer Streak-Bonus für aufeinanderfolgende Daily-Tage sind über
`quests.yml`/`config.yml` einstellbar.

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/quest`, `/quests` | Öffnet das Quest-GUI | `quest.use` |
| `/quest top` | Bestenliste nach abgeschlossenen Quests | `quest.use` |
| `/quest reload` | Lädt `quests.yml` neu (Pool-Änderungen) | `quest.admin` |
| `/quest reset <spieler> <daily\|weekly\|monthly>` | Setzt Fortschritt einer Kategorie zurück | `quest.admin` |
| `/quest info <spieler>` | Zeigt Fortschritt/Streak/Statistik eines Spielers | `quest.admin` |
| `/quest broadcast <text>` | Sendet eine Nachricht an alle Online-Spieler | `quest.admin` |

Quest-Definitionen (Namen, Ziele, Mengen, Belohnungen, Pool-Größe je
Kategorie) stehen komplett in `plugins/FrostbergHomes/quests.yml` mit
Beispielen zum einfachen Anpassen. Die technischen Einstellungen (Fortschritts-
Nachrichten, Erinnerungen, Effekte, Rang-Multiplikatoren) stehen im neuen
`quest:`-Block der `config.yml` und werden mit `/homes reload` übernommen.

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
| `homes.limit.1` … `homes.limit.28` | false | Erlaubt die jeweilige Anzahl an Homes (28 = alle Slots auf beiden Seiten im /homes-GUI) |
| `homes.limit.unlimited`     | false    | Unbegrenzt viele Homes                          |
| `homes.*`                   | false    | Sammel-Permission für alles Obige               |
| `tpa.use`                   | true     | `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny` nutzen |
| `tpa.admin`                 | op       | `/tp`, `/tphere` (sofort, ohne Anfrage) nutzen  |
| `tpa.bypass.cooldown`       | op       | Ignoriert den TPA-Cooldown                      |
| `tpa.bypass.warmup`         | op       | Ignoriert den Warmup-Countdown nach TPA-Annahme |
| `tpa.*`                     | false    | Sammel-Permission für alles TPA-Obige           |
| `quest.use`                 | true     | `/quest`, `/quests`, `/quest top` nutzen        |
| `quest.admin`                | op       | `/quest reload/reset/info/broadcast` + Vorschau der naechsten Periode im GUI |
| `quest.bypass.world`        | op       | Quest-Fortschritt zählt auch außerhalb der Farmwelt |
| `quest.bypass.creative`     | op       | Quest-Fortschritt zählt auch im Creative-Modus  |
| `quest.multiplier.vip` / `.premium` | false | Rang-Multiplikator für Quest-Belohnungen (siehe `config.yml`) |
| `quest.*`                   | false    | Sammel-Permission für alles Quest-Obige         |

Besitzt ein Spieler keine `homes.limit.*`-Permission, greift der Fallback-Wert
`settings.default-home-limit` aus der `config.yml` (Standard: `1`).
Ist mehr als eine `homes.limit.<n>`-Permission gesetzt, zählt die höchste Zahl.

### Beispiel-Setup mit LuckPerms

Passend zu den tatsächlich auf FrostbergMC konfigurierten Rängen (`default`,
`helfer`, `builder`, `mod`, `srmod`, `dev`, `admin`, `owner`):

```
# default: Grundfunktionen + 3 Homes
/lp group default permission set homes.set true
/lp group default permission set homes.use true
/lp group default permission set homes.delete true
/lp group default permission set homes.list true
/lp group default permission set homes.limit.3 true

# helfer: 5 Homes
/lp group helfer permission set homes.limit.5 true

# builder & mod: 8 Homes
/lp group builder permission set homes.limit.8 true
/lp group mod permission set homes.limit.8 true

# srmod & dev: 12 Homes
/lp group srmod permission set homes.limit.12 true
/lp group dev permission set homes.limit.12 true

# admin & owner: 14 Homes (Maximum), kein Cooldown/Warmup
/lp group admin permission set homes.limit.14 true
/lp group admin permission set homes.bypass.cooldown true
/lp group admin permission set homes.bypass.warmup true
/lp group owner permission set homes.* true
```

## Konfiguration (`config.yml` + `messages.yml`)

Zwei getrennte Dateien:

**`config.yml`** - reine Einstellungen, in zwei Bereiche gegliedert:
- **settings** - Cooldown, Warmup-Dauer, `cancel-warmup-on-move`,
  Safe-Teleport an/aus, Fallback-Home-Limit, Spawn-/Farmwelt-Namen und -Radius
- **effects** - Sounds (Warmup-Start, Teleport), Partikel (Typ + Menge),
  Title beim Ankommen (Fade-in/Stay/Fade-out), jeweils einzeln an-/abschaltbar

**`messages.yml`** - alle Chat- und GUI-Texte an einer Stelle, inkl. Prefix
(oberster Eintrag `prefix:`). Unterstützen `&`-Farbcodes sowie Platzhalter wie
`%prefix%`, `%nr%`, `%world%`, `%x%`/`%y%`/`%z%`, `%count%`, `%limit%`,
`%seconds%`, `%name%`, `%page%`/`%pages%` (je nach Nachricht - siehe
Kommentare in der Datei).

Änderungen an beiden Dateien werden mit `/homes reload` ohne Neustart übernommen.

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
    │   │   ├── SetHomeByNameCommand.java   (/sethome <name>)
    │   │   ├── DeleteHomeByNameCommand.java (/delhome <name>)
    │   │   ├── HomeCommand.java         (/home [nr] - Warmup, Cooldown, Safe-TP, GUI)
    │   │   ├── HomesCommand.java        (/homes, /homes reload)
    │   │   ├── TpaCommand.java          (/tpa, /tpahere - klickbare Anfrage)
    │   │   ├── TpaAcceptCommand.java    (/tpaccept)
    │   │   ├── TpaDenyCommand.java      (/tpdeny)
    │   │   ├── AdminTpCommand.java      (/tp, /tphere - sofort, ohne Anfrage)
    │   │   ├── SpawnCommand.java        (/spawn, /farmwelt)
    │   │   └── SetSpawnCommand.java     (/setspawn, /setfarmwelt)
    │   ├── gui/
    │   │   ├── HomeGuiHolder.java       (Marker fuer die Homes-GUI-Fenster)
    │   │   └── HomesGuiListener.java    (Klick-Logik: Uebersicht, Detail, Umbenennen, Loeschen)
    │   ├── clan/
    │   │   ├── commands/
    │   │   │   ├── ClanCommand.java     (/clan <unterbefehl> - kompletter Router)
    │   │   │   └── ClanChatCommand.java (/cc - Alias fuer /clan chat)
    │   │   ├── gui/
    │   │   │   ├── ClanGuiHolder.java   (Marker fuer die Clan-GUI-Fenster)
    │   │   │   └── ClanGuiListener.java (Clan-Liste mit Koepfen, Loesch-Bestaetigung)
    │   │   ├── manager/
    │   │   │   └── ClanManager.java     (YAML-Speicherung, Mitglieder, Einladungen)
    │   │   └── model/
    │   │       ├── Clan.java            (Datenmodell eines Clans)
    │   │       └── ClanInvite.java      (Datenmodell einer offenen Einladung)
    │   ├── listener/
    │   │   └── PlayerDataListener.java  (Laden/Entladen bei Join/Quit)
    │   ├── manager/
    │   │   ├── HomeManager.java         (YAML-Speicherung, Limits, Cooldown)
    │   │   └── TpaManager.java          (Anfragen, Ablauf-Timer, TPA-Cooldown)
    │   ├── model/
    │   │   ├── Home.java                (Datenmodell eines Homes)
    │   │   └── TpaRequest.java          (Datenmodell einer TPA-Anfrage)
    │   ├── quest/
    │   │   ├── commands/
    │   │   │   └── QuestCommand.java    (/quest, /quests - Router inkl. Admin-Unterbefehle)
    │   │   ├── gui/
    │   │   │   ├── QuestGuiHolder.java  (Marker fuer die Quest-GUI-Fenster)
    │   │   │   └── QuestGuiListener.java (Hauptmenue, Kategorie-Ansicht, Hilfe-Buch, Abholen)
    │   │   ├── listener/
    │   │   │   └── QuestProgressListener.java (Abbauen/Platzieren/Toeten/Craften/Fischen/Laufen)
    │   │   ├── manager/
    │   │   │   └── QuestManager.java    (Pool-Auswahl, Reset-Engine, Persistenz, Belohnungen)
    │   │   └── model/
    │   │       ├── Quest.java           (Datenmodell einer Quest-Definition)
    │   │       ├── QuestType.java       (Abbauen/Platzieren/Toeten/Craften/Fischen/Laufen/Tokens)
    │   │       ├── QuestCategory.java   (DAILY/WEEKLY/MONTHLY)
    │   │       └── PlayerQuestData.java (Fortschritt/Abholungen/Streak/Statistik eines Spielers)
    │   ├── tokens/commands/
    │   │   └── PayCommand.java          (/pay tokens|gold)
    │   └── util/
    │       ├── MessageUtil.java         (Farbcodes, %prefix%-Ersetzung, Klick-Komponenten)
    │       ├── TeleportWarmup.java      (gemeinsamer Warmup-Countdown fuer /home, TPA & Clan-Base)
    │       ├── SafeTeleport.java        (gemeinsame sichere-Landestelle-Suche fuer /home & /farmwelt)
    │       ├── CurrencyBridge.java      (Tokens/Gold-Bruecke fuer Quest-Belohnungen)
    │       ├── ClanPlaceholderExpansion.java (PlaceholderAPI: %frostbergclans_...% fuer TAB/Chat)
    │       └── QuestPlaceholderExpansion.java (PlaceholderAPI: %frostbergquests_...%)
    └── resources/
        ├── plugin.yml
        ├── config.yml                  (Einstellungen)
        ├── messages.yml                (alle Chat-/GUI-Texte)
        └── quests.yml                  (Quest-Pool je Kategorie, Kategorie-/Streak-Boni)
```

## Bekannte Grenzen

- Der Safe-Teleport ist eine einfache eigene Implementierung (kein PaperLib):
  Er sucht spaltenweise ab der Home-Höhe zuerst nach oben, dann nach unten
  nach zwei freien, ungefährlichen Blöcken über festem Untergrund.
- PlaceholderAPI-Platzhalter (z.B. `%frostberghomes_count%`) sind nicht
  Teil dieser Version - das Plugin lädt lediglich kompatibel nach PAPI.
- Quest-Fortschritt bei Shift-Klick-Craften ist eine Schätzung (kleinste
  Zutaten-Stapelgröße im Crafting-Raster), kein exaktes Nachbauen der
  Vanilla-Schleife.
- "Verdiene X Tokens"-Quests laufen über einen periodischen Kontostand-
  Vergleich per PlaceholderAPI (`%playerpoints_points%`), nicht über ein
  PlayerPoints-Event - dafür müssen PlaceholderAPI und die PlayerPoints-
  Erweiterung installiert sein.
