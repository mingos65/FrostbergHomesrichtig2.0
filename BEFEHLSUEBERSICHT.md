# FrostbergHomes – Komplette Befehlsübersicht

Stand: 18.08.2026. Alle Befehle aus dem eigenen Plugin FrostbergHomes (Home-,
Teleport-, Quest-, Clan-, Chat-/Staff-, Shop-System), plus ein Überblick über
die wichtigsten Befehle aller anderen bereits installierten Plugins.

**Spalten:** *Permission* zeigt die Bukkit-Permission und ihren Standardwert.
*Neu?* zeigt, ob der Befehl neu dazugekommen ist oder schon vorher existierte.

---

## 1) Home-Befehle

| Befehl | Beschreibung | Permission | Neu? |
|---|---|---|---|
| `/home` | Öffnet das Home-GUI (Truhen-Fenster, 2 Seiten à 14 Betten = 28 Homes) | `homes.use` (Standard: an) | Bestehend |
| `/home <nr>` | Teleportiert direkt zu Home Nummer `<nr>` (Warmup, Cooldown, sicherer Teleport) | `homes.use` (Standard: an) | Bestehend |
| `/homes` | Öffnet dasselbe Home-GUI wie `/home` | `homes.list` (Standard: an) | Bestehend |
| `/homes reload` | Lädt `config.yml` **und** `messages.yml` neu | `homes.reload` (Standard: op) | Bestehend |
| `/set home [nr]` | Setzt Home `<nr>` (Standard 1) an deiner Position | `homes.set` (Standard: an) | Bestehend |
| `/delete home [nr]` | Löscht Home `<nr>` | `homes.delete` (Standard: an) | Bestehend |
| `/sethome <name>` | Setzt ein Home im nächsten freien Slot mit eigenem Namen | `homes.set` (Standard: an) | Bestehend |
| `/delhome <name>` | Löscht das Home mit diesem Namen | `homes.delete` (Standard: an) | Bestehend |

**Im Home-GUI:** Linksklick auf ein grünes (gesetztes) Home teleportiert
direkt. Rechtsklick öffnet ein Menü mit Teleportieren / Koordinaten im Chat
anzeigen / Umbenennen (Amboss) / Löschen (mit Ja-Nein-Bestätigung). Blaue
Betten sind freie Slots, graue sind durch das Rang-Limit gesperrt (mit
farbigem Sperrhinweis). Home-Limit pro Rang über `homes.limit.1` bis
`homes.limit.28` sowie `homes.limit.unlimited`.

---

