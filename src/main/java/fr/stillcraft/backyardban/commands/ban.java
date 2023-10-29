package fr.stillcraft.backyardban.commands;

import com.google.common.collect.ImmutableSet;

import fr.stillcraft.backyardban.Main;
import io.netty.util.internal.StringUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class ban extends Command implements TabExecutor {
    public ban() { super("backyardban:ban","backyardban.ban", "ban"); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Get each string from config and locale data
        boolean broadcast = Main.config.getBoolean("format.broadcast");
        String banned = Main.locale.getString("ban.banned");
        String until = Main.locale.getString("ban.until");
        String confirm = Main.locale.getString("ban.confirm");
        String reason = Main.locale.getString("global.reason");
        String separator = Main.locale.getString("global.separator");
        String punctuation = Main.locale.getString("global.punctuation");
        String info = Main.locale.getString("ban.info");
        String bypass = Main.locale.getString("ban.bypass");
        String bypass_warn = Main.locale.getString("ban.bypass_warn");
        String usage = Main.locale.getString("global.usage")+Main.locale.getString("ban.usage");
        String description = Main.locale.getString("global.description")+Main.locale.getString("ban.description");
        String days = Main.locale.getString("global.days");
        String hours = Main.locale.getString("global.hours");
        String minutes = Main.locale.getString("global.minutes");
        String seconds = Main.locale.getString("global.seconds");
        String unknown = Main.locale.getString("ban.unknown");

        // Colorize each string
        banned = ChatColor.translateAlternateColorCodes('&', banned);
        until = ChatColor.translateAlternateColorCodes('&', until);
        confirm = ChatColor.translateAlternateColorCodes('&', confirm);
        reason = ChatColor.translateAlternateColorCodes('&', reason);
        separator = ChatColor.translateAlternateColorCodes('&', separator);
        punctuation = ChatColor.translateAlternateColorCodes('&', punctuation);
        info = ChatColor.translateAlternateColorCodes('&', info);
        usage = ChatColor.translateAlternateColorCodes('&', usage);
        description = ChatColor.translateAlternateColorCodes('&', description);
        bypass = ChatColor.translateAlternateColorCodes('&', bypass);
        bypass_warn = ChatColor.translateAlternateColorCodes('&', bypass_warn);
        days = ChatColor.translateAlternateColorCodes('&', days);
        hours = ChatColor.translateAlternateColorCodes('&', hours);
        minutes = ChatColor.translateAlternateColorCodes('&', minutes);
        seconds = ChatColor.translateAlternateColorCodes('&', seconds);
        unknown = ChatColor.translateAlternateColorCodes('&', unknown);

        if (args.length > 0) {
            // Return help message
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(new TextComponent(usage));
                sender.sendMessage(new TextComponent(description));
                return;
            }

            // Loop over players
            for (ProxiedPlayer player : Main.getInstance().getProxy().getPlayers()) {
                if (args[0].equalsIgnoreCase(player.getDisplayName())) {

                    // Get UUID of player
                    UUID player_uuid = player.getUniqueId();

                    // Construct complete ban strings
                    StringBuilder stringBuilder = new StringBuilder();

                    // Check if there is an end to that ban.
                    int reason_strstart = 1;
                    int timeleft = -1;
                    String timeleft_str = "";
                    if (args.length > 2) {
                        if (args[1].startsWith("t:")) {
                            reason_strstart = 2;
                            String timeleft_str_tmp = args[1].split(":")[1];
                            // Parse and convert time format to seconds from s | min | h | d | m | y
                            long endtime = System.currentTimeMillis() / 1000L;
                            if (timeleft_str_tmp.endsWith("s")) {
                                timeleft = Integer.parseInt(timeleft_str_tmp.split("s")[0]);
                            }
                            else if (timeleft_str_tmp.endsWith("min")) {
                                timeleft = 60*Integer.parseInt(timeleft_str_tmp.split("min")[0]);
                            }
                            else if (timeleft_str_tmp.endsWith("h")) {
                                timeleft = 3600*Integer.parseInt(timeleft_str_tmp.split("h")[0]);
                            }
                            else if (timeleft_str_tmp.endsWith("d")) {
                                timeleft = 86400*Integer.parseInt(timeleft_str_tmp.split("d")[0]);
                            }
                            else if (timeleft_str_tmp.endsWith("m")) {
                                timeleft = 2592000*Integer.parseInt(timeleft_str_tmp.split("m")[0]);
                            }
                            else if (timeleft_str_tmp.endsWith("y")) {
                                timeleft = 31104000*Integer.parseInt(timeleft_str_tmp.split("y")[0]);
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
                    bypass_warn = bypass_warn.replaceAll("%sender%", sender.getName());
                    banned = banned.replaceAll("%sender%", sender.getName());
                    confirm = confirm.replaceAll("%sender%", sender.getName());
                    info = info.replaceAll("%sender%", sender.getName());
                    banned = banned.replaceAll("%reason%", reason_string);
                    confirm = confirm.replaceAll("%reason%", reason_string);
                    info = info.replaceAll("%reason%", reason_string);
                    bypass = bypass.replaceAll("%player%", player.getDisplayName());
                    banned = banned.replaceAll("%player%", player.getDisplayName());
                    confirm = confirm.replaceAll("%player%", player.getDisplayName());
                    info = info.replaceAll("%player%", player.getDisplayName());
                    banned = banned.replaceAll("%timeleft%", timeleft_str);
                    confirm = confirm.replaceAll("%timeleft%", timeleft_str);
                    info = info.replaceAll("%timeleft%", timeleft_str);

                    // If player has bypass do not ban and warn player AND
                    // If sender is a player (CONSOLE and Rcon are not concerned)
                    if (player.hasPermission("backyardban.bypass") && (sender instanceof ProxiedPlayer)) {
                        sender.sendMessage(new TextComponent(bypass));
                        player.sendMessage(new TextComponent(bypass_warn));
                        return;
                    }

                    // Execute actions (kicks player, and send messages)
                    player.disconnect(new TextComponent(banned));
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
            }
            // Player not found, send message to sender
            unknown = unknown.replaceAll("%player%", args[0]);
            sender.sendMessage(new TextComponent(unknown));

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
