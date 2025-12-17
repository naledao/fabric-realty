package com.togettoyou.fabricrealty.springbootserver.fabric;

public interface FabricClient {
    byte[] evaluate(String orgName, String transactionName, String... args);

    byte[] submit(String orgName, String transactionName, String... args);
}

