package fr.stillcraft.backyardban.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.stillcraft.backyardban.core.BackyardBanConfig;
import fr.stillcraft.backyardban.velocity.commands.BackyardBanCommand;
import fr.stillcraft.backyardban.velocity.commands.BanCommand;
import fr.stillcraft.backyardban.velocity.commands.BanipCommand;
import fr.stillcraft.backyardban.velocity.commands.HelpCommand;
import fr.stillcraft.backyardban.velocity.commands.ReloadCommand;
import fr.stillcraft.backyardban.velocity.commands.UnbanCommand;
import fr.stillcraft.backyardban.velocity.commands.VersionCommand;
import fr.stillcraft.backyardban.velocity.listener.VelocityLoginListener;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "backyardban", name = "BackyardBan", version = BackyardBanConfig.VERSION, authors = "Augustin Blanchet",
        description = "BackyardBan is a proxy plugin which allows Minecraft server moderators to ban players from the entire network.")
public final class VelocityMain {
    private static VelocityMain instance;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public BackyardBanConfig cfg;

    @Inject
    public VelocityMain(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        instance = this;
        cfg = new BackyardBanConfig(dataDirectory.toFile(), logger::warn);
        cfg.checkAndLoad();

        server.getEventManager().register(this, new VelocityLoginListener(this));

        // Register commands (same names/aliases/permissions as the BungeeCord side)
        CommandManager commandManager = server.getCommandManager();
        commandManager.register(commandManager.metaBuilder("backyardban:help").plugin(this).build(), new HelpCommand(this));
        commandManager.register(commandManager.metaBuilder("backyardban:ban").aliases("ban").plugin(this).build(), new BanCommand(this));
        commandManager.register(commandManager.metaBuilder("backyardban:unban").aliases("unban", "pardon").plugin(this).build(), new UnbanCommand(this));
        commandManager.register(commandManager.metaBuilder("backyardban:banip").aliases("banip").plugin(this).build(), new BanipCommand(this));
        commandManager.register(commandManager.metaBuilder("backyardban:reload").plugin(this).build(), new ReloadCommand(this));
        commandManager.register(commandManager.metaBuilder("backyardban:version").aliases("backyardban:info").plugin(this).build(), new VersionCommand(this));
        commandManager.register(commandManager.metaBuilder("backyardban").aliases("byb").plugin(this).build(), new BackyardBanCommand(this));

        logger.info("Enabled plugin BackyardBan version " + BackyardBanConfig.VERSION + " by Augustin Blanchet");
    }

    public static VelocityMain getInstance() { return instance; }

    public ProxyServer getServer() { return server; }

    public Logger getLogger() { return logger; }
}
