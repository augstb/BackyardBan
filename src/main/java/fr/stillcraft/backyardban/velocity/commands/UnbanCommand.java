package fr.stillcraft.backyardban.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.stillcraft.backyardban.core.BackyardBanConfig;
import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.IpUtil;
import fr.stillcraft.backyardban.core.MessageFormatter;
import fr.stillcraft.backyardban.velocity.VelocityMain;
import fr.stillcraft.backyardban.velocity.VelocityText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UnbanCommand implements SimpleCommand {
    private final VelocityMain plugin;

    public UnbanCommand(VelocityMain plugin) { this.plugin = plugin; }

    public void executeUnban(UUID player_uuid, String player_name, CommandSource sender) {
        BackyardBanConfig cfg = plugin.cfg;
        boolean broadcast = cfg.cfgBool("broadcast");
        String senderName = VelocityText.senderName(sender);

        String confirm = MessageFormatter.replacePlaceholders(cfg.msg("unban.confirm"), senderName, player_name, null, null, null);
        String info = MessageFormatter.replacePlaceholders(cfg.msg("unban.info"), senderName, player_name, null, null, null);

        // EXECUTE UNBAN
        BanService.clearBan(cfg.banlist, player_uuid.toString());
        try {
            cfg.saveConfig(cfg.banlist, "data/banlist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plugin.getLogger().info(info);
        if (broadcast) {
            for (Player pp : plugin.getServer().getAllPlayers()) {
                VelocityText.send(pp, info);
            }
        } else {
            VelocityText.send(sender, confirm);
        }
    }

    public void executeUnbanip(String key, String tmp_player_ip, CommandSource sender) {
        BackyardBanConfig cfg = plugin.cfg;
        boolean broadcast = cfg.cfgBool("broadcast");
        String senderName = VelocityText.senderName(sender);

        String ipconfirm = MessageFormatter.replacePlaceholders(cfg.msg("unban.ipconfirm"), senderName, null, tmp_player_ip, null, null);
        String ipinfo = MessageFormatter.replacePlaceholders(cfg.msg("unban.ipinfo"), senderName, null, tmp_player_ip, null, null);

        // EXECUTE UNBAN
        BanService.clearBan(cfg.baniplist, key);
        try {
            cfg.saveConfig(cfg.baniplist, "data/baniplist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plugin.getLogger().info(ipinfo);
        if (broadcast) {
            for (Player pp : plugin.getServer().getAllPlayers()) {
                VelocityText.send(pp, ipinfo);
            }
        } else {
            VelocityText.send(sender, ipconfirm);
        }
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        BackyardBanConfig cfg = plugin.cfg;

        String yourself = cfg.msg("unban.yourself");
        String usage = cfg.msg("global.usage")+cfg.msg("unban.usage");
        String description = cfg.msg("global.description")+cfg.msg("unban.description");
        String notfound = cfg.msg("unban.notfound");
        String ipnotfound = cfg.msg("unban.ipnotfound");

        if (args.length > 0) {
            // Return help message | /!\ Problem if player is named "help".
            if (args[0].equalsIgnoreCase("help")) {
                VelocityText.send(sender, usage);
                VelocityText.send(sender, description);
                return;
            }

            // Parse placeholders
            UUID sender_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            String sender_ip = "";
            if (sender instanceof Player) {
                Player pSender = (Player) sender;
                sender_uuid = pSender.getUniqueId();
                sender_ip = pSender.getRemoteAddress().getAddress().getHostAddress();
            }

            // Analyse argument. Is a playername or an IP address ?
            String ip_tounban = "";
            String player_name = "";
            boolean arg_is_ip = false;
            boolean player_found = false;
            boolean ip_found = false;
            if (IpUtil.isIPv4(args[0]) || IpUtil.isIPv6(args[0])) {
                ip_tounban = args[0];
                arg_is_ip = true;
            }
            else player_name = args[0];

            String tmp_player_ip;
            if (arg_is_ip) {
                // only unban ip.
                for (String key : cfg.baniplist.getKeys()) {
                    tmp_player_ip = BanService.keyToIp(key);
                    if (ip_tounban.equalsIgnoreCase(tmp_player_ip)) {
                        long ip_until = cfg.baniplist.getLong(key+".until");
                        if (ip_tounban.equalsIgnoreCase(sender_ip)) {
                            // Deny players from unbanning themselves
                            VelocityText.send(sender, yourself);
                        } else if ((ip_until > System.currentTimeMillis() / 1000L) || ip_until < 0) {
                            // Unban this IP
                            ip_found = true;
                            executeUnbanip(key, tmp_player_ip, sender);
                        }
                    }
                }
            } else {
                // then unban a player
                for (String key : cfg.banlist.getKeys()) {
                    if (args[0].equalsIgnoreCase(cfg.banlist.getString(key+".player"))) {
                        UUID player_uuid = UUID.fromString(key);
                        player_name = cfg.banlist.getString(key+".player");
                        long until = cfg.banlist.getLong(key+".until");
                        if (player_uuid.equals(sender_uuid)) {
                            // Deny players from unbanning themselves
                            VelocityText.send(sender, yourself);
                        } else if ((until > System.currentTimeMillis() / 1000L) || until < 0) {
                            // Check if player is banned.
                            // Unban this UUID
                            player_found = true;
                            executeUnban(player_uuid, player_name, sender);
                        }
                    }
                }

                // then unban his last ip if banned
                for (String key : cfg.knownplayers.getKeys()) {
                    if (args[0].equalsIgnoreCase(cfg.knownplayers.getString(key+".player"))) {
                        UUID player_uuid = UUID.fromString(key);
                        if (player_uuid.equals(sender_uuid)) {
                            // Deny players from unbanning themselves
                            VelocityText.send(sender, yourself);
                        } else {
                            // Also unban last known IP address if it is banned.
                            String player_ip = cfg.knownplayers.getString(key+".ip");
                            String ip_key = BanService.ipToKey(player_ip);
                            if (cfg.baniplist.getKeys().contains(ip_key)) {
                                long ip_until = cfg.baniplist.getLong(ip_key+".until");
                                if ((ip_until > System.currentTimeMillis() / 1000L) || ip_until < 0) {
                                    // Unban this IP if found.
                                    player_found = true;
                                    executeUnbanip(ip_key, player_ip, sender);
                                }
                            }
                        }
                    }
                }
            }

            if (arg_is_ip && !ip_found) {
                VelocityText.send(sender, MessageFormatter.replacePlaceholders(ipnotfound, null, null, args[0], null, null));
            }
            if (!arg_is_ip && !player_found) {
                // Send message to sender if player is not banned.
                VelocityText.send(sender, MessageFormatter.replacePlaceholders(notfound, null, args[0], null, null, null));
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
        return invocation.source().hasPermission("backyardban.unban");
    }
}
