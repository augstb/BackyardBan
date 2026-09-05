package fr.stillcraft.backyardban.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.stillcraft.backyardban.core.BackyardBanConfig;
import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.DurationParser;
import fr.stillcraft.backyardban.core.MessageFormatter;
import fr.stillcraft.backyardban.velocity.VelocityMain;
import fr.stillcraft.backyardban.velocity.VelocityText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BanCommand implements SimpleCommand {
    private final VelocityMain plugin;

    public BanCommand(VelocityMain plugin) { this.plugin = plugin; }

    public void executeBan(UUID player_uuid, String player_name, String player_ip, CommandSource sender, String[] args, Player player) {
        BackyardBanConfig cfg = plugin.cfg;

        // Get each string from config and locale data
        boolean broadcast = cfg.cfgBool("broadcast");
        String banned = cfg.msg("ban.banned");
        String until = cfg.msg("ban.until");
        String confirm = cfg.msg("ban.confirm");
        String reason = cfg.msg("global.reason");
        String separator = cfg.msg("global.separator");
        String punctuation = cfg.msg("global.punctuation");
        String info = cfg.msg("ban.info");
        String days = cfg.msg("global.days");
        String hours = cfg.msg("global.hours");
        String minutes = cfg.msg("global.minutes");
        String seconds = cfg.msg("global.seconds");

        // Parse the optional "t:<duration>" argument
        DurationParser.Result duration = DurationParser.parse(args, days, hours, minutes, seconds);
        if (duration.invalid) {
            VelocityText.send(sender, cfg.msg("global.usage")+cfg.msg("ban.usage"));
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
        banned = MessageFormatter.replacePlaceholders(banned, senderName, player_name, null, reason_string, duration.timeleftStr);
        confirm = MessageFormatter.replacePlaceholders(confirm, senderName, player_name, null, reason_string, duration.timeleftStr);
        info = MessageFormatter.replacePlaceholders(info, senderName, player_name, null, reason_string, duration.timeleftStr);

        // Register the ban in yaml file.
        long fromtime = System.currentTimeMillis() / 1000L;
        BanService.recordBan(cfg.banlist, player_uuid, player_name, senderName, fromtime, duration.endtime, reason_string, player_ip);
        try {
            cfg.saveConfig(cfg.banlist, "data/banlist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Execute actions (kicks player, and send messages)
        if (player != null) player.disconnect(VelocityText.of(banned));
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

        String usage = cfg.msg("global.usage")+cfg.msg("ban.usage");
        String description = cfg.msg("global.description")+cfg.msg("ban.description");
        String yourself = cfg.msg("ban.yourself");
        String bypass = cfg.msg("ban.bypass");
        String bypass_warn = cfg.msg("ban.bypass_warn");
        String unknown = cfg.msg("ban.unknown");

        if (args.length > 0) {
            // Return help message | /!\ Problem if player is named "help".
            if (args[0].equalsIgnoreCase("help")) {
                VelocityText.send(sender, usage);
                VelocityText.send(sender, description);
                return;
            }

            // Loop over players
            UUID player_uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID sender_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            if (sender instanceof Player) {
                sender_uuid = ((Player) sender).getUniqueId();
            }
            String player_name = "";
            String player_ip = "";
            boolean player_found = false;

            for (Player pplayer : plugin.getServer().getAllPlayers()) {
                if (args[0].equalsIgnoreCase(pplayer.getUsername())) {
                    player_found = true;
                    player_uuid = pplayer.getUniqueId();

                    if (player_uuid.equals(sender_uuid)) {
                        // Deny players from banning themselves
                        VelocityText.send(sender, yourself);
                    } else if (pplayer.hasPermission("backyardban.bypass")) {
                        // Deny to ban players that have bypass permission
                        String bypassMsg = MessageFormatter.replacePlaceholders(bypass, null, pplayer.getUsername(), null, null, null);
                        String bypassWarnMsg = MessageFormatter.replacePlaceholders(bypass_warn, VelocityText.senderName(sender), null, null, null, null);
                        VelocityText.send(sender, bypassMsg);
                        VelocityText.send(pplayer, bypassWarnMsg);
                    } else {
                        // Ban this UUID
                        player_name = pplayer.getUsername();
                        player_ip = pplayer.getRemoteAddress().getAddress().getHostAddress();
                        executeBan(player_uuid, player_name, player_ip, sender, args, pplayer);
                    }
                }
            }

            // If player is not online, then search in the database file
            if (!player_found) {
                for (String key : cfg.knownplayers.getKeys()) {
                    if (args[0].equalsIgnoreCase(cfg.knownplayers.getString(key+".player"))) {
                        player_found = true;
                        player_uuid = UUID.fromString(key);
                        player_name = cfg.knownplayers.getString(key+".player");
                        player_ip = cfg.knownplayers.getString(key+".ip");
                        // Check if player has bypass from knownplayers file
                        if (cfg.knownplayers.getBoolean(player_uuid.toString()+".bypass")) {
                            String bypassMsg = MessageFormatter.replacePlaceholders(bypass, null, player_name, null, null, null);
                            VelocityText.send(sender, bypassMsg);
                        } else {
                            // EXECUTE BAN
                            executeBan(player_uuid, player_name, player_ip, sender, args, null);
                        }
                        break; // Match found, stop scanning for duplicate entries.
                    }
                }
            }

            if (!player_found) {
                // Send message to sender if no player has been banned.
                String unknownMsg = MessageFormatter.replacePlaceholders(unknown, VelocityText.senderName(sender), args[0], null, null, null);
                VelocityText.send(sender, unknownMsg);
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
        return invocation.source().hasPermission("backyardban.ban");
    }
}
