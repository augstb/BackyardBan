package fr.stillcraft.backyardban.listener;

import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.DurationParser;
import fr.stillcraft.backyardban.core.MessageFormatter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import fr.stillcraft.backyardban.Main;

public class loginlistener implements Listener {

    // HIGHEST runs last among LoginEvent listeners, so no other plugin's handler
    // (e.g. an auth plugin like MaxAuth) can flip isCancelled() back after we deny a banned login.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent event) {
        String player = event.getConnection().getName();
        UUID player_uuid = event.getConnection().getUniqueId();
        String player_ip = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().getHostAddress();
        String ip_key = BanService.ipToKey(player_ip);

        // Add player to knownplayers database file, or update it.
        long timestamp = System.currentTimeMillis() / 1000L;
        Main.cfg.knownplayers.set(player_uuid.toString()+".player", player);
        Main.cfg.knownplayers.set(player_uuid.toString()+".ip", player_ip);
        Main.cfg.knownplayers.set(player_uuid.toString()+".seen", timestamp);
        Main.cfg.knownplayers.set(player_uuid.toString()+".seendate", BanService.formatDate(timestamp));
        try {
            Main.cfg.saveConfig(Main.cfg.knownplayers, "data/knownplayers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Check if player has bypass from knownplayers file
        if (Main.cfg.knownplayers.getBoolean(player_uuid.toString()+".bypass")) return;

        // Check if player (or their IP) is banned. Player ban takes priority for the displayed reason/banisher.
        BanService.BanStatus status = BanService.checkBan(Main.cfg.banlist, player_uuid.toString(), "CONSOLE");
        if (!status.banned) {
            status = BanService.checkBan(Main.cfg.baniplist, ip_key, "CONSOLE");
        }

        if (status.banned) {
            // Get each string from config and locale data
            String banned = Main.cfg.msg("ban.banned");
            String until = Main.cfg.msg("ban.until");
            String reason = Main.cfg.msg("global.reason");
            String separator = Main.cfg.msg("global.separator");
            String punctuation = Main.cfg.msg("global.punctuation");
            String days = Main.cfg.msg("global.days");
            String hours = Main.cfg.msg("global.hours");
            String minutes = Main.cfg.msg("global.minutes");
            String seconds = Main.cfg.msg("global.seconds");

            String timeleft_str = status.timeleft > 0 ? DurationParser.humanReadable(status.timeleft, days, hours, minutes, seconds) : "";

            String[] withUntil = MessageFormatter.appendUntilIfPresent(new String[]{banned}, until, status.timeleft > 0);
            String[] withReason = MessageFormatter.appendReasonOrPunctuation(withUntil, status.reason, reason, separator, punctuation);
            banned = withReason[0];

            // Colorize
            banned = ChatColor.translateAlternateColorCodes('&', banned);

            // Parse placeholders
            banned = MessageFormatter.replacePlaceholders(banned, status.banisher, player, null, status.reason, timeleft_str);

            // Execute actions (deny the login with the ban message)
            event.setCancelled(true);
            event.setCancelReason(new TextComponent(banned));
        }

    }

    @EventHandler
    public void onLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID player_uuid = player.getUniqueId();
        // If player has bypass, then remember it because we do not have access to permissions at pre login event)
        Main.cfg.knownplayers.set(player_uuid.toString()+".bypass", player.hasPermission("backyardban.bypass"));
        try {
            Main.cfg.saveConfig(Main.cfg.knownplayers, "data/knownplayers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
