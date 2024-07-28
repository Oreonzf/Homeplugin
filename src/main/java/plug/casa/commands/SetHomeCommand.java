package plug.casa.commands;

import plug.casa.HomePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class SetHomeCommand implements CommandExecutor {

    private final HomePlugin plugin;

    public SetHomeCommand(HomePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        try (Connection connection = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "REPLACE INTO homes (player, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?)"
            );
            statement.setString(1, playerName);
            statement.setDouble(2, x);
            statement.setDouble(3, y);
            statement.setDouble(4, z);
            statement.setFloat(5, yaw);
            statement.setFloat(6, pitch);
            statement.executeUpdate();
            player.sendMessage("Home set successfully.");
        } catch (SQLException e) {
            player.sendMessage("An error occurred while setting your home.");
            plugin.getLogger().log(Level.SEVERE, "Could not save home location to the database.", e);
        }

        return true;
    }
}
