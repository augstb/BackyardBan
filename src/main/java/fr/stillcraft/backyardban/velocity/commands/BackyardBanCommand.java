package fr.stillcraft.backyardban.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.stillcraft.backyardban.velocity.VelocityMain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BackyardBanCommand implements SimpleCommand {
    private final VelocityMain plugin;
    private final BanCommand banCommand;
    private final UnbanCommand unbanCommand;
    private final BanipCommand banipCommand;
    private final ReloadCommand reloadCommand;
    private final HelpCommand helpCommand;
    private final VersionCommand versionCommand;

    public BackyardBanCommand(VelocityMain plugin) {
        this.plugin = plugin;
        this.banCommand = new BanCommand(plugin);
        this.unbanCommand = new UnbanCommand(plugin);
        this.banipCommand = new BanipCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.helpCommand = new HelpCommand(plugin);
        this.versionCommand = new VersionCommand(plugin);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        boolean senderIsPlayer = (sender instanceof Player);
        boolean hasUnbanPerm = (!senderIsPlayer || sender.hasPermission("backyardban.unban"));
        boolean hasBanipPerm = (!senderIsPlayer || sender.hasPermission("backyardban.banip"));
        boolean hasReloadPerm = (!senderIsPlayer || sender.hasPermission("backyardban.reload"));

        if (args.length >= 1) {
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            if (args[0].equals("ban")) banCommand.execute(subInvocation(sender, rest, "ban"));
            else if ((args[0].equals("unban") || args[0].equals("pardon")) && hasUnbanPerm) unbanCommand.execute(subInvocation(sender, rest, "unban"));
            else if (args[0].equals("banip") && hasBanipPerm) banipCommand.execute(subInvocation(sender, rest, "banip"));
            else if (args[0].equals("reload") && hasReloadPerm) reloadCommand.execute(subInvocation(sender, rest, "reload"));
            else if (args[0].equals("help")) helpCommand.execute(subInvocation(sender, rest, "help"));
            else if (args[0].equals("version")) versionCommand.execute(subInvocation(sender, rest, "version"));
            else if (args[0].equals("info")) versionCommand.execute(subInvocation(sender, rest, "info"));
            else helpCommand.execute(subInvocation(sender, rest, "help"));
        } else {
            helpCommand.execute(subInvocation(sender, new String[0], "help"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        boolean senderIsPlayer = (sender instanceof Player);
        boolean hasUnbanPerm = (!senderIsPlayer || sender.hasPermission("backyardban.unban"));
        boolean hasBanipPerm = (!senderIsPlayer || sender.hasPermission("backyardban.banip"));
        boolean hasReloadPerm = (!senderIsPlayer || sender.hasPermission("backyardban.reload"));

        if (args.length > 2 || args.length == 0) {
            return Collections.emptyList();
        }

        List<String> matches = new ArrayList<>();
        if (args.length == 1) {
            String search = args[0].toLowerCase();
            if ("help".startsWith(search)) matches.add("help");
            if ("ban".startsWith(search)) matches.add("ban");
            if (("unban".startsWith(search) || "pardon".startsWith(search)) && hasUnbanPerm) matches.add("unban");
            if ("banip".startsWith(search) && hasBanipPerm) matches.add("banip");
            if ("reload".startsWith(search) && hasReloadPerm) matches.add("reload");
            if ("version".startsWith(search)) matches.add("version");
            if ("info".startsWith(search)) matches.add("info");
        }
        if (args.length == 2) {
            String cmd = args[0].toLowerCase();
            String search = args[1].toLowerCase();
            if (cmd.equalsIgnoreCase("ban")) {
                for (Player player : plugin.getServer().getAllPlayers()) {
                    if (player.getUsername().toLowerCase().startsWith(search)) {
                        matches.add(player.getUsername());
                    }
                }
                if ("help".startsWith(search)) matches.add("help");
            }
            if ((cmd.equalsIgnoreCase("unban") || cmd.equalsIgnoreCase("pardon")) && hasUnbanPerm) {
                if ("help".startsWith(search)) matches.add("help");
            }
            if (cmd.equalsIgnoreCase("banip") && hasBanipPerm) {
                if ("help".startsWith(search)) matches.add("help");
            }
            if (cmd.equalsIgnoreCase("reload") && hasReloadPerm) {
                if ("help".startsWith(search)) matches.add("help");
            }
        }
        return matches;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("backyardban.ban");
    }

    // Velocity gives no public factory for its own Invocation, so build a tiny one to forward a
    // sub-command's remaining args (mirrors how the BungeeCord backyardban command directly calls
    // e.g. `new ban().execute(sender, Arrays.copyOfRange(args, 1, args.length))`).
    private static Invocation subInvocation(CommandSource source, String[] args, String alias) {
        return new Invocation() {
            @Override public CommandSource source() { return source; }
            @Override public String[] arguments() { return args; }
            @Override public String alias() { return alias; }
        };
    }
}
