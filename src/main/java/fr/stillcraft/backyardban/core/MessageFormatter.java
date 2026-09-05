package fr.stillcraft.backyardban.core;

/**
 * Platform-agnostic message-building helpers shared by the BungeeCord and Velocity ban/unban/banip
 * commands (and login listeners), so reason-parsing and placeholder-substitution logic only ever
 * lives in one place. Operates purely on raw '&'-coded strings; colorizing/sending them into the
 * platform's own text/Component API is each platform adapter's own job.
 */
public final class MessageFormatter {
    private MessageFormatter() {}

    // Joins args[fromIndex..] into a trimmed reason string ("" if there is none).
    public static String buildReason(String[] args, int fromIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = fromIndex; i < args.length; i++) {
            builder.append(args[i]).append(" ");
        }
        String reason = builder.toString();
        return reason.trim().isEmpty() ? "" : reason.substring(0, reason.length() - 1);
    }

    // Prepends " " + untilTemplate to each template when a duration was actually applied.
    public static String[] appendUntilIfPresent(String[] templates, String untilTemplate, boolean hasTimeleft) {
        if (!hasTimeleft) return templates;
        String[] result = new String[templates.length];
        for (int i = 0; i < templates.length; i++) result[i] = templates[i] + " " + untilTemplate;
        return result;
    }

    // Appends punctuation (no reason given) or separator+reasonTemplate (reason given) to each template.
    public static String[] appendReasonOrPunctuation(String[] templates, String reasonString,
                                                      String reasonTemplate, String separator, String punctuation) {
        String suffix = reasonString.isEmpty() ? punctuation : (separator + reasonTemplate);
        String[] result = new String[templates.length];
        for (int i = 0; i < templates.length; i++) result[i] = templates[i] + suffix;
        return result;
    }

    // Safe (non-regex) placeholder substitution. Uses String.replace() rather than replaceAll() so
    // player-supplied text (reason, player name) containing regex-special characters like $ or \
    // can't crash the command. Pass null to skip a placeholder.
    public static String replacePlaceholders(String template, String sender, String player, String ip, String reason, String timeleft) {
        String result = template;
        if (sender != null) result = result.replace("%sender%", sender);
        if (player != null) result = result.replace("%player%", player);
        if (ip != null) result = result.replace("%ip%", ip);
        if (reason != null) result = result.replace("%reason%", reason);
        if (timeleft != null) result = result.replace("%timeleft%", timeleft);
        return result;
    }
}
