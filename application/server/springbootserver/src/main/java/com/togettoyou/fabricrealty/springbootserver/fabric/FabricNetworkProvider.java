package com.togettoyou.fabricrealty.springbootserver.fabric;

import org.hyperledger.fabric.client.Network;

public interface FabricNetworkProvider {
    Network getNetwork(String orgName);
}

