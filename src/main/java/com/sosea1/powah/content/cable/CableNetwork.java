package com.sosea1.powah.content.cable;

import java.util.Collections;
import java.util.List;

final class CableNetwork {
    final List<TileCable> cables;
    final List<CableEndpoint> endpoints;
    final long builtAt;
    boolean insertionGuard;

    CableNetwork(List<TileCable> cables, List<CableEndpoint> endpoints, long builtAt) {
        this.cables = Collections.unmodifiableList(cables);
        this.endpoints = Collections.unmodifiableList(endpoints);
        this.builtAt = builtAt;
    }

    void invalidate() {
        for (TileCable cable : cables) {
            if (cable.getNetworkIfPresent() == this) {
                cable.setNetwork(null);
            }
        }
    }
}
