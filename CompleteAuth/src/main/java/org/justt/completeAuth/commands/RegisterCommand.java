package org.justt.completeAuth.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.justt.completeAuth.CompleteAuth;

public class RegisterCommand implements CommandExecutor {

    private CompleteAuth plugin;

    public RegisterCommand(CompleteAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                if (plugin.registerPassword(player.getName(), args[0])) {
                    player.sendMessage(plugin.getMessage("register-success"));
                } else {
                    player.sendMessage(plugin.getMessage("register-already"));
                }
            } else {
                player.sendMessage(plugin.getMessage("register-usage"));
            }
            return true;
        }
        return false;
    }
}