## 2) Teleport-Befehle (TPA & Spawn)

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/tpa <spieler>` | Fragt an, ob du dich zu ihm teleportieren darfst | `tpa.use` (Standard: an) |
| `/tpahere <spieler>` | Fragt an, ob er sich zu dir teleportieren soll | `tpa.use` (Standard: an) |
| `/tpaccept` | Nimmt die offene Anfrage an (auch per Klick auf `[Annehmen]` im Chat) | `tpa.use` (Standard: an) |
| `/tpdeny` | Lehnt die offene Anfrage ab (auch per Klick auf `[Ablehnen]` im Chat) | `tpa.use` (Standard: an) |
| `/tp <spieler>` | Sofortiger Teleport zum Spieler, ohne Anfrage/Countdown/Cooldown | `tpa.admin` (Standard: op) |
| `/tphere <spieler>` | Holt einen Spieler sofort zu dir, ohne Anfrage | `tpa.admin` (Standard: op) |
| `/spawn` | Teleportiert immer zum selben festen Punkt (in `plotwelt65`) | `spawn.use` (Standard: an) |
| `/farmwelt` | Öffnet ein Auswahl-GUI für 3 Farmwelten (Overworld/Nether/End), teleportiert zu einer zufälligen, sicheren Stelle im jeweils konfigurierten Radius | `spawn.use` (Standard: an) |
| `/farmwelt <overworld\|nether\|end>` | Shortcut: teleportiert direkt in die angegebene Farmwelt, ohne das GUI zu öffnen | `spawn.use` (Standard: an) |
| `/setspawn` | Setzt den `/spawn`-Punkt auf deine Position | `spawn.admin` (Standard: op) |
| `/setfarmwelt <overworld\|nether\|end>` | Setzt den Mittelpunkt für die Zufallsteleports der angegebenen Farmwelt (du musst dort stehen) | `spawn.admin` (Standard: op) |

TPA-Anfragen laufen nach 60 Sekunden automatisch ab. Nach Annahme läuft ein
3-Sekunden-Countdown (bricht bei Bewegung ab), danach ein eigener
30-Sekunden-Cooldown, unabhängig vom Homes-Cooldown.

---

## 3) Clan-Befehle

Rollen (Leader/Clan-Mod/Member) sind reine Clan-Daten, keine LuckPerms-Ränge –
wer was darf, steht in der letzten Spalte.

| Befehl | Beschreibung | Permission | Wer im Clan |
|---|---|---|---|
| `/clan create <name> <tag>` | Gründet einen neuen Clan, du wirst Leader. Tag ist **Pflicht** (2-4 Zeichen), Name 2-15 Zeichen | `clan.use` (Standard: an) | – (noch in keinem Clan) |
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
| `/clan color` | Öffnet den Farben-Shop für den Clan-Tag (Kauf mit Gold, inkl. Hex-/Verlauf-Farben in höheren Preisstufen) | `clan.use` | Leader / Clan-Mod |

Clan-Tag erscheint automatisch in Tabliste, Scoreboard-Sidebar ("Kein Clan"
wenn keiner vorhanden) und im Chat vor dem Rang-Präfix – läuft über die
eigene PlaceholderAPI-Erweiterung (`%frostbergclans_name%`,
`%frostbergclans_tagdisplay%` u.a.).

---

## 4) Quest-Befehle

Daily-/Weekly-/Monthly-Quest-System. Alle Spieler bekommen pro Periode
dieselben, zufällig aus dem Pool gewählten Quests (nicht individuell
zufällig). Fortschritt zählt nur in der Farmwelt.

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/quest`, `/quests` | Öffnet das Quest-GUI (Täglich/Wöchentlich/Monatlich, Countdown, Statistik, Hilfe-Buch) | `quest.use` (Standard: an) |
| `/quest top` | Zeigt die Bestenliste nach abgeschlossenen Quests | `quest.use` (Standard: an) |
| `/quest reload` | Lädt `quests.yml` neu (Pool-Änderungen) | `quest.admin` (Standard: op) |
| `/quest reset <spieler> <daily\|weekly\|monthly>` | Setzt den Fortschritt einer Kategorie für einen Spieler zurück | `quest.admin` (Standard: op) |
| `/quest info <spieler>` | Zeigt Fortschritt, Streak und Statistik eines Spielers | `quest.admin` (Standard: op) |
| `/quest broadcast <text>` | Sendet eine Nachricht an alle Online-Spieler | `quest.admin` (Standard: op) |

Reset-Zeitpunkte: Täglich (1 von 10 möglichen Quests) jeden Tag 00:00,
Wöchentlich (3 von 13) jeden Montag 00:00, Monatlich (5 von 13) am 1. jedes
Monats 00:00. War der Server zum Reset-Zeitpunkt offline, wird der Reset
beim nächsten Start automatisch nachgeholt.

**Belohnung:** Jede Quest gibt immer Tokens, gestaffelt nach den 1-5
Schwierigkeits-Sternen (Täglich 550-1.600, Wöchentlich 1.600-4.600,
Monatlich 4.800-13.600). Ab 3 Sternen gibt es zusätzlich etwas Gold obendrauf
(1 Gold = 1.000 Tokens). Belohnungen müssen manuell im GUI abgeholt werden.
Kategorie-Bonus (nur Wöchentlich/Monatlich) und automatischer Daily-Streak-
Bonus kommen zusätzlich dazu.

---

## 5) Chat- & Staff-Befehle

| Befehl | Beschreibung | Permission | Neu? |
|---|---|---|---|
| `/chatcolor <farbe\|hex\|gradient\|bold\|reset>` | Eigene Chat-Farbe einstellen (16 deutsche Farbnamen, Standard). Hex/Verlauf brauchen `frostbergchat.color.rgb`, Fett `frostbergchat.color.bold` | `frostbergchat.color.use` (Standard: an) | Neu |
| `/gm <0\|1\|2\|3>` | Schneller eigener Gamemode-Wechsel (Survival/Creative/Adventure/Spectator) | `frostberg.gamemode` (Standard: op) | Neu |
| `/vanish`, `/v` | Schaltet eigene Unsichtbarkeit für normale Spieler um | `frostberg.vanish` (Standard: op) | Neu |

Spieler mit der Permission `frostbergchat.teamline` bekommen automatisch vor
jeder ihrer Chat-Nachrichten eine optische Leerzeile für alle sichtbar
(kein eigener Befehl, reine Permission-Freischaltung).

