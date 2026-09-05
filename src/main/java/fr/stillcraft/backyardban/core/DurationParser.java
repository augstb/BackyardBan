package fr.stillcraft.backyardban.core;

import java.time.Duration;

/**
 * Parses BackyardBan's "t:&lt;n&gt;&lt;unit&gt;" ban-duration argument (units: s, min, h, d, m, y).
 * Shared verbatim by the BungeeCord and Velocity ban/banip commands.
 */
public final class DurationParser {
    private DurationParser() {}

    public static final class Result {
        public final int reasonStartIndex; // 1 if no duration argument was given, 2 if one was consumed
        public final boolean invalid;      // a "t:" argument was present but unparseable/overflowing
        public final long timeleft;        // seconds; > 0 only when a valid duration was parsed
        public final long endtime;         // epoch seconds; -1 = forever
        public final String timeleftStr;   // human-readable, e.g. "3d"; "" if none

        private Result(int reasonStartIndex, boolean invalid, long timeleft, long endtime, String timeleftStr) {
            this.reasonStartIndex = reasonStartIndex;
            this.invalid = invalid;
            this.timeleft = timeleft;
            this.endtime = endtime;
            this.timeleftStr = timeleftStr;
        }
    }

    // days/hours/minutes/seconds are the localized unit suffixes for the human-readable string
    // (e.g. "j"/"h"/"m"/"s" in French, from global.days/hours/minutes/seconds).
    public static Result parse(String[] args, String days, String hours, String minutes, String seconds) {
        int reasonStartIndex = 1;
        long timeleft = -1;
        long endtime = -1;
        String timeleftStr = "";
        boolean invalid = false;

        if (args.length > 1 && args[1].startsWith("t:")) {
            reasonStartIndex = 2;
            String[] timeleft_str_parts = args[1].split(":");
            if (timeleft_str_parts.length > 1) {
                String timeleft_str_tmp = timeleft_str_parts[1];
                // Parse and convert time format to seconds from s | min | h | d | m | y
                endtime = System.currentTimeMillis() / 1000L;
                try {
                    if (timeleft_str_tmp.endsWith("s")) {
                        String[] p = timeleft_str_tmp.split("s");
                        if (p.length > 0) timeleft = Integer.parseInt(p[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("min")) {
                        String[] p = timeleft_str_tmp.split("min");
                        if (p.length > 0) timeleft = 60L*Integer.parseInt(p[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("h")) {
                        String[] p = timeleft_str_tmp.split("h");
                        if (p.length > 0) timeleft = 3600L*Integer.parseInt(p[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("d")) {
                        String[] p = timeleft_str_tmp.split("d");
                        if (p.length > 0) timeleft = 86400L*Integer.parseInt(p[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("m")) {
                        String[] p = timeleft_str_tmp.split("m");
                        if (p.length > 0) timeleft = 2592000L*Integer.parseInt(p[0]);
                    }
                    else if (timeleft_str_tmp.endsWith("y")) {
                        String[] p = timeleft_str_tmp.split("y");
                        if (p.length > 0) timeleft = 31104000L*Integer.parseInt(p[0]);
                    }
                } catch (NumberFormatException e) {
                    timeleft = -1;
                }
                if (timeleft > 0) {
                    endtime += timeleft;
                    timeleftStr = humanReadable(timeleft, days, hours, minutes, seconds);
                } else {
                    // Unrecognized/overflowing duration suffix: reject rather than silently ban forever.
                    invalid = true;
                }
            } else {
                // No value after "t:" (e.g. just "t:"): reject rather than silently ban forever.
                invalid = true;
            }
        }

        return new Result(reasonStartIndex, invalid, timeleft, endtime, timeleftStr);
    }

    // Also used to render a BanService.BanStatus's remaining timeleft for a login-time kick message.
    public static String humanReadable(long timeleft, String days, String hours, String minutes, String seconds) {
        Duration d = Duration.ofSeconds(timeleft);
        long int_days = d.toDays(); d = d.minusDays(int_days);
        long int_hours = d.toHours(); d = d.minusHours(int_hours);
        long int_minutes = d.toMinutes(); d = d.minusMinutes(int_minutes);
        long int_seconds = d.getSeconds();
        if (int_days > 0) return Long.toString(int_days) + days;
        else if (int_hours > 0) return Long.toString(int_hours) + hours;
        else if (int_minutes > 0) return Long.toString(int_minutes) + minutes;
        else if (int_seconds > 0) return Long.toString(int_seconds) + seconds;
        return "";
    }
}
