package org.justt.completeAuth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.justt.completeAuth.commands.ChangePasswordCommand;
import org.justt.completeAuth.commands.LoginCommand;
import org.justt.completeAuth.commands.RegisterCommand;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class CompleteAuth extends JavaPlugin implements Listener {

    private Map<String, String> playerPasswords = new HashMap<>();
    private Map<String, Long> authenticationTimeouts = new HashMap<>();
    private File passwordsFile;
    private FileConfiguration passwordsConfig;
    private FileConfiguration messagesConfig;
    private int authenticationTimeoutSeconds;
    private boolean hubEnabled;
    private Location hubLocation;

    @Override
    public void onEnable() {

        passwordsFile = new File(getDataFolder(), "passwords.yml");
        if (!passwordsFile.exists()) {
            passwordsFile.getParentFile().mkdirs();
            try {
                passwordsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        passwordsConfig = YamlConfiguration.loadConfiguration(passwordsFile);
        loadPasswords();

        saveDefaultConfig();
        messagesConfig = getConfig();


        authenticationTimeoutSeconds = getConfig().getInt("authentication-timeout-seconds", 60);
        hubEnabled = getConfig().getBoolean("hub.enabled", false);
        if (hubEnabled) {
            String worldName = getConfig().getString("hub.world", "world");
            double x = getConfig().getDouble("hub.x", 0);
            double y = getConfig().getDouble("hub.y", 100);
            double z = getConfig().getDouble("hub.z", 0);
            hubLocation = new Location(Bukkit.getWorld(worldName), x, y, z);
        }


        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("register").setExecutor(new RegisterCommand(this));
        this.getCommand("login").setExecutor(new LoginCommand(this));
        this.getCommand("changepass").setExecutor(new ChangePasswordCommand(this));
        getLogger().info("CompleteAuth has been enabled!");


        Bukkit.getScheduler().runTaskTimer(this, this::checkAuthenticationTimeout, 20L, 20L * 30);
    }

    @Override
    public void onDisable() {
        savePasswords();
        getLogger().info("CompleteAuth has been disabled!");
    }

    private boolean isPremiumPlayer(Player player) {
        try {
            String playerName = player.getName();
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status == 200) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hubEnabled) {
            player.teleport(hubLocation);
        }
        if (isPremiumPlayer(player)){
            if(isPremiumSession(player)){
                player.sendMessage(getMessage("premium-welcome"));
                return;
            } else{
              player.kickPlayer(getMessage("premium-with-cracked"));
              return;
            }
        }
        if (player.hasPlayedBefore()) {

            player.sendMessage(getMessage("login-prompt"));
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
            authenticationTimeouts.put(player.getName(), System.currentTimeMillis());

        } else {
            player.sendMessage(getMessage("register-prompt"));
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
            authenticationTimeouts.put(player.getName(), System.currentTimeMillis());
        }


    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isAuthenticated(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!isAuthenticated(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isAuthenticated(player.getName())) {
            String command = event.getMessage().toLowerCase();
            if (!command.startsWith("/register") && !command.startsWith("/login")) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isPremiumSession(Player player) {
        try {
            String playerName = player.getName();
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();


                String response = content.toString();
                return response.contains(player.getUniqueId().toString().replace("-", ""));
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean registerPassword(String playerName, String password) {
        if (!playerPasswords.containsKey(playerName)) {
            playerPasswords.put(playerName, password);
            savePasswords();
            return true;
        }
        return false;
    }

    public boolean login(String playerName, String password) {
        return playerPasswords.containsKey(playerName) && playerPasswords.get(playerName).equals(password);
    }

    public void changePassword(String playerName, String newPassword) {
        playerPasswords.put(playerName, newPassword);
        savePasswords();
    }

    private boolean isAuthenticated(String playerName) {
        return playerPasswords.containsKey(playerName) && !playerPasswords.get(playerName).isEmpty();
    }

    private void loadPasswords() {
        for (String key : passwordsConfig.getKeys(false)) {
            playerPasswords.put(key, passwordsConfig.getString(key));
        }
    }

    private void savePasswords() {
        for (Map.Entry<String, String> entry : playerPasswords.entrySet()) {
            passwordsConfig.set(entry.getKey(), entry.getValue());
        }
        try {
            passwordsConfig.save(passwordsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String key) {
        String message = messagesConfig.getString("messages." + key, "&cMessage not found: " + key);
        String prefix = "&eCompleteAuth &b-";
        String finalmessage = ChatColor.translateAlternateColorCodes('&', prefix + " " + message);
        return finalmessage;
    }

    private void checkAuthenticationTimeout() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : authenticationTimeouts.entrySet()) {
            String playerName = entry.getKey();
            long joinTime = entry.getValue();
            if (currentTime - joinTime > authenticationTimeoutSeconds * 1000) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null && !isAuthenticated(playerName)) {
                    player.kickPlayer(getMessage("authentication-timeout"));
                }
                authenticationTimeouts.remove(playerName);
            }
        }
    }
}
