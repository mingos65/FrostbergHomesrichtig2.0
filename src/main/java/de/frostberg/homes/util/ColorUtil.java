package de.frostberg.homes.util;

/**
 * Wandelt &amp;#RRGGBB-Hexcodes (und Farbverlaeufe zwischen zwei Hexcodes) in
 * die von Minecraft intern verstandene Legacy-Hex-Escape-Sequenz
 * (&sect;x&sect;R&sect;R&sect;G&sect;G&sect;B&sect;B) um. Funktioniert dadurch mit ganz normalem,
 * String-basiertem player.sendMessage(...)/Item-Lore - es ist KEINE Umstellung
 * auf Adventure-Components fuer den gesamten Chat noetig, nur diese eine
 * kleine Uebersetzung an der Stelle, wo Hex-Farben ins Spiel kommen.
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    /**
     * Ersetzt jedes "&amp;#RRGGBB" in input durch die Legacy-Hex-Sequenz.
     * Text ohne Hexcodes bleibt unveraendert (normale &amp;-Codes werden hier
     * NICHT uebersetzt - dafuer weiterhin MessageUtil.color() im Anschluss nutzen).
     */
    public static String translateHex(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) == '&' && i + 7 < input.length() && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                if (hex.matches("[0-9a-fA-F]{6}")) {
                    result.append(hexToLegacy(hex));
                    i += 8;
                    continue;
                }
            }
            result.append(input.charAt(i));
            i++;
        }
        return result.toString();
    }

    private static String hexToLegacy(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    public static boolean isValidHex(String hex) {
        return hex != null && hex.matches("[0-9a-fA-F]{6}");
    }

    /**
     * Wendet einen gespeicherten Farb-Code (siehe Clan#getTagColor() /
     * ChatColorManager) auf text an - versteht normale Legacy-Codes ("&amp;c"),
     * Hexcodes ("&amp;#RRGGBB") und Verlaeufe ("gradient:RRGGBB:RRGGBB").
     * Gibt bereits fertig eingefaerbten Text zurueck (echte Formatierungs-
     * zeichen, kein rohes &amp; mehr).
     */
    public static String applyColorCode(String code, String text) {
        if (code == null || code.isEmpty()) {
            return MessageUtil.color(text);
        }
        if (code.startsWith("gradient:")) {
            String[] parts = code.substring("gradient:".length()).split(":");
            if (parts.length == 2 && isValidHex(parts[0]) && isValidHex(parts[1])) {
                return gradient(text, parts[0], parts[1]);
            }
            return MessageUtil.color(text);
        }
        if (code.startsWith("&#")) {
            return translateHex(code) + text;
        }
        return MessageUtil.color(code + text);
    }

    /**
     * Faerbt jedes Zeichen von text einzeln in einem linearen Verlauf
     * zwischen fromHex und toHex (je 6-stelliger Hexcode ohne '#'/'&amp;').
     */
    public static String gradient(String text, String fromHex, String toHex) {
        return gradient(text, fromHex, toHex, false);
    }

    /**
     * Wie {@link #gradient(String, String, String)}, haengt aber bei bold=true
     * nach JEDEM Farbcode zusaetzlich &sect;l an - noetig, weil ein neuer
     * Legacy-/Hex-Farbcode in Minecraft alle vorherigen Formatierungen
     * (fett/kursiv/...) zuruecksetzt, ein einzelnes &sect;l am Anfang wuerde also
     * schon vom naechsten Zeichen-Farbwechsel wieder aufgehoben.
     */
    public static String gradient(String text, String fromHex, String toHex, boolean bold) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int r1 = Integer.parseInt(fromHex.substring(0, 2), 16);
        int g1 = Integer.parseInt(fromHex.substring(2, 4), 16);
        int b1 = Integer.parseInt(fromHex.substring(4, 6), 16);
        int r2 = Integer.parseInt(toHex.substring(0, 2), 16);
        int g2 = Integer.parseInt(toHex.substring(2, 4), 16);
        int b2 = Integer.parseInt(toHex.substring(4, 6), 16);

        int length = text.length();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            double ratio = length == 1 ? 0 : (double) i / (length - 1);
            int r = (int) Math.round(r1 + (r2 - r1) * ratio);
            int g = (int) Math.round(g1 + (g2 - g1) * ratio);
            int b = (int) Math.round(b1 + (b2 - b1) * ratio);
            String hex = String.format("%02x%02x%02x", r, g, b);
            result.append(hexToLegacy(hex));
            if (bold) {
                result.append("§l");
            }
            result.append(text.charAt(i));
        }
        return result.toString();
    }
}
