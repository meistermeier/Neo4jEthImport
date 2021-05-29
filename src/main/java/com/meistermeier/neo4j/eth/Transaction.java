package com.meistermeier.neo4j.eth;

import java.util.Map;

public record Transaction(String from, String to, String txHash,
                          String txBlockNumber) {

    private static final String KEY_ADDRESS_FROM = "addressFrom";
    private static final String KEY_ADDRESS_TO = "addressTo";
    private static final String KEY_TRANSACTION_HASH = "transactionHash";
    private static final String KEY_BLOCK_NUMBER = "blockNumber";

    /**
     * Determines if this transaction object contains all four mandatory values.
     *
     * @return true - if this is a valid transaction value to store, otherwise false
     */
    public boolean isValid() {
        return from() != null
                && to() != null
                && txBlockNumber() != null
                && txHash() != null;
    }

    /**
     * Creates a map representation of the properties.
     *
     * @return Map representation of the properties.
     */
    public Map<String, Object> toMap() {
        return Map.of(
                KEY_ADDRESS_FROM, from(),
                KEY_ADDRESS_TO, to(),
                KEY_TRANSACTION_HASH, txHash(),
                KEY_BLOCK_NUMBER, txBlockNumber());
    }
}
