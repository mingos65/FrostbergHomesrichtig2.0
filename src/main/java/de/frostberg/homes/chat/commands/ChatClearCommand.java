package de.frostberg.homes.chat.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /cc - schiebt den Chat fuer alle Spieler per Leerzeilen weg, dann eine Bestaetigung. */
public class ChatClearCommand implements CommandExecutor {

    private static final int EMPTY_LINES = 100;

    private final FrostbergHomes plugin;

    public ChatClearCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String blank = " ";
        for (int i = 0; i < EMPTY_LINES; i++) {
            Bukkit.broadcastMessage(blank);
        }

        String message = MessageUtil.get(plugin.getMessages(), "cc-cleared")
                .replace("%staff%", sender.getName());
        Bukkit.broadcastMessage(message);
        return true;
    }
}
