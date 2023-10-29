package fr.stillcraft.backyardban.commands;

import com.google.common.collect.ImmutableSet;

import fr.stillcraft.backyardban.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class backyardban extends Command implements TabExecutor {
    public backyardban() {
        super("backyardban", "backyardban.ban", "byb");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length >= 1) {
            if(args[0].equals("ban")) new ban().execute(sender, Arrays.copyOfRange(args, 1, args.length));
            // else if(args[0].equals("banip")) new banip().execute(sender, Arrays.copyOfRange(args, 1, args.length));
            else if(args[0].equals("reload")) new reload().execute(sender, Arrays.copyOfRange(args, 1, args.length));
            else if(args[0].equals("help")) new help().execute(sender, Arrays.copyOfRange(args, 1, args.length));
            else if(args[0].equals("version")) new version().execute(sender, Arrays.copyOfRange(args, 1, args.length));
            else if(args[0].equals("info")) new version().execute(sender, Arrays.copyOfRange(args, 1, args.length));
            else new help().execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            new help().execute(sender,null);
        }
    }

    public Iterable<String> onTabComplete(CommandSender sender, String[] args){
        if (args.length>2 || args.length==0){
            return ImmutableSet.of();
        }

        Set<String> matches = new HashSet<>();
        if (args.length == 1){
            String search = args[0].toLowerCase();
            if ("help".startsWith(search)) matches.add("help");
            if ("ban".startsWith(search)) matches.add("ban");
            // if ("banip".startsWith(search)) matches.add("banip");
            if ("reload".startsWith(search)) matches.add("reload");
            if ("version".startsWith(search)) matches.add("version");
            if ("info".startsWith(search)) matches.add("info");
        }
        if (args.length == 2){
            String cmd = args[0].toLowerCase();
            String search = args[1].toLowerCase();
            if (cmd.equalsIgnoreCase("ban")){
                for (ProxiedPlayer player: Main.getInstance().getProxy().getPlayers()){
                    if (player.getName().toLowerCase().startsWith(search)){
                        matches.add(player.getName());
                    }
                }
                if ("help".startsWith(search)) matches.add("help");
            }
            // if (cmd.equalsIgnoreCase("banip")){
            //     if ("help".startsWith(search)) matches.add("help");
            // }
            if (cmd.equalsIgnoreCase("reload")){
                if ("help".startsWith(search)) matches.add("help");
            }
        }
        return matches;
    }
}
