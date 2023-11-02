package fr.stillcraft.backyardban.commands;

import fr.stillcraft.backyardban.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class help extends Command {
    public help() { super("backyardban:help","backyardban.ban"); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean sender_isplayer = (sender instanceof ProxiedPlayer);
        boolean has_unban_perm = (!sender_isplayer || sender.hasPermission("backyardban.unban"));
        boolean has_banip_perm = (!sender_isplayer || sender.hasPermission("backyardban.banip"));
        boolean has_reload_perm = (!sender_isplayer || sender.hasPermission("backyardban.reload"));

        // Get each string from config and locale data
        String global_prefix = Main.locale.getString("global.prefix");
        String help_usage = Main.locale.getString("help.usage");
        String help_description = Main.locale.getString("help.description");
        String ban_usage = Main.locale.getString("ban.usage");
        String ban_description = Main.locale.getString("ban.description");
        String banip_usage = Main.locale.getString("banip.usage");
        String banip_description = Main.locale.getString("banip.description");
        String unban_usage = Main.locale.getString("unban.usage");
        String unban_description = Main.locale.getString("unban.description");
        String reload_usage = Main.locale.getString("reload.usage");
        String reload_description = Main.locale.getString("reload.description");
        String version_usage = Main.locale.getString("version.usage");
        String version_description = Main.locale.getString("version.description");

        // Colorize each string
        global_prefix = ChatColor.translateAlternateColorCodes('&', global_prefix);
        help_usage = ChatColor.translateAlternateColorCodes('&', help_usage);
        help_description = ChatColor.translateAlternateColorCodes('&', help_description);
        ban_usage = ChatColor.translateAlternateColorCodes('&', ban_usage);
        ban_description = ChatColor.translateAlternateColorCodes('&', ban_description);
        banip_usage = ChatColor.translateAlternateColorCodes('&', banip_usage);
        banip_description = ChatColor.translateAlternateColorCodes('&', banip_description);
        unban_usage = ChatColor.translateAlternateColorCodes('&', unban_usage);
        unban_description = ChatColor.translateAlternateColorCodes('&', unban_description);
        reload_usage = ChatColor.translateAlternateColorCodes('&', reload_usage);
        reload_description = ChatColor.translateAlternateColorCodes('&', reload_description);
        version_usage = ChatColor.translateAlternateColorCodes('&', version_usage);
        version_description = ChatColor.translateAlternateColorCodes('&', version_description);

        sender.sendMessage(new TextComponent(ChatColor.WHITE+"--- "+global_prefix+ChatColor.WHITE+" ---"));
        sender.sendMessage(new TextComponent(ban_usage+ChatColor.WHITE+" - "+ban_description));
        if (has_banip_perm) sender.sendMessage(new TextComponent(banip_usage+ChatColor.WHITE+" - "+banip_description));
        if (has_unban_perm) sender.sendMessage(new TextComponent(unban_usage+ChatColor.WHITE+" - "+unban_description));
        sender.sendMessage(new TextComponent(help_usage+ChatColor.WHITE+" - "+help_description));
        if (has_reload_perm) sender.sendMessage(new TextComponent(reload_usage+ChatColor.WHITE+" - "+reload_description));
        sender.sendMessage(new TextComponent(version_usage+ChatColor.WHITE+" - "+version_description));
    }
}