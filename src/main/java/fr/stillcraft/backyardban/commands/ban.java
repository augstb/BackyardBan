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
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class ban extends Command implements TabExecutor {
    public ban() { super("backyardban:ban","backyardban.ban", "ban"); }

    public void execute_ban(UUID player_uuid, String player_name, String player_ip, CommandSender sender, String[] args, ProxiedPlayer player){
        // Get each string from config and locale data
        boolean broadcast = Main.config.getBoolean("broadcast");
        String banned = Main.locale.getString("ban.banned");
        String until = Main.locale.getString("ban.until");
        String confirm = Main.locale.getString("ban.confirm");
        String reason = Main.locale.getString("global.reason");
        String separator = Main.locale.getString("global.separator");
        String punctuation = Main.locale.getString("global.punctuation");
        String info = Main.locale.getString("ban.info");
        String days = Main.locale.getString("global.days");
        String hours = Main.locale.getString("global.hours");
        String minutes = Main.locale.getString("global.minutes");
        String seconds = Main.locale.getString("global.seconds");

        // Colorize each string
        banned = ChatColor.translateAlternateColorCodes('&', banned);
        until = ChatColor.translateAlternateColorCodes('&', until);
        confirm = ChatColor.translateAlternateColorCodes('&', confirm);
        reason = ChatColor.translateAlternateColorCodes('&', reason);
        separator = ChatColor.translateAlternateColorCodes('&', separator);
        punctuation = ChatColor.translateAlternateColorCodes('&', punctuation);
        info = ChatColor.translateAlternateColorCodes('&', info);
        days = ChatColor.translateAlternateColorCodes('&', days);
        hours = ChatColor.translateAlternateColorCodes('&', hours);
        minutes = ChatColor.translateAlternateColorCodes('&', minutes);
        seconds = ChatColor.translateAlternateColorCodes('&', seconds);

        // Construct complete ban strings
        StringBuilder stringBuilder = new StringBuilder();

        // Check if there is an end to that ban.
        int reason_strstart = 1;
        long timeleft = -1;
        long endtime = -1;
        String timeleft_str = "";
        if (args.length > 1) {
            if (args[1].startsWith("t:")) {
                reason_strstart = 2;
                String[] timeleft_str_parts = args[1].split(":");
                if (timeleft_str_parts.length > 1) {
                    String timeleft_str_tmp = timeleft_str_parts[1];
                    // Parse and convert time format to seconds from s | min | h | d | m | y
                    endtime = System.currentTimeMillis() / 1000L;
                    if (timeleft_str_tmp.endsWith("s")) {
                        String[] timeleft_parts = timeleft_str_tmp.split("s");
                        if (timeleft_parts.length > 0) timeleft = Integer.parseInt(timeleft_parts[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("min")) {
                        String[] timeleft_parts = timeleft_str_tmp.split("min");
                        if (timeleft_parts.length > 0) timeleft = 60*Integer.parseInt(timeleft_parts[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("h")) {
                        String[] timeleft_parts = timeleft_str_tmp.split("h");
                        if (timeleft_parts.length > 0) timeleft = 3600*Integer.parseInt(timeleft_parts[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("d")) {
                        String[] timeleft_parts = timeleft_str_tmp.split("d");
                        if (timeleft_parts.length > 0) timeleft = 86400*Integer.parseInt(timeleft_parts[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("m")) {
                        String[] timeleft_parts = timeleft_str_tmp.split("m");
                        if (timeleft_parts.length > 0) timeleft = 2592000*Integer.parseInt(timeleft_parts[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("y")) {
                        String[] timeleft_parts = timeleft_str_tmp.split("y");
                        if (timeleft_parts.length > 0) timeleft = 31104000*Integer.parseInt(timeleft_parts[0]);
                    }
                    if (timeleft > 0) {
                        endtime += timeleft;
                        Duration d = Duration.ofSeconds(timeleft);
                        long int_days = d.toDays();
                        d = d.minusDays(int_days);
                        long int_hours = d.toHours();
                        d = d.minusHours(int_hours);
                        long int_minutes = d.toMinutes();
                        d = d.minusMinutes(int_minutes);
                        long int_seconds = d.getSeconds();
                        if (int_days > 0) timeleft_str += Long.toString(int_days) + days;
                        else if (int_hours > 0) timeleft_str += Long.toString(int_hours) + hours;
                        else if (int_minutes > 0) timeleft_str += Long.toString(int_minutes) + minutes;
                        else if (int_seconds > 0) timeleft_str += Long.toString(int_seconds) + seconds;
                    }
                    else endtime = -1;
                } else {
                    // Handle the case where there is no colon in the string
                    // You can set a default value, display an error message, or take appropriate action.
                }
            }
        }

        for (String arg : Arrays.copyOfRange(args, reason_strstart, args.length)) {
            stringBuilder.append(arg).append(" ");
        }
        String reason_string = stringBuilder.toString();

        if (timeleft > 0) {
            banned += " " + until;
            confirm += " " + until;
            info += " " + until;
        }
        // Check if there is a reason or not.
        if (reason_string.trim().isEmpty()) {
            banned += punctuation;
            confirm += punctuation;
            info += punctuation;
        } else {
            reason_string = reason_string.substring(0, reason_string.length()-1);
            banned += separator + reason;
            confirm += separator + reason;
            info += separator + reason;
        }

        // Parse placeholders
        banned = banned.replaceAll("%sender%", sender.getName());
        confirm = confirm.replaceAll("%sender%", sender.getName());
        info = info.replaceAll("%sender%", sender.getName());
        banned = banned.replaceAll("%reason%", reason_string);
        confirm = confirm.replaceAll("%reason%", reason_string);
        info = info.replaceAll("%reason%", reason_string);
        banned = banned.replaceAll("%player%", player_name);
        confirm = confirm.replaceAll("%player%", player_name);
        info = info.replaceAll("%player%", player_name);
        banned = banned.replaceAll("%timeleft%", timeleft_str);
        confirm = confirm.replaceAll("%timeleft%", timeleft_str);
        info = info.replaceAll("%timeleft%", timeleft_str);

        // Register the ban in yaml file.
        Date endtime_date = new Date(endtime * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(endtime_date);
        long fromtime = System.currentTimeMillis() / 1000L;
        String formattedfromDate = sdf.format(new Date(fromtime * 1000L));
        if (endtime < 0) formattedDate = "Forever";
        Main.banlist.set(player_uuid.toString()+".player", player_name);
        Main.banlist.set(player_uuid.toString()+".banisher", sender.getName());
        Main.banlist.set(player_uuid.toString()+".from", fromtime);
        Main.banlist.set(player_uuid.toString()+".until", endtime);
        Main.banlist.set(player_uuid.toString()+".fromdate", formattedfromDate);
        Main.banlist.set(player_uuid.toString()+".untildate", formattedDate);
        Main.banlist.set(player_uuid.toString()+".reason", reason_string);
        Main.banlist.set(player_uuid.toString()+".ip", player_ip);
        try {
            Main.getInstance().saveConfig(Main.banlist, "data/banlist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Execute actions (kicks player, and send messages)
        if (player != Main.getInstance().getProxy().getPlayer("")) player.disconnect(new TextComponent(banned));
        Main.getInstance().getLogger().log(Level.INFO, info);
        // Broadcast message to all players if broadcast true in config
        if (broadcast) {
            for (ProxiedPlayer pp : Main.getInstance().getProxy().getPlayers()) {
                pp.sendMessage(new TextComponent(info));
            }
        } else {
            sender.sendMessage(new TextComponent(confirm));
        }
        return;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String usage = Main.locale.getString("global.usage")+Main.locale.getString("ban.usage");
        String description = Main.locale.getString("global.description")+Main.locale.getString("ban.description");
        String yourself = Main.locale.getString("ban.yourself");
        String bypass = Main.locale.getString("ban.bypass");
        String bypass_warn = Main.locale.getString("ban.bypass_warn");
        String unknown = Main.locale.getString("ban.unknown");
        usage = ChatColor.translateAlternateColorCodes('&', usage);
        description = ChatColor.translateAlternateColorCodes('&', description);
        yourself = ChatColor.translateAlternateColorCodes('&', yourself);
        bypass = ChatColor.translateAlternateColorCodes('&', bypass);
        bypass_warn = ChatColor.translateAlternateColorCodes('&', bypass_warn);
        unknown = ChatColor.translateAlternateColorCodes('&', unknown);

        if (args.length > 0) {
            // Return help message | /!\ Problem if player is named "help".
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(new TextComponent(usage));
                sender.sendMessage(new TextComponent(description));
                return;
            }

            // Loop over players
            UUID player_uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID sender_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            if(sender instanceof ProxiedPlayer) {
                sender_uuid = ((ProxiedPlayer) sender).getUniqueId();
            }
            String player_name = "";
            String player_ip = "";
            Boolean player_found = false;
            ProxiedPlayer player = Main.getInstance().getProxy().getPlayer("");

            for (ProxiedPlayer pplayer : Main.getInstance().getProxy().getPlayers()) {
                if (args[0].equalsIgnoreCase(pplayer.getDisplayName())) {
                    player_found = true;
                    player_uuid = pplayer.getUniqueId();
                    
                    if (player_uuid == sender_uuid) {
                        // Deny players from banning themselves
                        sender.sendMessage(new TextComponent(yourself));
                    } else if (pplayer.hasPermission("backyardban.bypass")) {
                        // Deny to ban players that have bypass permission
                        bypass = bypass.replaceAll("%player%", pplayer.getDisplayName());
                        bypass_warn = bypass_warn.replaceAll("%sender%", sender.getName());
                        sender.sendMessage(new TextComponent(bypass));
                        pplayer.sendMessage(new TextComponent(bypass_warn));
                    } else {
                        // Ban this UUID
                        player_name = pplayer.getDisplayName();
                        player_ip = ((InetSocketAddress) pplayer.getSocketAddress()).getAddress().getHostAddress();
                        execute_ban(player_uuid, player_name, player_ip, sender, args, pplayer);
                    }
                }
            }
            
            // If player is not online, then search in the database file
            if (!player_found) {
                for (String key: Main.knownplayers.getKeys()) {
                    if (args[0].equalsIgnoreCase(Main.knownplayers.getString(key+".player"))) {
                        player_found = true;
                        player_uuid = UUID.fromString(key);
                        player_name = Main.knownplayers.getString(key+".player");
                        player_ip = Main.knownplayers.getString(key+".ip");
                        // Check if player has bypass from knownplayers file
                        if (Main.knownplayers.getBoolean(player_uuid.toString()+".bypass")) {
                            bypass = bypass.replaceAll("%player%", player_name);
                            bypass_warn = bypass_warn.replaceAll("%sender%", sender.getName());
                            sender.sendMessage(new TextComponent(bypass));
                        }
                        else{
                            // EXECUTE BAN
                            execute_ban(player_uuid, player_name, player_ip, sender, args, player);
                        }
                    }
                }
            }

            if (!player_found) {
                // Send message to sender if no player has been banned.
                unknown = unknown.replaceAll("%sender%", sender.getName());
                unknown = unknown.replaceAll("%player%", args[0]);
                sender.sendMessage(new TextComponent(unknown));
            }
        } else {
            // Send usage and description message to sender
            sender.sendMessage(new TextComponent(usage));
            sender.sendMessage(new TextComponent(description));
        }
    }

    public Iterable<String> onTabComplete(CommandSender sender, String[] args){
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
