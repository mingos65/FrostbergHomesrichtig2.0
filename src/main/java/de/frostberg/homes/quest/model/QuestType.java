package de.frostberg.homes.quest.model;

/**
 * Alle unterstuetzten Aufgaben-Arten. Jeder Typ wird von genau einem Teil des
 * QuestProgressListener bedient (siehe dortige Event-Handler).
 */
public enum QuestType {
    MINE_BLOCK,
    PLACE_BLOCK,
    KILL_ENTITY,
    CRAFT_ITEM,
    FISH,
    WALK_DISTANCE,
    EARN_TOKENS
}
