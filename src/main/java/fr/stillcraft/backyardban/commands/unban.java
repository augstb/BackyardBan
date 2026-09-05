package fr.stillcraft.backyardban.commands;

import com.google.common.collect.ImmutableSet;

import fr.stillcraft.backyardban.Main;
import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.IpUtil;
import fr.stillcraft.backyardban.core.MessageFormatter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class unban extends Command implements TabExecutor {
    public unban() { super("backyardban:unban","backyardban.unban", "unban", "pardon"); }

    public void execute_unban(UUID player_uuid, String player_name, CommandSender sender){
        // Get each string from config and locale data
        boolean broadcast = Main.cfg.cfgBool("broadcast");

        // Parse placeholders
        String confirm = Main.cfg.msg("unban.confirm");
        String info = Main.cfg.msg("unban.info");
        confirm = ChatColor.translateAlternateColorCodes('&', confirm);
        info = ChatColor.translateAlternateColorCodes('&', info);
        confirm = MessageFormatter.replacePlaceholders(confirm, sender.getName(), player_name, null, null, null);
        info = MessageFormatter.replacePlaceholders(info, sender.getName(), player_name, null, null, null);

        // EXECUTE UNBAN
        BanService.clearBan(Main.cfg.banlist, player_uuid.toString());
        try {
            Main.cfg.saveConfig(Main.cfg.banlist, "data/banlist");
        } catch (java.io.IOException e) {
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
        boolean broadcast = Main.cfg.cfgBool("broadcast");

        // Parse placeholders
        String ipconfirm = Main.cfg.msg("unban.ipconfirm");
        String ipinfo = Main.cfg.msg("unban.ipinfo");
        ipconfirm = ChatColor.translateAlternateColorCodes('&', ipconfirm);
        ipinfo = ChatColor.translateAlternateColorCodes('&', ipinfo);
        ipconfirm = MessageFormatter.replacePlaceholders(ipconfirm, sender.getName(), null, tmp_player_ip, null, null);
        ipinfo = MessageFormatter.replacePlaceholders(ipinfo, sender.getName(), null, tmp_player_ip, null, null);

        // EXECUTE UNBAN
        BanService.clearBan(Main.cfg.baniplist, key);
        try {
            Main.cfg.saveConfig(Main.cfg.baniplist, "data/baniplist");
        } catch (java.io.IOException e) {
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
        String yourself = Main.cfg.msg("unban.yourself");
        String usage = Main.cfg.msg("global.usage")+Main.cfg.msg("unban.usage");
        String description = Main.cfg.msg("global.description")+Main.cfg.msg("unban.description");
        String notfound = Main.cfg.msg("unban.notfound");
        String ipnotfound = Main.cfg.msg("unban.ipnotfound");
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
            if (IpUtil.isIPv4(args[0]) || IpUtil.isIPv6(args[0])) {
                ip_tounban = args[0];
                arg_is_ip = true;
            }
            else player_name = args[0];

            String tmp_player_ip = "";
            if (arg_is_ip) {
                // only unban ip.
                for (String key: Main.cfg.baniplist.getKeys()) {
                    tmp_player_ip = BanService.keyToIp(key);
                    if (ip_tounban.equalsIgnoreCase(tmp_player_ip)) {
                        long ip_until = Main.cfg.baniplist.getLong(key+".until");
                        if (ip_tounban.equalsIgnoreCase(sender_ip)){
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
                for (String key: Main.cfg.banlist.getKeys()) {
                    if (args[0].equalsIgnoreCase(Main.cfg.banlist.getString(key+".player"))) {
                        player_uuid = UUID.fromString(key);
                        player_name = Main.cfg.banlist.getString(key+".player");
                        long until = Main.cfg.banlist.getLong(key+".until");
                        if (player_uuid.equals(sender_uuid)) {
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
                for (String key: Main.cfg.knownplayers.getKeys()) {
                    if (args[0].equalsIgnoreCase(Main.cfg.knownplayers.getString(key+".player"))) {
                        player_uuid = UUID.fromString(key);
                        player_name = Main.cfg.knownplayers.getString(key+".player");
                        if (player_uuid.equals(sender_uuid)) {
                            // Deny players from unbanning themselves
                            sender.sendMessage(new TextComponent(yourself));
                        } else {
                            // Also unban last known IP address if it is banned.
                            String player_ip = Main.cfg.knownplayers.getString(key+".ip");
                            String ip_key = BanService.ipToKey(player_ip);
                            if (Main.cfg.baniplist.getKeys().contains(ip_key)){
                                long ip_until = Main.cfg.baniplist.getLong(ip_key+".until");
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
                ipnotfound = MessageFormatter.replacePlaceholders(ipnotfound, null, null, args[0], null, null);
                sender.sendMessage(new TextComponent(ipnotfound));
            }
            if (!arg_is_ip && !player_found) {
                // Send message to sender if player is not banned.
                notfound = MessageFormatter.replacePlaceholders(notfound, null, args[0], null, null, null);
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
