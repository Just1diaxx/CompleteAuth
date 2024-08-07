package org.justt.completeAuth.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.justt.completeAuth.CompleteAuth;

public class ChangePasswordCommand implements CommandExecutor {

    private CompleteAuth plugin;

    public ChangePasswordCommand(CompleteAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 2) {
                String oldPassword = args[0];
                String newPassword = args[1];
                if (plugin.login(player.getName(), oldPassword)) {
                    plugin.changePassword(player.getName(), newPassword);
                    player.sendMessage(plugin.getMessage("changepass-success"));
                } else {
                    player.sendMessage(plugin.getMessage("changepass-fail"));
                }
            } else {
                player.sendMessage(plugin.getMessage("changepass-usage"));
            }
            return true;
        }
        return false;
    }
}
