package io.github.mmarkimark.llartv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NetworkLabelTest {
    @Test public void noConnectionIsOffline() {
        assertEquals(NetworkLabel.Pill.OFFLINE, NetworkLabel.of(false, null));
        assertEquals(NetworkLabel.Pill.OFFLINE, NetworkLabel.of(false, false));
        assertEquals(NetworkLabel.Pill.OFFLINE, NetworkLabel.of(false, true));
    }

    @Test public void aConnectionWithoutInternetSaysSo() {
        // The router has no uplink, or a captive portal is in the way: the cards will fail, so Home says it (#95).
        assertEquals(NetworkLabel.Pill.NO_INTERNET, NetworkLabel.of(true, false));
    }

    @Test public void aValidatedConnectionShowsTheNetwork() {
        assertEquals(NetworkLabel.Pill.NETWORK, NetworkLabel.of(true, true));
    }

    @Test public void anUnobservedStateClaimsNothing() {
        // Until the first capability callback arrives nothing is known: keep the network text, do not warn.
        assertEquals(NetworkLabel.Pill.NETWORK, NetworkLabel.of(true, null));
    }
}
