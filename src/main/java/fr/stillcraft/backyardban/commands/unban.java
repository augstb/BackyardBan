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
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class unban extends Command implements TabExecutor {
    public unban() { super("backyardban:unban","backyardban.unban", "unban", "pardon"); }

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
            Main.getInstance().saveConfig(Main.banlist, "data/banlist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Main.getInstance().getLogger().log(Level.INFO, info);
        if (broadcast) {
            for (ProxiedPlayer pp : Main.getInstance().getProxy().getPlayers()) {
                pp.sendMessage(new TextComponent(info));
            }
        } else {
            sender.sendMessage(new TextComponent(confirm));
        }
    }

    public void execute_unbanip(String key, String tmp_player_ip, CommandSender sender){
        // Get each string from config and locale data
        boolean broadcast = Main.config.getBoolean("broadcast");

        // Parse placeholders
        String ipconfirm = Main.locale.getString("unban.ipconfirm");
        String ipinfo = Main.locale.getString("unban.ipinfo");
        ipconfirm = ChatColor.translateAlternateColorCodes('&', ipconfirm);
        ipinfo = ChatColor.translateAlternateColorCodes('&', ipinfo);
        ipconfirm = ipconfirm.replaceAll("%sender%", sender.getName());
        ipconfirm = ipconfirm.replaceAll("%ip%", tmp_player_ip);
        ipinfo = ipinfo.replaceAll("%sender%", sender.getName());
        ipinfo = ipinfo.replaceAll("%ip%", tmp_player_ip);

        long endtime = System.currentTimeMillis() / 1000L;
        Date endtime_date = new Date(endtime * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(endtime_date);
        
        // EXECUTE UNBAN
        Main.baniplist.set(key+".until", endtime);
        Main.baniplist.set(key+".untildate", formattedDate);
        try {
            Main.getInstance().saveConfig(Main.baniplist, "data/baniplist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Main.getInstance().getLogger().log(Level.INFO, ipinfo);
        if (broadcast) {
            for (ProxiedPlayer pp : Main.getInstance().getProxy().getPlayers()) {
                pp.sendMessage(new TextComponent(ipinfo));
            }
        } else {
            sender.sendMessage(new TextComponent(ipconfirm));
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args){
        // Get each string from config and locale data
        String yourself = Main.locale.getString("unban.yourself");
        String usage = Main.locale.getString("global.usage")+Main.locale.getString("unban.usage");
        String description = Main.locale.getString("global.description")+Main.locale.getString("unban.description");
        String notfound = Main.locale.getString("unban.notfound");
        String ipnotfound = Main.locale.getString("unban.ipnotfound");
        // Colorize each string
        yourself = ChatColor.translateAlternateColorCodes('&', yourself);
        usage = ChatColor.translateAlternateColorCodes('&', usage);
        description = ChatColor.translateAlternateColorCodes('&', description);
        notfound = ChatColor.translateAlternateColorCodes('&', notfound);
        ipnotfound = ChatColor.translateAlternateColorCodes('&', ipnotfound);

        if (args.length > 0) {
            // Return help message | /!\ Problem if player is named "help".
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(new TextComponent(usage));
                sender.sendMessage(new TextComponent(description));
                return;
            }

            // Parse placeholders
            UUID player_uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID sender_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            String sender_ip = "";
            if(sender instanceof ProxiedPlayer) {
                ProxiedPlayer pp_sender = ((ProxiedPlayer) sender);
                sender_uuid = pp_sender.getUniqueId();
                sender_ip = ((InetSocketAddress) pp_sender.getSocketAddress()).getAddress().getHostAddress();
            }

            // Analyse argument. Is a playername or an IP adress ?
            String ip_tounban = "";
            String player_name = "";
            Boolean arg_is_ip = false;
            Boolean player_found = false;
            Boolean ip_found = false;
            if (Main.isIPv4(args[0]) || Main.isIPv6(args[0])) {
                ip_tounban = args[0];
                arg_is_ip = true;
            }
            else player_name = args[0];

            String tmp_player_ip = "";
            if (arg_is_ip) {
                // only unban ip.
                for (String key: Main.baniplist.getKeys()) {
                    tmp_player_ip = key.replace("-",".").replace("_",":");
                    if (ip_tounban.equalsIgnoreCase(tmp_player_ip)) {
                        long ip_until = Main.baniplist.getLong(key+".until");
                        if (ip_tounban == sender_ip){
                            // Deny players from unbanning themselves
                            sender.sendMessage(new TextComponent(yourself));
                        } else if ((ip_until > System.currentTimeMillis() / 1000L) || ip_until < 0) {
                            // Unban this IP
                            ip_found = true;
                            execute_unbanip(key, tmp_player_ip, sender);
                        }
                    }
                }
            } else {
                // then unban a player
                for (String key: Main.banlist.getKeys()) {
                    if (args[0].equalsIgnoreCase(Main.banlist.getString(key+".player"))) {
                        player_uuid = UUID.fromString(key);
                        player_name = Main.banlist.getString(key+".player");
                        long until = Main.banlist.getLong(key+".until");
                        if (player_uuid == sender_uuid) {
                            // Deny players from unbanning themselves
                            sender.sendMessage(new TextComponent(yourself));
                        } else if ((until > System.currentTimeMillis() / 1000L) || until < 0) {
                            // Check if player is banned.
                            // Unban this UUID
                            player_found = true;
                            execute_unban(player_uuid, player_name, sender);
                        }
                    }
                }

                // then unban his last ip if banned
                for (String key: Main.knownplayers.getKeys()) {
                    if (args[0].equalsIgnoreCase(Main.knownplayers.getString(key+".player"))) {
                        player_uuid = UUID.fromString(key);
                        player_name = Main.knownplayers.getString(key+".player");
                        if (player_uuid == sender_uuid) {
                            // Deny players from unbanning themselves
                            sender.sendMessage(new TextComponent(yourself));
                        } else {
                            // Also unban last known IP adress if it is banned.
                            String player_ip = Main.knownplayers.getString(key+".ip");
                            String ip_key = player_ip.replace(".","-").replace(":","_");
                            if (Main.baniplist.getKeys().contains(ip_key)){
                                long ip_until = Main.baniplist.getLong(ip_key+".until");
                                if ((ip_until > System.currentTimeMillis() / 1000L) || ip_until < 0 ) {
                                    // Unban this IP if found.
                                    player_found = true;
                                    execute_unbanip(ip_key, player_ip, sender);
                                }
                            }
                        }
                    }
                }
            }
            
            if (arg_is_ip && !ip_found) {
                ipnotfound = ipnotfound.replaceAll("%ip%", args[0]);
                sender.sendMessage(new TextComponent(ipnotfound));
            }
            if (!arg_is_ip && !player_found) {
                // Send message to sender if player is not banned.
                notfound = notfound.replaceAll("%player%", args[0]);
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
