package fr.stillcraft.backyardban.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.stillcraft.backyardban.core.BackyardBanConfig;
import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.DurationParser;
import fr.stillcraft.backyardban.core.IpUtil;
import fr.stillcraft.backyardban.core.MessageFormatter;
import fr.stillcraft.backyardban.velocity.VelocityMain;
import fr.stillcraft.backyardban.velocity.VelocityText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BanipCommand implements SimpleCommand {
    private final VelocityMain plugin;

    public BanipCommand(VelocityMain plugin) { this.plugin = plugin; }

    public void executeBanip(String player_ip, CommandSource sender, String[] args) {
        BackyardBanConfig cfg = plugin.cfg;

        // Get each string from config and locale data
        boolean broadcast = cfg.cfgBool("broadcast");
        String banned = cfg.msg("banip.banned");
        String until = cfg.msg("banip.until");
        String confirm = cfg.msg("banip.confirm");
        String reason = cfg.msg("global.reason");
        String separator = cfg.msg("global.separator");
        String punctuation = cfg.msg("global.punctuation");
        String info = cfg.msg("banip.info");
        String days = cfg.msg("global.days");
        String hours = cfg.msg("global.hours");
        String minutes = cfg.msg("global.minutes");
        String seconds = cfg.msg("global.seconds");

        // Parse the optional "t:<duration>" argument
        DurationParser.Result duration = DurationParser.parse(args, days, hours, minutes, seconds);
        if (duration.invalid) {
            VelocityText.send(sender, cfg.msg("global.usage")+cfg.msg("banip.usage"));
            return;
        }

        String reason_string = MessageFormatter.buildReason(args, duration.reasonStartIndex);
        String senderName = VelocityText.senderName(sender);

        String[] withUntil = MessageFormatter.appendUntilIfPresent(new String[]{banned, confirm, info}, until, duration.timeleft > 0);
        String[] withReason = MessageFormatter.appendReasonOrPunctuation(withUntil, reason_string, reason, separator, punctuation);
        banned = withReason[0];
        confirm = withReason[1];
        info = withReason[2];

        // Parse placeholders
        banned = MessageFormatter.replacePlaceholders(banned, senderName, null, player_ip, reason_string, duration.timeleftStr);
        confirm = MessageFormatter.replacePlaceholders(confirm, senderName, null, player_ip, reason_string, duration.timeleftStr);
        info = MessageFormatter.replacePlaceholders(info, senderName, null, player_ip, reason_string, duration.timeleftStr);

        // Register the ban in yaml file.
        long fromtime = System.currentTimeMillis() / 1000L;
        String ip_key = BanService.ipToKey(player_ip);
        BanService.recordIpBan(cfg.baniplist, ip_key, senderName, fromtime, duration.endtime, reason_string);
        try {
            cfg.saveConfig(cfg.baniplist, "data/baniplist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Execute actions (kicks players, and send messages)
        for (Player pp : plugin.getServer().getAllPlayers()) {
            if (player_ip.equalsIgnoreCase(pp.getRemoteAddress().getAddress().getHostAddress())) {
                pp.disconnect(VelocityText.of(banned));
            }
        }
        plugin.getLogger().info(info);
        // Broadcast message to all players if broadcast true in config
        if (broadcast) {
            for (Player pp : plugin.getServer().getAllPlayers()) {
                VelocityText.send(pp, info);
            }
        } else {
            VelocityText.send(sender, confirm);
        }
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        BackyardBanConfig cfg = plugin.cfg;

        String usage = cfg.msg("global.usage")+cfg.msg("banip.usage");
        String description = cfg.msg("global.description")+cfg.msg("banip.description");
        String yourself = cfg.msg("banip.yourself");
        String unknown = cfg.msg("banip.unknown");
        String bypass = cfg.msg("banip.bypass");
        String bypass_warn = cfg.msg("banip.bypass_warn");

        if (args.length > 0) {
            // Return help message | /!\ Problem if player is named "help".
            if (args[0].equalsIgnoreCase("help")) {
                VelocityText.send(sender, usage);
                VelocityText.send(sender, description);
                return;
            }

            // Analyse argument. Is a playername or an IP address ?
            String ip_toban = "";
            String player_name = "";
            boolean arg_is_ip = false;
            if (IpUtil.isIPv4(args[0]) || IpUtil.isIPv6(args[0])) {
                ip_toban = args[0];
                arg_is_ip = true;
            }
            else player_name = args[0];

            // Loop over players
            UUID sender_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            if (sender instanceof Player) {
                sender_uuid = ((Player) sender).getUniqueId();
            }
            boolean player_found = false;
            String tmp_player_ip;
            for (Player pplayer : plugin.getServer().getAllPlayers()) {
                tmp_player_ip = pplayer.getRemoteAddress().getAddress().getHostAddress();
                if (ip_toban.equalsIgnoreCase(tmp_player_ip) || player_name.equalsIgnoreCase(pplayer.getUsername())) {
                    player_found = true;
                    ip_toban = tmp_player_ip;
                    UUID player_uuid = pplayer.getUniqueId();
                    if (player_uuid.equals(sender_uuid)) {
                        // Deny players from banning themselves
                        VelocityText.send(sender, yourself);
                        return; // Force exiting
                    }
                    else if (pplayer.hasPermission("backyardban.bypass")) {
                        // Deny to banip players that have bypass permission
                        String bypassMsg = MessageFormatter.replacePlaceholders(bypass, null, null, tmp_player_ip, null, null);
                        String bypassWarnMsg = MessageFormatter.replacePlaceholders(bypass_warn, VelocityText.senderName(sender), null, null, null, null);
                        VelocityText.send(sender, bypassMsg);
                        VelocityText.send(pplayer, bypassWarnMsg);
                        return; // Force exiting
                    }
                }
            }

            // Also search in knownplayers list if IP is protected.
            for (String key : cfg.knownplayers.getKeys()) {
                if (ip_toban.equalsIgnoreCase(cfg.knownplayers.getString(key+".ip")) || player_name.equalsIgnoreCase(cfg.knownplayers.getString(key+".player"))) {
                    player_found = true;
                    ip_toban = cfg.knownplayers.getString(key+".ip");
                    // Check if player has bypass from knownplayers file
                    if (cfg.knownplayers.getBoolean(key+".bypass")) {
                        String bypassMsg = MessageFormatter.replacePlaceholders(bypass, null, null, ip_toban, null, null);
                        VelocityText.send(sender, bypassMsg);
                        return; // Force exiting
                    }
                }
            }
            // Redo the loop, assuming we know the ip of the player now.
            if (!arg_is_ip) {
                for (String key : cfg.knownplayers.getKeys()) {
                    if (ip_toban.equalsIgnoreCase(cfg.knownplayers.getString(key+".ip"))) {
                        // Check if player has bypass from knownplayers file
                        if (cfg.knownplayers.getBoolean(key+".bypass")) {
                            String bypassMsg = MessageFormatter.replacePlaceholders(bypass, null, null, ip_toban, null, null);
                            VelocityText.send(sender, bypassMsg);
                            return; // Force exiting
                        }
                    }
                }
            }

            // If the code runs until here, then the IP is safe to be banned.
            // If ip not valid then return
            if (arg_is_ip) {
                executeBanip(ip_toban, sender, args);
            } else {
                // then player is not found or ip is not valid.
                if (player_found) executeBanip(ip_toban, sender, args);
                else VelocityText.send(sender, unknown);
            }
        } else {
            // Send usage and description message to sender
            VelocityText.send(sender, usage);
            VelocityText.send(sender, description);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length > 1 || args.length == 0) {
            return Collections.emptyList();
        }

        List<String> matches = new ArrayList<>();
        String search = args[0].toLowerCase();
        for (Player player : plugin.getServer().getAllPlayers()) {
            if (player.getUsername().toLowerCase().startsWith(search)) {
                matches.add(player.getUsername());
            }
        }
        if ("help".startsWith(search)) matches.add("help");
        return matches;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("backyardban.banip");
    }
}
