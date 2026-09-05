package fr.stillcraft.backyardban.commands;

import com.google.common.collect.ImmutableSet;

import fr.stillcraft.backyardban.Main;
import fr.stillcraft.backyardban.core.BanService;
import fr.stillcraft.backyardban.core.DurationParser;
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

public class ban extends Command implements TabExecutor {
    public ban() { super("backyardban:ban","backyardban.ban", "ban"); }

    public void execute_ban(UUID player_uuid, String player_name, String player_ip, CommandSender sender, String[] args, ProxiedPlayer player){
        // Get each string from config and locale data
        boolean broadcast = Main.cfg.cfgBool("broadcast");
        String banned = Main.cfg.msg("ban.banned");
        String until = Main.cfg.msg("ban.until");
        String confirm = Main.cfg.msg("ban.confirm");
        String reason = Main.cfg.msg("global.reason");
        String separator = Main.cfg.msg("global.separator");
        String punctuation = Main.cfg.msg("global.punctuation");
        String info = Main.cfg.msg("ban.info");
        String days = Main.cfg.msg("global.days");
        String hours = Main.cfg.msg("global.hours");
        String minutes = Main.cfg.msg("global.minutes");
        String seconds = Main.cfg.msg("global.seconds");

        // Parse the optional "t:<duration>" argument
        DurationParser.Result duration = DurationParser.parse(args, days, hours, minutes, seconds);
        if (duration.invalid) {
            String usage = ChatColor.translateAlternateColorCodes('&', Main.cfg.msg("global.usage")+Main.cfg.msg("ban.usage"));
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
        banned = MessageFormatter.replacePlaceholders(banned, sender.getName(), player_name, null, reason_string, duration.timeleftStr);
        confirm = MessageFormatter.replacePlaceholders(confirm, sender.getName(), player_name, null, reason_string, duration.timeleftStr);
        info = MessageFormatter.replacePlaceholders(info, sender.getName(), player_name, null, reason_string, duration.timeleftStr);

        // Register the ban in yaml file.
        long fromtime = System.currentTimeMillis() / 1000L;
        BanService.recordBan(Main.cfg.banlist, player_uuid, player_name, sender.getName(), fromtime, duration.endtime, reason_string, player_ip);
        try {
            Main.cfg.saveConfig(Main.cfg.banlist, "data/banlist");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        // Execute actions (kicks player, and send messages)
        if (player != null) player.disconnect(new TextComponent(banned));
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
        String usage = Main.cfg.msg("global.usage")+Main.cfg.msg("ban.usage");
        String description = Main.cfg.msg("global.description")+Main.cfg.msg("ban.description");
        String yourself = Main.cfg.msg("ban.yourself");
        String bypass = Main.cfg.msg("ban.bypass");
        String bypass_warn = Main.cfg.msg("ban.bypass_warn");
        String unknown = Main.cfg.msg("ban.unknown");
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
            ProxiedPlayer player = null;

            for (ProxiedPlayer pplayer : Main.getInstance().getProxy().getPlayers()) {
                if (args[0].equalsIgnoreCase(pplayer.getName())) {
                    player_found = true;
                    player_uuid = pplayer.getUniqueId();

                    if (player_uuid.equals(sender_uuid)) {
                        // Deny players from banning themselves
                        sender.sendMessage(new TextComponent(yourself));
                    } else if (pplayer.hasPermission("backyardban.bypass")) {
                        // Deny to ban players that have bypass permission
                        bypass = MessageFormatter.replacePlaceholders(bypass, null, pplayer.getName(), null, null, null);
                        bypass_warn = MessageFormatter.replacePlaceholders(bypass_warn, sender.getName(), null, null, null, null);
                        sender.sendMessage(new TextComponent(bypass));
                        pplayer.sendMessage(new TextComponent(bypass_warn));
                    } else {
                        // Ban this UUID
                        player_name = pplayer.getName();
                        player_ip = ((InetSocketAddress) pplayer.getSocketAddress()).getAddress().getHostAddress();
                        execute_ban(player_uuid, player_name, player_ip, sender, args, pplayer);
                    }
                }
            }

            // If player is not online, then search in the database file
            if (!player_found) {
                for (String key: Main.cfg.knownplayers.getKeys()) {
                    if (args[0].equalsIgnoreCase(Main.cfg.knownplayers.getString(key+".player"))) {
                        player_found = true;
                        player_uuid = UUID.fromString(key);
                        player_name = Main.cfg.knownplayers.getString(key+".player");
                        player_ip = Main.cfg.knownplayers.getString(key+".ip");
                        // Check if player has bypass from knownplayers file
                        if (Main.cfg.knownplayers.getBoolean(player_uuid.toString()+".bypass")) {
                            bypass = MessageFormatter.replacePlaceholders(bypass, null, player_name, null, null, null);
                            bypass_warn = MessageFormatter.replacePlaceholders(bypass_warn, sender.getName(), null, null, null, null);
                            sender.sendMessage(new TextComponent(bypass));
                        }
                        else{
                            // EXECUTE BAN
                            execute_ban(player_uuid, player_name, player_ip, sender, args, player);
                        }
                        break; // Match found, stop scanning for duplicate entries.
                    }
                }
            }

            if (!player_found) {
                // Send message to sender if no player has been banned.
                unknown = MessageFormatter.replacePlaceholders(unknown, sender.getName(), args[0], null, null, null);
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
