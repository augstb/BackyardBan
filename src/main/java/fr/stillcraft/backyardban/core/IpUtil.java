package fr.stillcraft.backyardban.core;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** IP-address validation shared by the BungeeCord and Velocity banip commands. */
public final class IpUtil {
    private IpUtil() {}

    public static boolean isIPv4(String input) {
        try {
            InetAddress inetAddress = InetAddress.getByName(input);
            return (inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(input);
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    public static boolean isIPv6(String input) {
        try {
            InetAddress inetAddress = InetAddress.getByName(input);
            return (inetAddress instanceof Inet6Address);
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
