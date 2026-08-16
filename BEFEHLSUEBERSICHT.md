# FrostbergHomes – Komplette Befehlsübersicht

Stand: 2026-08-16. Alle Befehle aus dem eigenen Plugin FrostbergHomes (Home-,
Teleport-, Clan-System), plus ein kurzer Überblick über die wichtigsten
Befehle anderer, bereits installierter Plugins.

**Spalten:** *Permission* zeigt die Bukkit-Permission und ihren Standardwert.
*Neu?* zeigt, ob der Befehl in diesem großen Update (Home-GUI + Clan-System,
16.08.2026) neu dazugekommen ist oder schon vorher existierte.

---

## 1) Home-Befehle

| Befehl | Beschreibung | Permission | Neu? |
|---|---|---|---|
| `/home` | Öffnet das Home-GUI (Truhen-Fenster, 2 Seiten à 14 Betten) | `homes.use` (Standard: an) | **Verhalten neu** – vorher direkter Teleport zu Home 1 |
| `/home <nr>` | Teleportiert direkt zu Home Nummer `<nr>` (Warmup, Cooldown, sicherer Teleport) | `homes.use` (Standard: an) | Bestehend |
| `/homes` | Öffnet dasselbe Home-GUI wie `/home` | `homes.list` (Standard: an) | **Verhalten neu** – vorher Chat-Liste statt GUI |
| `/homes reload` | Lädt `config.yml` **und** `messages.yml` neu | `homes.reload` (Standard: op) | Bestehend (lädt jetzt auch messages.yml) |
| `/set home [nr]` | Setzt Home `<nr>` (Standard 1) an deiner Position | `homes.set` (Standard: an) | Bestehend |
| `/delete home [nr]` | Löscht Home `<nr>` | `homes.delete` (Standard: an) | Bestehend |
| `/sethome <name>` | Setzt ein Home im nächsten freien Slot mit eigenem Namen | `homes.set` (Standard: an) | **Neu** |
| `/delhome <name>` | Löscht das Home mit diesem Namen | `homes.delete` (Standard: an) | **Neu** |

**Im Home-GUI:** Linksklick auf ein grünes (gesetztes) Home teleportiert
direkt. Rechtsklick öffnet ein Menü mit Teleportieren / Koordinaten im Chat
anzeigen / Umbenennen (Amboss) / Löschen (mit Ja-Nein-Bestätigung). Blaue
Betten sind freie Slots, graue sind durch das Rang-Limit gesperrt.
Home-Limit pro Rang über `homes.limit.1` bis `homes.limit.28` (erweitert von
vorher 10 auf 28, für die 2 GUI-Seiten) sowie `homes.limit.unlimited`.

---

## 2) Teleport-Befehle (TPA & Spawn)

