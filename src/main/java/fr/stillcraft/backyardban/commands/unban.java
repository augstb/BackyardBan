package fr.stillcraft.backyardban.commands;

import com.google.common.collect.ImmutableSet;

import fr.stillcraft.backyardban.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class unban extends Command implements TabExecutor {
    public unban() { super("backyardban:unban","backyardban.unban", "unban"); }

    public void execute_unban(UUID player_uuid, String player_name, CommandSender sender){
        // Get each string from config and locale data
        boolean broadcast = Main.config.getBoolean("broadcast");

        // Parse placeholders
        String confirm = Main.locale.getString("unban.confirm");
        String info = Main.locale.getString("unban.info");
        confirm = ChatColor.translateAlternateColorCodes('&', confirm);
        info = ChatColor.translateAlternateColorCodes('&', info);
        confirm = confirm.replaceAll("%sender%", sender.getName());
        confirm = confirm.replaceAll("%player%", player_name);
        info = info.replaceAll("%sender%", sender.getName());
        info = info.replaceAll("%player%", player_name);

        long endtime = System.currentTimeMillis() / 1000L;
        Date endtime_date = new Date(endtime * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(endtime_date);
        
        // EXECUTE UNBAN
        Main.banlist.set(player_uuid.toString()+".until", endtime);
        Main.banlist.set(player_uuid.toString()+".untildate", formattedDate);
        try {
            Main.getInstance().saveConfig(Main.banlist, "banlist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (broadcast) {
            for (ProxiedPlayer pp : Main.getInstance().getProxy().getPlayers()) {
                pp.sendMessage(new TextComponent(info));
            }
        } else {
            sender.sendMessage(new TextComponent(confirm));
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args){
        // Get each string from config and locale data
        String yourself = Main.locale.getString("unban.yourself");
        String usage = Main.locale.getString("global.usage")+Main.locale.getString("unban.usage");
        String description = Main.locale.getString("global.description")+Main.locale.getString("unban.description");
        String notfound = Main.locale.getString("unban.notfound");
        // Colorize each string
        yourself = ChatColor.translateAlternateColorCodes('&', yourself);
        usage = ChatColor.translateAlternateColorCodes('&', usage);
        description = ChatColor.translateAlternateColorCodes('&', description);
        notfound = ChatColor.translateAlternateColorCodes('&', notfound);
        
        if (args.length > 0) {
            // Parse placeholders
            notfound = notfound.replaceAll("%player%", args[0]);
            UUID player_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            UUID sender_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            String player_name = "";
            Boolean player_found = false;
            for (String key: Main.banlist.getKeys()) {
                if (args[0].equalsIgnoreCase(Main.banlist.getString(key+".player"))) {
                    player_found = true;
                    player_uuid = UUID.fromString(key);
                    player_name = Main.banlist.getString(key+".player");
                    if(sender instanceof ProxiedPlayer) {
                        sender_uuid = ((ProxiedPlayer) sender).getUniqueId();
                    }
                    if (player_uuid == sender_uuid) {
                        // Deny players from banning themselves
                        sender.sendMessage(new TextComponent(yourself));
                    }
                    else {
                        // Check if player is banned.
                        if (Main.banlist.getLong(key+".until") > System.currentTimeMillis() / 1000L) {
                            // Unban this UUID
                            execute_unban(player_uuid, player_name, sender);
                        }
                        else {
                            sender.sendMessage(new TextComponent(notfound));
                        }
                    }
                }
            }

            if (!player_found) {
                // Send message to sender if player is not banned.
                sender.sendMessage(new TextComponent(notfound));
            }
        } else {
            // Send usage and description message to sender
            sender.sendMessage(new TextComponent(usage));
            sender.sendMessage(new TextComponent(description));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length>1 || args.length==0){
            return ImmutableSet.of();
        }

        Set<String> matches = new HashSet<>();
        if (args.length == 1){
            String search = args[0].toLowerCase();
            for (ProxiedPlayer player: Main.getInstance().getProxy().getPlayers()){
                if (player.getName().toLowerCase().startsWith(search)){
                    matches.add(player.getName());
                }
            }
            if ("help".startsWith(search)) matches.add("help");
        }
        return matches;
    }
}
