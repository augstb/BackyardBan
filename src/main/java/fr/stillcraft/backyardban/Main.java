package fr.stillcraft.backyardban;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;

import fr.stillcraft.backyardban.commands.*;
import fr.stillcraft.backyardban.listener.loginlistener;

public final class Main extends Plugin {
    public static Main instance;
    public static Configuration config;
    public static Configuration locale;
    public static Configuration banlist;
    public static Configuration knownplayers;

    // Version (don't forget to increment)
    public static final String version = "1.0";
    // Used config files keys
    private static final String[] locale_keys = {
            "ban.banned","ban.until","ban.confirm","ban.info","ban.bypass","ban.bypass_warn","ban.usage","ban.description","ban.yourself",
            "unban.confirm","unban.info","unban.usage","unban.description","unban.yourself","unban.notfound",
            "banip.banned","banip.until","banip.confirm","banip.info","banip.bypass","banip.bypass_warn","banip.usage","banip.description","banip.yourself",
            "global.reason","global.separator","global.punctuation","global.usage","global.description","global.prefix",
            "global.days","global.hours","global.minutes","global.seconds",
            "help.usage","help.description",
            "version.usage","version.description",
            "reload.success","reload.usage","reload.description",
            "global.version" // VERSION SHOULD BE LAST
    };
    private static final String[] config_keys  = {"version","locale","broadcast"};