Alle Befehle in diesem Abschnitt sind **komplett neu**.

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/tpa <spieler>` | Fragt an, ob du dich zu ihm teleportieren darfst | `tpa.use` (Standard: an) |
| `/tpahere <spieler>` | Fragt an, ob er sich zu dir teleportieren soll | `tpa.use` (Standard: an) |
| `/tpaccept` | Nimmt die offene Anfrage an (auch per Klick auf `[Annehmen]` im Chat) | `tpa.use` (Standard: an) |
| `/tpdeny` | Lehnt die offene Anfrage ab (auch per Klick auf `[Ablehnen]` im Chat) | `tpa.use` (Standard: an) |
| `/tp <spieler>` | Sofortiger Teleport zum Spieler, ohne Anfrage/Countdown/Cooldown | `tpa.admin` (Standard: op) |
| `/tphere <spieler>` | Holt einen Spieler sofort zu dir, ohne Anfrage | `tpa.admin` (Standard: op) |
| `/spawn` | Teleportiert immer zum selben festen Punkt (in `plotwelt65`) | `spawn.use` (Standard: an) |
| `/farmwelt` | Teleportiert zu einer zufälligen, sicheren Stelle (2.000–4.000 Blöcke vom Mittelpunkt) | `spawn.use` (Standard: an) |
| `/setspawn` | Setzt den `/spawn`-Punkt auf deine Position | `spawn.admin` (Standard: op) |
| `/setfarmwelt` | Setzt den Mittelpunkt für die `/farmwelt`-Zufallsteleports | `spawn.admin` (Standard: op) |

TPA-Anfragen laufen nach 60 Sekunden automatisch ab. Nach Annahme läuft ein
3-Sekunden-Countdown (bricht bei Bewegung ab), danach ein eigener
30-Sekunden-Cooldown, unabhängig vom Homes-Cooldown.

---

## 3) Clan-Befehle

Alle Befehle in diesem Abschnitt sind **komplett neu**. Rollen
(Leader/Clan-Mod/Member) sind reine Clan-Daten, keine LuckPerms-Ränge – wer
was darf, steht in der letzten Spalte.

| Befehl | Beschreibung | Permission | Wer im Clan |
|---|---|---|---|
| `/clan create <name> [tag]` | Gründet einen neuen Clan, du wirst Leader | `clan.use` (Standard: an) | – (noch in keinem Clan) |
| `/clan delete` / `/clan disband` | Löst deinen Clan auf (Ja-Nein-Bestätigung) | `clan.use` | Leader (oder `clan.admin`) |
| `/clan invite <spieler>` | Lädt einen Spieler ein (klickbare Annehmen/Ablehnen-Nachricht) | `clan.use` | Leader / Clan-Mod |
| `/clan accept` | Nimmt die offene Einladung an | `clan.use` | – |
| `/clan deny` | Lehnt die offene Einladung ab | `clan.use` | – |
| `/clan leave` | Verlässt den Clan (Ja-Nein-Bestätigung) | `clan.use` | Alle außer Leader mit weiteren Mitgliedern |
| `/clan kick <spieler>` | Wirft ein Mitglied aus dem Clan | `clan.use` | Leader / Clan-Mod |
| `/clan promote <spieler>` | Befördert ein Mitglied zum Clan-Mod | `clan.use` | Leader |
| `/clan demote <spieler>` | Stuft einen Clan-Mod zum Member zurück | `clan.use` | Leader |
| `/clan info [clan]` | Zeigt Name, Tag, Leader, Mitgliederzahl | `clan.use` | Alle |
| `/clan list` | GUI mit allen Clans (Leader-Köpfe); Klick auf einen Kopf zeigt alle Mitglieder | `clan.use` | Alle |
| `/clan chat <nachricht>` / `/cc <nachricht>` | Nachricht nur an Clan-Mitglieder | `clan.use` | Alle |
| `/clan setbase` | Setzt die Clan-Base auf deine Position | `clan.use` | Leader / Clan-Mod |
| `/clan base` | Teleportiert zur Clan-Base (Warmup wie bei Homes) | `clan.use` | Alle |
| `/clan rename <neuer-name>` | Benennt den Clan um | `clan.use` | Leader |
| `/clan bank` | Zeigt den Kontostand der Clan-Kasse (Tokens + Gold) | `clan.use` | Alle |
| `/clan bank deposit <tokens\|gold> <betrag>` | Zahlt eigenes Guthaben in die Clan-Kasse ein | `clan.use` | Alle |
| `/clan bank withdraw <tokens\|gold> <betrag>` | Hebt aus der Clan-Kasse ab | `clan.use` | Leader / Clan-Mod |

Clan-Tag erscheint automatisch in Tabliste, Scoreboard-Sidebar ("Kein Clan"
wenn keiner vorhanden) und lässt sich auch in den Chat einbinden – läuft über
die eigene PlaceholderAPI-Erweiterung (`%frostbergclans_name%`,
`%frostbergclans_tagdisplay%` u.a.), Details siehe README im Repository.

---

## 4) Wirtschaft (Pay-Brücke, Tokens, Gold)

| Befehl | Beschreibung | Permission | Neu? |
|---|---|---|---|
| `/pay tokens <spieler> <anzahl>` | Sendet Tokens (reicht 1:1 an PlayerPoints weiter) | `frostberg.pay.tokens` (Standard: an) | Bestehend |
| `/pay gold <spieler> <betrag>` | Sendet Gold (direkt über Vault-Economy-API) | `frostberg.pay.tokens` (Standard: an) | Bestehend |

**Tokens (PlayerPoints, fremdes Plugin):** `/tokens`, `/tokens pay`,
`/tokens lead`, `/tokens look`, `/tokens give|take|set` (Admin) usw.

**Gold (ExcellentEconomy, fremdes Plugin):** `/balance`/`/bal`,
`/balancetop`, `/ecoset`, `/ecotake`, `/money give|giveall|reset` (Admin) usw.

---

## 5) Allgemeine / bereits vorhandene Befehle (andere Plugins)

Diese Befehle stammen **nicht** aus FrostbergHomes, sondern aus den anderen
auf dem Server installierten Plugins. Kurzübersicht, keine vollständige
Referenz (eine ausführlichere Liste inkl. WorldEdit/PlotSquared/Multiverse
im Detail hast du bereits als separate Datei von mir bekommen):

| Plugin | Wichtigste Befehle | Wofür |
|---|---|---|
| **LuckPerms** | `/lp user ...`, `/lp group ...`, `/lp editor` | Ränge & Rechte (nur Admin) |
| **PlotSquared** | `/plot claim`, `/plot home`, `/plot trust`, `/plot flag ...` | Grundstücke in `plotwelt65` |
| **Multiverse-Core** | `/mv tp`, `/mv list`, `/mv create` (Admin) | Welten-Verwaltung |
| **FastAsyncWorldEdit** | `//wand`, `//set`, `//copy`/`//paste`, `/brush ...` | Bauen (Builder+) |
| **TAB** | `/tab reload` (Admin) | Tabliste/Scoreboard, läuft sonst automatisch |
| **PlaceholderAPI** | `/papi list`, `/papi parse` (Admin) | Platzhalter-Verwaltung |
| **EconomyShopGUI** | `/shop`, `/sellall` | Item-Shop (Preise noch nicht eingerichtet) |

---

## Kurz gesagt

- **Grün/"kann jeder Spieler"**: Homes verwalten, TPA senden/annehmen,
  `/spawn`/`/farmwelt`, Clan gründen/beitreten/chatten/Base nutzen/einzahlen
- **Braucht Clan-Rolle Leader/Mod**: Clan löschen, einladen, kicken,
  befördern, umbenennen, Base setzen, aus der Kasse abheben
- **Braucht Admin/OP-Permission**: `/tp`/`/tphere`, `/setspawn`/`/setfarmwelt`,
  `/homes reload`
