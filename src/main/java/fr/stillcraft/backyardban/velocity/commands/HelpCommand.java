package fr.stillcraft.backyardban.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.stillcraft.backyardban.core.BackyardBanConfig;
import fr.stillcraft.backyardban.velocity.VelocityMain;
import fr.stillcraft.backyardban.velocity.VelocityText;

public class HelpCommand implements SimpleCommand {
    private final VelocityMain plugin;

    public HelpCommand(VelocityMain plugin) { this.plugin = plugin; }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        BackyardBanConfig cfg = plugin.cfg;

        boolean senderIsPlayer = (sender instanceof Player);
        boolean hasUnbanPerm = (!senderIsPlayer || sender.hasPermission("backyardban.unban"));
        boolean hasBanipPerm = (!senderIsPlayer || sender.hasPermission("backyardban.banip"));
        boolean hasReloadPerm = (!senderIsPlayer || sender.hasPermission("backyardban.reload"));

        // Get each string from config and locale data
        String globalPrefix = cfg.msg("global.prefix");
        String helpUsage = cfg.msg("help.usage");
        String helpDescription = cfg.msg("help.description");
        String banUsage = cfg.msg("ban.usage");
        String banDescription = cfg.msg("ban.description");
        String banipUsage = cfg.msg("banip.usage");
        String banipDescription = cfg.msg("banip.description");
        String unbanUsage = cfg.msg("unban.usage");
        String unbanDescription = cfg.msg("unban.description");
        String reloadUsage = cfg.msg("reload.usage");
        String reloadDescription = cfg.msg("reload.description");
        String versionUsage = cfg.msg("version.usage");
        String versionDescription = cfg.msg("version.description");

        VelocityText.send(sender, "&f--- " + globalPrefix + "&f ---");
        VelocityText.send(sender, banUsage + "&f - " + banDescription);
        if (hasBanipPerm) VelocityText.send(sender, banipUsage + "&f - " + banipDescription);
        if (hasUnbanPerm) VelocityText.send(sender, unbanUsage + "&f - " + unbanDescription);
        VelocityText.send(sender, helpUsage + "&f - " + helpDescription);
        if (hasReloadPerm) VelocityText.send(sender, reloadUsage + "&f - " + reloadDescription);
        VelocityText.send(sender, versionUsage + "&f - " + versionDescription);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("backyardban.ban");
    }
}