    @Override
    public void onEnable() {
        instance = this;

        checkConfig("config");
        checkConfig("locale_fr");
        checkConfig("locale_en");
        checkConfig("banlist");
        checkConfig("knownplayers");
        try {
            // Load config file
            config = getInstance().getConfig("config");
            String locale_string = config.getString("locale");
            locale = getInstance().getConfig("locale_" + locale_string);
            banlist = getInstance().getConfig("banlist");
            knownplayers = getInstance().getConfig("knownplayers");

            // Register new commands
            getProxy().getPluginManager().registerListener(this, new loginlistener());
            getProxy().getPluginManager().registerCommand(this, new help());
            getProxy().getPluginManager().registerCommand(this, new ban());
            getProxy().getPluginManager().registerCommand(this, new unban());
            // getProxy().getPluginManager().registerCommand(this, new banip());
            getProxy().getPluginManager().registerCommand(this, new backyardban());
            getProxy().getPluginManager().registerCommand(this, new reload());
            getProxy().getPluginManager().registerCommand(this, new version());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Main getInstance() { return instance; }

    public static void checkConfig(String fileName) {
        if(!Main.getInstance().getDataFolder().exists()){
            Main.getInstance().getDataFolder().mkdir();
        }
        File file = new File(Main.getInstance().getDataFolder(), fileName+".yml");
        try {
            boolean save_config = false;
            if (!file.exists()) {
                // Initialize configuration
                file.createNewFile();
                Configuration config = Main.getInstance().getConfig(fileName);

                // Writing default config values
                if (fileName.equals("locale_en") || fileName.equals("locale_fr")) {
                    for (String locale_key : locale_keys) {
                        config.set(locale_key, Main.getInstance().defaultConfig(locale_key, fileName));
                    }
                }
                else if (fileName.equals("config")) {
                    for (String config_key : config_keys) {
                        String temp_str = Main.getInstance().defaultConfig(config_key, fileName);
                        if (Boolean.parseBoolean(temp_str)) config.set(config_key, Boolean.parseBoolean(temp_str));
                        else config.set(config_key, temp_str);
                    }
                }
                // Save configuration
                Main.getInstance().saveConfig(config, fileName);
            } else { // Check config data (add keys if does not exists)
                Configuration config = Main.getInstance().getConfig(fileName);
                if (fileName.equals("locale_en") || fileName.equals("locale_fr")) {
                    for (int i=0; i<locale_keys.length; i++){                                   // browse locale keys ...
                        if (!locale_keys[i].equals("global.version")) {                         // if not global.version key
                            if (config.getString(locale_keys[i]).isEmpty()) {                   // if key is empty
                                if (!config.getString("global.version").equals(version)) { // if versions are not the same
                                    // config conversion
                                } else {                                                        // if versions are the same add default
                                    config.set(locale_keys[i], Main.getInstance().defaultConfig(locale_keys[i], fileName));
                                }
                                save_config = true;
                            } else if (!config.getString("global.version").equals(version)) {
                                // config conversion
                            }
                        } else {
                            if(!config.getString(locale_keys[i]).equals(version)){ // modify version if does not coincides with plugin version.
                                config.set(locale_keys[i], Main.getInstance().defaultConfig(locale_keys[i], fileName));
                                // Throw old config keys
                                save_config = true;
                            }
                        }
                    }
                }
                if (fileName.equals("config")) {
                    for (String config_key : config_keys) {
                        if (config.getString(config_key).isEmpty()) {
                            // Handle Boolean types
                            String temp_str = Main.getInstance().defaultConfig(config_key, fileName);
                            if (Boolean.parseBoolean(temp_str)) config.set(config_key, Boolean.parseBoolean(temp_str));
                            else config.set(config_key, temp_str);
                            save_config = true;
                        }
                    }
                }
                // Save configuration
                if (save_config) Main.getInstance().saveConfig(config, fileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String defaultConfig(String key, String locale){
        // config file default values :
        if(key.equals("version"))                 return version;
        if(key.equals("locale"))                  return "en";
        if(key.equals("broadcast"))               return "true";

        // locale files default values :
        if(locale.equals("locale_en")) {
            if(key.equals("global.reason"))       return "&c%reason%";
            if(key.equals("global.separator"))    return "&7: ";
            if(key.equals("global.punctuation"))  return "&7.";
            if(key.equals("global.usage"))        return "&fUsage: ";
            if(key.equals("global.description"))  return "&fDescription: ";
            if(key.equals("global.prefix"))       return "&f[BackyardBan]";
            if(key.equals("global.version"))      return version;
            if(key.equals("global.days"))         return "d";
            if(key.equals("global.hours"))        return "h";
            if(key.equals("global.minutes"))      return "m";
            if(key.equals("global.seconds"))      return "s";

            if(key.equals("ban.banned"))          return "&7You have been banned by &f%sender%";
            if(key.equals("ban.until"))           return "&7(&f%timeleft%&7 left)";
            if(key.equals("ban.confirm"))         return "&7You banned &f%player%";
            if(key.equals("ban.info"))            return "&f%player% &7has been banned by &f%sender%";
            if(key.equals("ban.unknown"))         return "&cError: &4%player%&c is unknown.";
            if(key.equals("ban.bypass"))          return "&7You can't ban &f%player%&7.";
            if(key.equals("ban.bypass_warn"))     return "&f%sender% &7tried to ban you.";
            if(key.equals("ban.usage"))           return "&3/ban &b[playername] &et:(1h|1d|1m|1y) &b(reason)";
            if(key.equals("ban.description"))     return "&7Ban player with a message.";
            if(key.equals("ban.yourself"))        return "&7You can't ban yourself.";

            if(key.equals("unban.confirm"))       return "&7You unbanned &f%player%&7.";
            if(key.equals("unban.info"))          return "&f%player% &7has been unbanned by &f%sender%&7.";
            if(key.equals("unban.usage"))         return "&3/unban &b[playername]";
            if(key.equals("unban.description"))   return "&7Unban player.";
            if(key.equals("unban.yourself"))      return "&7You can't unban yourself.";
            if(key.equals("unban.notfound"))      return "&f%player% &7is not banned.";

            if(key.equals("banip.banned"))        return "&7Your IP has been banned by &f%sender%";
            if(key.equals("banip.until"))         return "&7(&f%timeleft%&7 left)";
            if(key.equals("banip.confirm"))       return "&7You banned &f%ip%";
            if(key.equals("banip.info"))          return "&f%ip% &7has been banned by &f%sender%";
            if(key.equals("banip.bypass"))        return "&7You can't ban &f%ip%&7.";
            if(key.equals("banip.bypass_warn"))   return "&f%sender% &7tried to ban you.";
            if(key.equals("banip.usage"))         return "&3/banip &b[ip] &et:(1h|1d|1m|1y) &b(reason)";
            if(key.equals("banip.description"))   return "&7Ban IP with a message.";
            if(key.equals("banip.yourself"))      return "&7You can't ban your own IP.";

            if(key.equals("help.usage"))          return "&3/backyardban:help";
            if(key.equals("help.description"))    return "&7Show the help page.";

            if(key.equals("version.usage"))       return "&3/backyardban:version";
            if(key.equals("version.description")) return "&7Show plugin version.";

            if(key.equals("reload.success"))      return "&7Config and locale files reloaded.";
            if(key.equals("reload.usage"))        return "&3/backyardban:reload";
            if(key.equals("reload.description"))  return "&7Reload the configuration files.";
        } else if(locale.equals("locale_fr")) {
            if(key.equals("global.reason"))       return "&c%reason%";
            if(key.equals("global.separator"))    return " &7: ";
            if(key.equals("global.punctuation"))  return "&7.";
            if(key.equals("global.usage"))        return "&fSyntaxe : ";
            if(key.equals("global.description"))  return "&fDescription : ";
            if(key.equals("global.prefix"))       return "&f[BackyardBan]";
            if(key.equals("global.version"))      return version;
            if(key.equals("global.days"))         return "j";
            if(key.equals("global.hours"))        return "h";
            if(key.equals("global.minutes"))      return "m";
            if(key.equals("global.seconds"))      return "s";

            if(key.equals("ban.banned"))          return "&7Vous avez été banni par &f%sender%";
            if(key.equals("ban.until"))           return "&7(Il reste &f%timeleft%&7)";
            if(key.equals("ban.confirm"))         return "&7Vous avez banni &f%player%";
            if(key.equals("ban.info"))            return "&f%player% &7a été banni par &f%sender%";
            if(key.equals("ban.unknown"))         return "&cErreur : &4%player%&c est inconnu.";
            if(key.equals("ban.bypass"))          return "&7Vous ne pouvez pas bannir &f%player%&7.";
            if(key.equals("ban.bypass_warn"))     return "&f%sender% &7a essayé de vous bannir.";
            if(key.equals("ban.usage"))           return "&3/ban &b[joueur] &et:(1h|1d|1m|1y) &b(raison)";
            if(key.equals("ban.description"))     return "&7Bannir un joueur avec un message.";
            if(key.equals("ban.yourself"))        return "&7Vous ne pouvez pas vour bannir vous-même.";

            if(key.equals("unban.confirm"))       return "&7Vous avez débanni &f%player%&7.";
            if(key.equals("unban.info"))          return "&f%player% &7a été débanni par &f%sender%&7.";
            if(key.equals("unban.usage"))         return "&3/unban &b[joueur]";
            if(key.equals("unban.description"))   return "&7Débannir un joueur.";
            if(key.equals("unban.yourself"))      return "&7Vous ne pouvez pas vous débannir vous-même.";
            if(key.equals("unban.notfound"))      return "&f%player% &7n'est pas banni.";

            if(key.equals("banip.banned"))        return "&7Votre IP a été bannie par &f%sender%";
            if(key.equals("banip.until"))         return "&7(Il reste &f%timeleft%&7)";
            if(key.equals("banip.confirm"))       return "&7Vous avez banni &f%ip%";
            if(key.equals("banip.info"))          return "&f%ip% &7a été banni par &f%sender%";
            if(key.equals("banip.bypass"))        return "&7Vous ne pouvez pas bannir l'ip &f%ip%&7.";
            if(key.equals("banip.bypass_warn"))   return "&f%sender% &7a essayé de bannir votre IP.";
            if(key.equals("banip.usage"))         return "&3/banip &b[ip] &et:(1h|1d|1m|1y) &b(raison)";
            if(key.equals("banip.description"))   return "&7Bannir une IP avec un message.";
            if(key.equals("banip.yourself"))      return "&7Vous ne pouvez pas bannir votre propre IP.";

            if(key.equals("help.usage"))          return "&3/backyardban:help";
            if(key.equals("help.description"))    return "&7Afficher la page d'aide.";

            if(key.equals("version.usage"))       return "&3/backyardban:version";
            if(key.equals("version.description")) return "&7Afficher la version du plugin.";

            if(key.equals("reload.success"))      return "&7Fichiers de config et de langue rechargés.";
            if(key.equals("reload.usage"))        return "&3/backyardban:reload";
            if(key.equals("reload.description"))  return "&7Recharger les fichiers de configuration.";
        }
        return "";
    }

    public Configuration getConfig(String fileName) throws IOException {
        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), fileName+".yml"));
    }

    public void saveConfig(Configuration config, String fileName) throws IOException {
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(getDataFolder(), fileName+".yml"));
    }
}
