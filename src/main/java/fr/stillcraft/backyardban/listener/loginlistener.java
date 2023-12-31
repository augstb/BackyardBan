package fr.stillcraft.backyardban.listener;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import fr.stillcraft.backyardban.Main;

public class loginlistener implements Listener {

    @EventHandler
    public void onLogin(LoginEvent event) {
        boolean kick_player = false;
        long timeleft = -1;
        String reason_string = "";
        String banisher = "CONSOLE";
        String timeleft_str = "";
        String player = event.getConnection().getName();
        UUID player_uuid = event.getConnection().getUniqueId();
        String player_ip = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().getHostAddress();
        String ip_key = player_ip.replace(".","-").replace(":","_");

        // Add player to knownplayers database file, or update it.
        long timestamp = System.currentTimeMillis() / 1000L;
        Date timestamp_date = new Date(timestamp * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(timestamp_date);
        Main.knownplayers.set(player_uuid.toString()+".player", player);
        Main.knownplayers.set(player_uuid.toString()+".ip", player_ip);
        Main.knownplayers.set(player_uuid.toString()+".seen", timestamp);
        Main.knownplayers.set(player_uuid.toString()+".seendate", formattedDate);
        try {
            Main.getInstance().saveConfig(Main.knownplayers, "data/knownplayers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Check if player has bypass from knownplayers file
        if (Main.knownplayers.getBoolean(player_uuid.toString()+".bypass")) return;

        if (Main.banlist.getKeys().contains(player_uuid.toString())){
            // Check if player is banned
            long until = -1;
            if (Main.banlist.getSection(player_uuid.toString()).contains("until")){
                until = Main.banlist.getLong(player_uuid.toString()+".until");
                timeleft = until - System.currentTimeMillis() / 1000L;
            }
            if (Main.banlist.getSection(player_uuid.toString()).contains("banisher")){
                banisher = Main.banlist.getString(player_uuid.toString()+".banisher");
            }
            if (Main.banlist.getSection(player_uuid.toString()).contains("reason")){
                reason_string = Main.banlist.getString(player_uuid.toString()+".reason");
            }
            if (until < 0 || timeleft > 0) kick_player = true;
        }
        
        if (!kick_player && Main.baniplist.getKeys().contains(ip_key)){
            // Check if IP address is banned
            long until = -1;
            if (Main.baniplist.getSection(ip_key).contains("until")){
                until = Main.baniplist.getLong(ip_key+".until");
                timeleft = until - System.currentTimeMillis() / 1000L;
            }
            if (Main.baniplist.getSection(ip_key).contains("banisher")){
                banisher = Main.baniplist.getString(ip_key+".banisher");
            }
            if (Main.baniplist.getSection(ip_key).contains("reason")){
                reason_string = Main.baniplist.getString(ip_key+".reason");
            }
            if (until < 0 || timeleft > 0) kick_player = true;
        }

        if (kick_player) {
            // Get each string from config and locale data
            String banned = Main.locale.getString("ban.banned");
            String until = Main.locale.getString("ban.until");
            String reason = Main.locale.getString("global.reason");
            String separator = Main.locale.getString("global.separator");
            String punctuation = Main.locale.getString("global.punctuation");
            String days = Main.locale.getString("global.days");
            String hours = Main.locale.getString("global.hours");
            String minutes = Main.locale.getString("global.minutes");
            String seconds = Main.locale.getString("global.seconds");

            // Colorize each string
            banned = ChatColor.translateAlternateColorCodes('&', banned);
            until = ChatColor.translateAlternateColorCodes('&', until);
            reason = ChatColor.translateAlternateColorCodes('&', reason);
            separator = ChatColor.translateAlternateColorCodes('&', separator);
            punctuation = ChatColor.translateAlternateColorCodes('&', punctuation);
            days = ChatColor.translateAlternateColorCodes('&', days);
            hours = ChatColor.translateAlternateColorCodes('&', hours);
            minutes = ChatColor.translateAlternateColorCodes('&', minutes);
            seconds = ChatColor.translateAlternateColorCodes('&', seconds);

            if (timeleft > 0) {
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
                banned += " " + until;
            }
            if (reason_string.trim().isEmpty()) banned += punctuation;
            else banned += separator + reason;

            // Parse placeholders
            banned = banned.replaceAll("%sender%", banisher);
            banned = banned.replaceAll("%reason%", reason_string);
            banned = banned.replaceAll("%player%", player);
            banned = banned.replaceAll("%timeleft%", timeleft_str);

            // Execute actions (kicks player, and send messages)
            event.getConnection().disconnect(new TextComponent(banned));
        }

    }

    @EventHandler
    public void onLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID player_uuid = player.getUniqueId();
        // If player has bypass, then remember it because we do not have access to permissions at pre login event)
        if (player.hasPermission("backyardban.bypass")) {
            Main.knownplayers.set(player_uuid.toString()+".bypass", true);
        }
        else {
            Main.knownplayers.set(player_uuid.toString()+".bypass", false);
        }
        try {
            Main.getInstance().saveConfig(Main.knownplayers, "data/knownplayers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
