# Minecraft Plugin - Home

Bem-vindo ao repositório dos plugins Home para Minecraft! Estes plugins permitem a personalização e funcionalidades adicionais para o servidor Minecraft, tornando a jogabilidade mais dinâmica e interativa.

## Índice

- [Sobre](#sobre)
- [Funcionalidades](#funcionalidades)
- [Configuração](#configuração)
- [Plugin Home](#plugin-home)
- [Estrutura do Projeto](#estrutura-do-projeto)

## Sobre

Este repositório contém um plugin para Minecraft: Home. Que é configuráveis através do arquivo `config.yml` gerado após a inicialização do servidor, 
lembrando que o plugin Home funciona utilizando um banco de dados `MYSQL` para as informações necessárias de uso.

## Funcionalidades
O plugin Home é utilizado pelos jogadores para marcar um local específico como um ponto fixo para teleporte em qualquer lugar do mapa, ao utilizar o comando "/sethome" o jogador define uma localização para qual ele irá se teleportar ao utilizar o comando "/home" o plugin tem um tempo de recarga (cooldown) ajustado de 60 segundos que pode ser alterado para outra quantidade desejável através do Config.yml gerado ao inicializar o server, o plugin também conta com uma característica visual de teleporte que pode ser alterada entre ligada ou desligada, nesta versão do código a partícula utilizada é  `PORTAL` do banco de dados do Bukkit.

### Plugin Home

- **Comando `/sethome`**: Define a localização atual do jogador como sua "Home".
- **Comando `/home`**: Teleporta o jogador para sua "Home".
- **Configuração de Cooldown**: Tempo de espera configurável para usar o comando `/home`.
- **Partículas**: Ativação de partículas visuais para o teletransporte.

## Configuração

As opções de tempo de recarga do plugin pode ser configurado pelo arquivo `config.yml` gerado após a inicialização do servidor. 
`Ao configurar o código, é necessário a configuração de um banco de dados, como informado anteriormente, neste código foi utilizado um banco de dados MYSQL.
Abaixo está um exemplo das opções de configuração para o plugin.`

### DatabaseManager
```Java
package plug.casa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {

    private Connection connection;
    private final HomePlugin plugin;
    private final String url;
    private final String username;
    private final String password;

    public DatabaseManager(HomePlugin plugin) {
        this.plugin = plugin;
        this.url = plugin.getConfig().getString("database.url");
        this.username = plugin.getConfig().getString("database.username");
        this.password = plugin.getConfig().getString("database.password");
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not close the database connection.", e);
            }
        }
    }
}
```
### HomePluguin

```java
package plug.casa;

import plug.casa.commands.HomeCommand;
import plug.casa.commands.SetHomeCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class HomePlugin extends JavaPlugin {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.databaseManager = new DatabaseManager(this);
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
```


### Estrutura e Lógica de implementação:

### Lógica do Plugin Home

#### Comando `/sethome`

1. **Verificação de Tipo de Remetente**:
   - Apenas jogadores podem usar o comando `/sethome`.

2. **Armazenamento da Localização "Home"**:
   - A localização atual do jogador é armazenada no banco de dados MySQL quando o comando é executado.

3. **Tratamento de Erros**:
   - Se ocorrer um erro ao acessar o banco de dados, uma mensagem de erro é registrada e enviada ao jogador.

#### Comando `/home`

O `HomeCommand` permite que os jogadores se teletransportem para uma localização "Home" previamente definida. Abaixo está uma visão geral da lógica de implementação:

1. **Verificação de Tipo de Remetente**:
   - Apenas jogadores podem usar o comando `/home`.

2. **Verificação de Cooldown**:
   - O comando verifica se o cooldown está ativo para o jogador. Se o cooldown não expirou, uma mensagem informa o tempo restante.

3. **Recuperação da Localização "Home"**:
   - A localização "Home" do jogador é recuperada do banco de dados de forma assíncrona.
   - Se uma localização é encontrada, o jogador é teleportado para essa localização, e partículas são geradas se configuradas.
   - Se nenhuma localização é encontrada, uma mensagem de erro é enviada.

4. **Tratamento de Erros**:
   - Se ocorrer um erro ao acessar o banco de dados, uma mensagem de erro é registrada e enviada ao jogador.
  
### Exemplo de Código
### /sethome
```java
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
```
### /home
```Java
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
```
