package fr.stillcraft.backyardban.commands;

import com.google.common.collect.ImmutableSet;

import fr.stillcraft.backyardban.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class reload extends Command implements TabExecutor {
    public reload() { super("backyardban:reload","backyardban.reload"); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length > 0) {
            // Get each string from config, locale data and banlists
            String usage = Main.locale.getString("global.usage")+Main.locale.getString("reload.usage");
            String description = Main.locale.getString("global.description")+Main.locale.getString("reload.description");

            // Colorize each string
            usage = ChatColor.translateAlternateColorCodes('&', usage);
            description = ChatColor.translateAlternateColorCodes('&', description);

            // Return help message
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(new TextComponent(usage));
                sender.sendMessage(new TextComponent(description));
                return;
            }
        }

        Main.checkConfig("config");
        Main.checkConfig("locales/locale_fr");
        Main.checkConfig("locales/locale_en");
        Main.checkConfig("data/banlist");
        Main.checkConfig("data/baniplist");
        Main.checkConfig("data/knownplayers");
        try {
            // Reload config file
            Main.config = Main.getInstance().getConfig("config");
            String locale_string = Main.config.getString("locale");
            Main.locale = Main.getInstance().getConfig("locales/locale_" + locale_string);
            Main.banlist = Main.getInstance().getConfig("data/banlist");
            Main.baniplist = Main.getInstance().getConfig("data/baniplist");
            Main.knownplayers = Main.getInstance().getConfig("data/knownplayers");

            String success = Main.locale.getString("global.prefix")+" "+Main.locale.getString("reload.success");
            success = ChatColor.translateAlternateColorCodes('&', success);

            sender.sendMessage(new TextComponent(success));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<String> onTabComplete(CommandSender sender, String[] args){
        if (args.length>1 || args.length==0){
            return ImmutableSet.of();
        }

        Set<String> matches = new HashSet<>();
        if (args.length == 1){
            String search = args[0].toLowerCase();
            if ("help".startsWith(search)) matches.add("help");
        }
        return matches;
    }
}