---

## 6) Shop-Befehle

Eigenes Shop-System (Ersatz für EconomyShopGUI), läuft komplett auf Deutsch
und ausschließlich mit Tokens.

| Befehl | Beschreibung | Permission | Neu? |
|---|---|---|---|
| `/shop` | Öffnet das Shop-Hauptmenü (Kategorien: Kampf, Bauen, Dekoration, Farming, Magie, Mobs, Sonstiges) | `shop.use` (Standard: an) | Neu |
| `/shop reload` | Lädt `shop-items.yml` neu (Preisänderungen ohne Serverneustart) | `shop.admin` (Standard: op) | Neu |

**Im Shop-GUI (4 Ebenen):** Hauptmenü (9 Kategorien: Blöcke, Werkzeuge,
Waffen, Rüstung, Essen, Redstone, Farming, Deko, Sonstiges) → Kategorie-
Übersicht mit Icons der Unterkategorien (z.B. bei Blöcke: Holz/Stein/Erde &
Natur/Nether/End/Glas) → Item-Liste → Kauf-/Verkauf-Fenster für das
angeklickte Item (Item mittig, rechts grüne Scheiben +1/+32/+64 zum Kaufen,
links rote Scheiben -1/-32/-64 zum Verkaufen, inkl. aktuellem Guthaben in
der Beschreibung). Unten auf jeder Ebene an derselben Stelle: Zurück und
Hauptmenü. Jedes Item gibt es nur in genau einer Kategorie.

---

## 7) Wirtschaft (Pay-Brücke, Tokens, Gold)

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/pay tokens <spieler> <anzahl>` | Sendet Tokens (reicht 1:1 an PlayerPoints weiter) | `frostberg.pay.tokens` (Standard: an) |
| `/pay gold <spieler> <betrag>` | Sendet Gold (direkt über Vault-Economy-API) | `frostberg.pay.tokens` (Standard: an) |

**Tokens (PlayerPoints, fremdes Plugin):** `/tokens`, `/tokens pay`,
`/tokens lead`, `/tokens look`, `/tokens give|take|set` (Admin) usw.

**Gold (ExcellentEconomy, fremdes Plugin):** `/balance`/`/bal`,
`/balancetop`, `/ecoset`, `/ecotake`, `/money give|giveall|reset` (Admin) usw.

---

## 8) Allgemeine / bereits vorhandene Befehle (andere Plugins)

Diese Befehle stammen **nicht** aus FrostbergHomes, sondern aus den anderen
auf dem Server installierten Plugins. Kurzübersicht, keine vollständige
Referenz.

| Plugin | Wichtigste Befehle | Wofür |
|---|---|---|
| **LuckPerms** | `/lp user ...`, `/lp group ...`, `/lp editor` | Ränge & Rechte (nur Admin) |
| **PlotSquared** | `/plot claim`, `/plot home`, `/plot trust`, `/plot flag ...` | Grundstücke in `plotwelt65` |
| **Multiverse-Core** | `/mv tp`, `/mv list`, `/mv create` (Admin) | Welten-Verwaltung |
| **FastAsyncWorldEdit** | `//wand`, `//set`, `//copy`/`//paste`, `/brush ...` | Bauen (Builder+) |
| **TAB** | `/tab reload` (Admin) | Tabliste/Scoreboard, läuft sonst automatisch |
| **PlaceholderAPI** | `/papi list`, `/papi parse` (Admin) | Platzhalter-Verwaltung |
| **EconomyShopGUI** | `/shop`, `/sellall` | Wird schrittweise durch den eigenen Shop (Abschnitt 6) ersetzt |

---

## Kurz gesagt

- **Grün/"kann jeder Spieler"**: Homes verwalten, TPA senden/annehmen,
  `/spawn`/`/farmwelt`, Clan gründen/beitreten/chatten/Base nutzen/einzahlen,
  `/quest` nutzen und Belohnungen abholen, `/chatcolor` (Standardfarben),
  `/shop` nutzen
- **Braucht Clan-Rolle Leader/Mod**: Clan löschen, einladen, kicken,
  befördern, umbenennen, Base setzen, aus der Kasse abheben, Farbe kaufen
- **Braucht Admin/OP-Permission**: `/tp`/`/tphere`, `/setspawn`/`/setfarmwelt`,
  `/homes reload`, `/quest reload`/`reset`/`info`/`broadcast`, `/gm`,
  `/vanish`/`/v`, `/shop reload`
