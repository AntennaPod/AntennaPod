package de.danoeh.antennapod.net.sync;

import org.apache.commons.lang3.StringUtils;

import java.net.IDN;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostnameParser {
    public String scheme;
    public int port;
    public String host;
    public String subfolder;

    // split into schema, host and port - missing parts are null
    private static final Pattern URLSPLIT_REGEX = Pattern.compile("(?:(https?)://)?([^:/]+)(?::(\\d+))?(.+)?");

    public HostnameParser(String hosturl) {
        Matcher m = URLSPLIT_REGEX.matcher(hosturl);
        if (m.matches()) {
            scheme = m.group(1);
            host = IDN.toASCII(m.group(2));
            if (m.group(3) == null) {
                port = -1;
            } else {
                port = Integer.parseInt(m.group(3));    // regex -> can only be digits
            }
            if (m.group(4) == null) {
                subfolder = "";
            } else {
                subfolder = StringUtils.stripEnd(m.group(4), "/");
            }
        } else {
            // URL does not match regex: use it anyway -> this will cause an exception on connect
            scheme = "https";
            host = IDN.toASCII(hosturl);
            port = 443;
        }

        if (scheme == null && port == 80) {
            scheme = "http";
        } else if (scheme == null) {
            scheme = "https"; // assume https
        }

        if (scheme.equals("https") && port == -1) {
            port = 443;
        } else if (scheme.equals("http") && port == -1) {
            port = 80;
        }
    }
}
