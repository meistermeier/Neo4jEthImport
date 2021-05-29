package com.meistermeier.neo4j.eth;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record BlockTransactions(List<Transaction> transactions) {

    private static final String BLOCK_PARAMS_KEY = "params";

    /**
     * Checks if there is any transaction to store for this block.
     *
     * @return true, if there is data to store, otherwise false
     */
    public boolean hasData() {
        return !transactions.isEmpty();
    }

    /**
     * Creates a map representation of the block containing all transactions' map representations.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        return Map.of(BLOCK_PARAMS_KEY, transactions.stream().map(Transaction::toMap).collect(Collectors.toList()));
    }
}


