package de.frostberg.homes.farmwelt;

import org.bukkit.Material;

/**
 * Die drei waehlbaren Farmwelten. {@code configKey} ist sowohl der
 * Unterschluessel in {@code settings.farm-worlds.<configKey>} als auch das
 * Argument fuer "/farmwelt <configKey>" und "/setfarmwelt <configKey>".
 */
public enum FarmType {

    OVERWORLD("overworld", Material.GRASS_BLOCK),
    NETHER("nether", Material.NETHERRACK),
    END("end", Material.END_STONE);

    private final String configKey;
    private final Material icon;

    FarmType(String configKey, Material icon) {
        this.configKey = configKey;
        this.icon = icon;
    }

    public String getConfigKey() {
        return configKey;
    }

    public Material getIcon() {
        return icon;
    }

    public static FarmType fromConfigKey(String key) {
        for (FarmType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
