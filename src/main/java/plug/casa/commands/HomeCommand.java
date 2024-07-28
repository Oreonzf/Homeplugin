package plug.casa.commands;

import plug.casa.HomePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class HomeCommand implements CommandExecutor {

    private final HomePlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public HomeCommand(HomePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        long cooldownTime = plugin.getConfig().getLong("cooldown");

        if (cooldowns.containsKey(playerId)) {
            long lastUsed = cooldowns.get(playerId);
            long timeElapsed = (System.currentTimeMillis() - lastUsed) / 1000;

            if (timeElapsed < cooldownTime) {
                player.sendMessage("You must wait " + (cooldownTime - timeElapsed) + " seconds to use this command again.");
                return true;
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = plugin.getDatabaseManager().getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT x, y, z, yaw, pitch FROM homes WHERE player = ?"
                    );
                    statement.setString(1, player.getName());
                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        double x = resultSet.getDouble("x");
                        double y = resultSet.getDouble("y");
                        double z = resultSet.getDouble("z");
                        float yaw = resultSet.getFloat("yaw");
                        float pitch = resultSet.getFloat("pitch");

                        Location homeLocation = new Location(player.getWorld(), x, y, z, yaw, pitch);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.teleport(homeLocation);
                            if (plugin.getConfig().getBoolean("particles")) {
                                player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, homeLocation, 100);
                            }
                            player.sendMessage("Teleported to your home.");
                        });

                        cooldowns.put(playerId, System.currentTimeMillis());
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("No home set."));
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not retrieve home location from the database.", e);
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("An error occurred while retrieving your home."));
                }
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }
}
