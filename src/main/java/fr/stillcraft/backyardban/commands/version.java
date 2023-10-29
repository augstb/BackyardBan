package fr.stillcraft.backyardban.commands;

import fr.stillcraft.backyardban.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class version extends Command {
    public version() { super("backyardban:version","backyardban.ban","backyardban:info"); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Get each string from config and locale data
        String global_prefix = Main.locale.getString("global.prefix");

        // Colorize each string
        global_prefix = ChatColor.translateAlternateColorCodes('&', global_prefix);

        sender.sendMessage(new TextComponent(ChatColor.WHITE+"--- "+global_prefix+ChatColor.WHITE+" ---"));
        sender.sendMessage(new TextComponent(ChatColor.WHITE+"BackyardBan: "+ChatColor.AQUA+"v"+Main.version));
        sender.sendMessage(new TextComponent(ChatColor.WHITE+"Source: "+ChatColor.AQUA+ChatColor.ITALIC+"github.com/augstb/BackyardBan"));
        sender.sendMessage(new TextComponent(ChatColor.WHITE+"Dev: "+ChatColor.AQUA+"Augustin Blanchet"));
    }

}
