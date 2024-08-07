package org.justt.completeAuth.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.justt.completeAuth.CompleteAuth;

public class LoginCommand implements CommandExecutor {

    private CompleteAuth plugin;

    public LoginCommand(CompleteAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                if (plugin.login(player.getName(), args[0])) {
                    player.sendMessage(plugin.getMessage("login-success"));
                    player.setWalkSpeed(0.2f);
                    player.setFlySpeed(0.1f);
                } else {
                    player.sendMessage(plugin.getMessage("login-fail"));
                }
            } else {
                player.sendMessage(plugin.getMessage("login-usage"));
            }
            return true;
        }
        return false;
    }
}
