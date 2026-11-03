package io.github.mmarkimark.llartv;

/**
 * What the Wi-Fi pill on Home says about connectivity (#95), free of Android so the JVM tests can pin it.
 *
 * A TV can be connected to a network that has no Internet (a router without an uplink, a captive portal). The
 * system still reports a connection, so the pill used to show the network name while the weather and the news
 * quietly disappeared, because their fetches failed. Android's validated capability is what tells them apart,
 * and Home already watches it to retry downloads when real Internet comes back (Reconnect).
 *
 * Nothing is claimed before it is observed: until the first capability callback arrives the state is unknown,
 * and an unknown state keeps the network text rather than reporting a problem that may not exist.
 */
final class NetworkLabel {
    private NetworkLabel() {}

    enum Pill {
        OFFLINE,       // no connection at all
        NO_INTERNET,   // connected to a network that Android has not validated
        NETWORK        // the network's own text: its name, Ethernet, or just "connected"
    }

    /**
     * @param connected whether the system reports an active, connected network
     * @param validated Android's validated Internet capability, null while nothing has been observed yet
     */
    static Pill of(boolean connected, Boolean validated) {
        if (!connected) return Pill.OFFLINE;
        if (Boolean.FALSE.equals(validated)) return Pill.NO_INTERNET;
        return Pill.NETWORK;
    }
}
