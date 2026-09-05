package fr.stillcraft.backyardban.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.DurationParser;
import fr.stillcraft.backyardban.core.MessageFormatter;
import fr.stillcraft.backyardban.velocity.VelocityMain;
import fr.stillcraft.backyardban.velocity.VelocityText;

import java.io.IOException;
import java.util.UUID;

public class VelocityLoginListener {
    private final VelocityMain plugin;

    public VelocityLoginListener(VelocityMain plugin) { this.plugin = plugin; }

    // LAST runs after every other plugin's LoginEvent listener (e.g. an auth plugin), so no one
    // else can flip the result back to allowed after we deny a banned login.
    @Subscribe(order = PostOrder.LAST)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String player_name = player.getUsername();
        UUID player_uuid = player.getUniqueId();
        String player_ip = player.getRemoteAddress().getAddress().getHostAddress();
        String ip_key = BanService.ipToKey(player_ip);

        // Add player to knownplayers database file, or update it.
        long timestamp = System.currentTimeMillis() / 1000L;
        plugin.cfg.knownplayers.set(player_uuid.toString()+".player", player_name);
        plugin.cfg.knownplayers.set(player_uuid.toString()+".ip", player_ip);
        plugin.cfg.knownplayers.set(player_uuid.toString()+".seen", timestamp);
        plugin.cfg.knownplayers.set(player_uuid.toString()+".seendate", BanService.formatDate(timestamp));
        try {
            plugin.cfg.saveConfig(plugin.cfg.knownplayers, "data/knownplayers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Check if player has bypass from knownplayers file
        if (plugin.cfg.knownplayers.getBoolean(player_uuid.toString()+".bypass")) return;

        // Check if player (or their IP) is banned. Player ban takes priority for the displayed reason/banisher.
        BanService.BanStatus status = BanService.checkBan(plugin.cfg.banlist, player_uuid.toString(), "CONSOLE");
        if (!status.banned) {
            status = BanService.checkBan(plugin.cfg.baniplist, ip_key, "CONSOLE");
        }

        if (status.banned) {
            // Get each string from config and locale data
            String banned = plugin.cfg.msg("ban.banned");
            String until = plugin.cfg.msg("ban.until");
            String reason = plugin.cfg.msg("global.reason");
            String separator = plugin.cfg.msg("global.separator");
            String punctuation = plugin.cfg.msg("global.punctuation");
            String days = plugin.cfg.msg("global.days");
            String hours = plugin.cfg.msg("global.hours");
            String minutes = plugin.cfg.msg("global.minutes");
            String seconds = plugin.cfg.msg("global.seconds");

            String timeleft_str = status.timeleft > 0 ? DurationParser.humanReadable(status.timeleft, days, hours, minutes, seconds) : "";

            String[] withUntil = MessageFormatter.appendUntilIfPresent(new String[]{banned}, until, status.timeleft > 0);
            String[] withReason = MessageFormatter.appendReasonOrPunctuation(withUntil, status.reason, reason, separator, punctuation);
            banned = withReason[0];

            // Parse placeholders
            banned = MessageFormatter.replacePlaceholders(banned, status.banisher, player_name, null, status.reason, timeleft_str);

            // Execute actions (deny the login with the ban message)
            event.setResult(ResultedEvent.ComponentResult.denied(VelocityText.of(banned)));
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID player_uuid = player.getUniqueId();
        // If player has bypass, then remember it because we do not have access to permissions at pre login event)
        plugin.cfg.knownplayers.set(player_uuid.toString()+".bypass", player.hasPermission("backyardban.bypass"));
        try {
            plugin.cfg.saveConfig(plugin.cfg.knownplayers, "data/knownplayers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
