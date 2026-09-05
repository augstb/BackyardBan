package fr.stillcraft.backyardban.core;

import net.md_5.bungee.config.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Reads and writes ban records (data/banlist.yml, data/baniplist.yml) and decides whether a given
 * player/IP is currently banned. Shared verbatim by the BungeeCord and Velocity login listeners so
 * "is this player banned right now" can only ever be answered one way.
 */
public final class BanService {
    private BanService() {}

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static String ipToKey(String ip) { return ip.replace(".", "-").replace(":", "_"); }
    public static String keyToIp(String key) { return key.replace("-", ".").replace("_", ":"); }

    public static String formatDate(long epochSeconds) {
        return DATE_FORMAT.get().format(new Date(epochSeconds * 1000L));
    }

    // "until" dates use "Forever" for a permanent ban (endtime < 0) instead of a literal epoch-0 date.
    public static String formatUntilDate(long endtime) {
        return endtime < 0 ? "Forever" : formatDate(endtime);
    }

    public static void recordBan(Configuration banlist, UUID uuid, String player, String banisher,
                                  long fromtime, long endtime, String reason, String ip) {
        String key = uuid.toString();
        banlist.set(key + ".player", player);
        banlist.set(key + ".banisher", banisher);
        banlist.set(key + ".from", fromtime);
        banlist.set(key + ".until", endtime);
        banlist.set(key + ".fromdate", formatDate(fromtime));
        banlist.set(key + ".untildate", formatUntilDate(endtime));
        banlist.set(key + ".reason", reason);
        banlist.set(key + ".ip", ip);
    }

    public static void recordIpBan(Configuration baniplist, String ipKey, String banisher,
                                    long fromtime, long endtime, String reason) {
        baniplist.set(ipKey + ".banisher", banisher);
        baniplist.set(ipKey + ".from", fromtime);
        baniplist.set(ipKey + ".until", endtime);
        baniplist.set(ipKey + ".fromdate", formatDate(fromtime));
        baniplist.set(ipKey + ".untildate", formatUntilDate(endtime));
        baniplist.set(ipKey + ".reason", reason);
    }

    // Marks a ban record as expired as of now (keeps history: banisher/reason/etc are left untouched).
    public static void clearBan(Configuration list, String key) {
        long now = System.currentTimeMillis() / 1000L;
        list.set(key + ".until", now);
        list.set(key + ".untildate", formatDate(now));
    }

    public static final class BanStatus {
        public final boolean banned;
        public final long until;
        public final long timeleft;
        public final String banisher;
        public final String reason;

        private BanStatus(boolean banned, long until, long timeleft, String banisher, String reason) {
            this.banned = banned;
            this.until = until;
            this.timeleft = timeleft;
            this.banisher = banisher;
            this.reason = reason;
        }
    }

    // Works identically for banlist (key = uuid string) and baniplist (key = ip key).
    public static BanStatus checkBan(Configuration list, String key, String defaultBanisher) {
        long until = -1;
        long timeleft = -1;
        String banisher = defaultBanisher;
        String reason = "";
        boolean present = list.getKeys().contains(key);
        if (present) {
            if (list.getSection(key).contains("until")) {
                until = list.getLong(key + ".until");
                timeleft = until - System.currentTimeMillis() / 1000L;
            }
            if (list.getSection(key).contains("banisher")) {
                banisher = list.getString(key + ".banisher");
            }
            if (list.getSection(key).contains("reason")) {
                reason = list.getString(key + ".reason");
            }
        }
        boolean banned = present && (until < 0 || timeleft > 0);
        return new BanStatus(banned, until, timeleft, banisher, reason);
    }
}
