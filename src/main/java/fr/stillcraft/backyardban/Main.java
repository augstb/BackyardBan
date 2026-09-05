package fr.stillcraft.backyardban;

import fr.stillcraft.backyardban.commands.*;
import fr.stillcraft.backyardban.core.BackyardBanConfig;
import fr.stillcraft.backyardban.listener.loginlistener;
import net.md_5.bungee.api.plugin.Plugin;

public final class Main extends Plugin {
    public static Main instance;
    public static BackyardBanConfig cfg;

    @Override
    public void onEnable() {
        instance = this;
        cfg = new BackyardBanConfig(getDataFolder(), getLogger()::warning);
        cfg.checkAndLoad();

        // Register new commands
        getProxy().getPluginManager().registerListener(this, new loginlistener());
        getProxy().getPluginManager().registerCommand(this, new help());
        getProxy().getPluginManager().registerCommand(this, new ban());
        getProxy().getPluginManager().registerCommand(this, new unban());
        getProxy().getPluginManager().registerCommand(this, new banip());
        getProxy().getPluginManager().registerCommand(this, new backyardban());
        getProxy().getPluginManager().registerCommand(this, new reload());
        getProxy().getPluginManager().registerCommand(this, new version());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Main getInstance() { return instance; }
}
