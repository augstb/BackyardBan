package fr.stillcraft.backyardban.commands;

import com.google.common.collect.ImmutableSet;

import fr.stillcraft.backyardban.Main;
import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.DurationParser;
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

public class banip extends Command implements TabExecutor {
    public banip() { super("backyardban:banip","backyardban.banip", "banip"); }

    public void execute_banip(String player_ip, CommandSender sender, String[] args){
        // Get each string from config and locale data
        boolean broadcast = Main.cfg.cfgBool("broadcast");
        String banned = Main.cfg.msg("banip.banned");
        String until = Main.cfg.msg("banip.until");
        String confirm = Main.cfg.msg("banip.confirm");
        String reason = Main.cfg.msg("global.reason");
        String separator = Main.cfg.msg("global.separator");
        String punctuation = Main.cfg.msg("global.punctuation");
        String info = Main.cfg.msg("banip.info");
        String days = Main.cfg.msg("global.days");
        String hours = Main.cfg.msg("global.hours");
        String minutes = Main.cfg.msg("global.minutes");
        String seconds = Main.cfg.msg("global.seconds");

        // Parse the optional "t:<duration>" argument
        DurationParser.Result duration = DurationParser.parse(args, days, hours, minutes, seconds);
        if (duration.invalid) {
            String usage = ChatColor.translateAlternateColorCodes('&', Main.cfg.msg("global.usage")+Main.cfg.msg("banip.usage"));
            sender.sendMessage(new TextComponent(usage));
            return;
        }

        String reason_string = MessageFormatter.buildReason(args, duration.reasonStartIndex);

        String[] withUntil = MessageFormatter.appendUntilIfPresent(new String[]{banned, confirm, info}, until, duration.timeleft > 0);
        String[] withReason = MessageFormatter.appendReasonOrPunctuation(withUntil, reason_string, reason, separator, punctuation);
        banned = withReason[0];
        confirm = withReason[1];
        info = withReason[2];

        // Colorize each string
        banned = ChatColor.translateAlternateColorCodes('&', banned);
        confirm = ChatColor.translateAlternateColorCodes('&', confirm);
        info = ChatColor.translateAlternateColorCodes('&', info);

        // Parse placeholders
        banned = MessageFormatter.replacePlaceholders(banned, sender.getName(), null, player_ip, reason_string, duration.timeleftStr);
        confirm = MessageFormatter.replacePlaceholders(confirm, sender.getName(), null, player_ip, reason_string, duration.timeleftStr);
        info = MessageFormatter.replacePlaceholders(info, sender.getName(), null, player_ip, reason_string, duration.timeleftStr);

        // Register the ban in yaml file.
        long fromtime = System.currentTimeMillis() / 1000L;
        String ip_key = BanService.ipToKey(player_ip);
        BanService.recordIpBan(Main.cfg.baniplist, ip_key, sender.getName(), fromtime, duration.endtime, reason_string);
        try {
            Main.cfg.saveConfig(Main.cfg.baniplist, "data/baniplist");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        // Execute actions (kicks players, and send messages)
        for (ProxiedPlayer pp : Main.getInstance().getProxy().getPlayers()) {
            if (player_ip.equalsIgnoreCase(((InetSocketAddress) pp.getSocketAddress()).getAddress().getHostAddress())) {
                pp.disconnect(new TextComponent(banned));
            }
        }
        Main.getInstance().getLogger().log(Level.INFO, info);
        // Broadcast message to all players if broadcast true in config
        if (broadcast) {
            for (ProxiedPlayer pp : Main.getInstance().getProxy().getPlayers()) {
                pp.sendMessage(new TextComponent(info));
            }
        } else {
            sender.sendMessage(new TextComponent(confirm));
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String usage = Main.cfg.msg("global.usage")+Main.cfg.msg("banip.usage");
        String description = Main.cfg.msg("global.description")+Main.cfg.msg("banip.description");
        String yourself = Main.cfg.msg("banip.yourself");
        String unknown = Main.cfg.msg("banip.unknown");
        String bypass = Main.cfg.msg("banip.bypass");
        String bypass_warn = Main.cfg.msg("banip.bypass_warn");
        usage = ChatColor.translateAlternateColorCodes('&', usage);
        description = ChatColor.translateAlternateColorCodes('&', description);
        yourself = ChatColor.translateAlternateColorCodes('&', yourself);
        unknown = ChatColor.translateAlternateColorCodes('&', unknown);
        bypass = ChatColor.translateAlternateColorCodes('&', bypass);
        bypass_warn = ChatColor.translateAlternateColorCodes('&', bypass_warn);

        if (args.length > 0) {
            // Return help message | /!\ Problem if player is named "help".
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(new TextComponent(usage));
                sender.sendMessage(new TextComponent(description));
                return;
            }

            // Analyse argument. Is a playername or an IP address ?
            String ip_toban = "";
            String player_name = "";
            Boolean arg_is_ip = false;
            if (IpUtil.isIPv4(args[0]) || IpUtil.isIPv6(args[0])) {
                ip_toban = args[0];
                arg_is_ip = true;
            }
            else player_name = args[0];

            // Loop over players
            UUID player_uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID sender_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            if(sender instanceof ProxiedPlayer) {
                sender_uuid = ((ProxiedPlayer) sender).getUniqueId();
            }
            Boolean player_found = false;
            String tmp_player_ip = "";
            for (ProxiedPlayer pplayer : Main.getInstance().getProxy().getPlayers()) {
                tmp_player_ip = ((InetSocketAddress) pplayer.getSocketAddress()).getAddress().getHostAddress();
                if (ip_toban.equalsIgnoreCase(tmp_player_ip) || player_name.equalsIgnoreCase(pplayer.getName())) {
                    player_found = true;
                    ip_toban = tmp_player_ip;
                    player_uuid = pplayer.getUniqueId();
                    if (player_uuid.equals(sender_uuid)) {
                        // Deny players from banning themselves
                        sender.sendMessage(new TextComponent(yourself));
                        return; // Force exiting
                    }
                    else if (pplayer.hasPermission("backyardban.bypass")) {
                        // Deny to banip players that have bypass permission
                        bypass = MessageFormatter.replacePlaceholders(bypass, null, null, tmp_player_ip, null, null);
                        bypass_warn = MessageFormatter.replacePlaceholders(bypass_warn, sender.getName(), null, null, null, null);
                        sender.sendMessage(new TextComponent(bypass));
                        pplayer.sendMessage(new TextComponent(bypass_warn));
                        return; // Force exiting
                    }
                }
            }

            // Also search in knownplayers list if IP is protected.
            for (String key: Main.cfg.knownplayers.getKeys()) {
                if (ip_toban.equalsIgnoreCase(Main.cfg.knownplayers.getString(key+".ip")) || player_name.equalsIgnoreCase(Main.cfg.knownplayers.getString(key+".player"))) {
                    player_found = true;
                    ip_toban = Main.cfg.knownplayers.getString(key+".ip");
                    // Check if player has bypass from knownplayers file
                    if (Main.cfg.knownplayers.getBoolean(key+".bypass")) {
                        bypass = MessageFormatter.replacePlaceholders(bypass, null, null, ip_toban, null, null);
                        bypass_warn = MessageFormatter.replacePlaceholders(bypass_warn, sender.getName(), null, null, null, null);
                        sender.sendMessage(new TextComponent(bypass));
                        return; // Force exiting
                    }
                }
            }
            // Redo the loop, assuming we know the ip of the player now.
            if (!arg_is_ip) {
                for (String key: Main.cfg.knownplayers.getKeys()) {
                    if (ip_toban.equalsIgnoreCase(Main.cfg.knownplayers.getString(key+".ip"))) {
                        // Check if player has bypass from knownplayers file
                        if (Main.cfg.knownplayers.getBoolean(key+".bypass")) {
                            bypass = MessageFormatter.replacePlaceholders(bypass, null, null, ip_toban, null, null);
                            bypass_warn = MessageFormatter.replacePlaceholders(bypass_warn, sender.getName(), null, null, null, null);
                            sender.sendMessage(new TextComponent(bypass));
                            return; // Force exiting
                        }
                    }
                }
            }

            // If the code runs until here, then the IP is safe to be banned.
            // If ip not valid then return
            if (arg_is_ip) {
                execute_banip(ip_toban, sender, args);
            } else {
                // then player is not found or ip is not valid.
                if (player_found) execute_banip(ip_toban, sender, args);
                else sender.sendMessage(new TextComponent(unknown));
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
