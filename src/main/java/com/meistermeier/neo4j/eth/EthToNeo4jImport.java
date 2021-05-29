package com.meistermeier.neo4j.eth;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.ipc.UnixIpcService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EthToNeo4jImport {

    private static Properties properties = new Properties();


    public static void main(String[] args) throws Exception {
        loadProperties();
        importBlocks();
    }

    private static void loadProperties() throws IOException {
        properties.load(EthToNeo4jImport.class.getClassLoader().getResourceAsStream("application.properties"));
    }

    public static void importBlocks() throws Exception {

        // Database driver (expensive creation)
        Driver driver = GraphDatabase.driver("neo4j://localhost:7687", AuthTokens.basic("neo4j", "secret"));

        // either use websocket

        // var url = properties.getProperty("web3j.websocket.uri");
        // var socketService = new WebSocketService(url, false);
        // socketService.connect();

        // or use ipc
        String ipcPath = properties.getProperty("web3j.ipc.path");
        var socketService = new UnixIpcService(ipcPath);

        var web3j = Web3j.build(socketService);

        // if the program got quit in between you can pick up here with the latest block
        int startingBlock = 4000000;

        int endBlock = web3j.ethBlockNumber().send().getBlockNumber().intValue();
        IntStream.rangeClosed(startingBlock, endBlock)
                .mapToObj(DefaultBlockParameterNumber::new)
                .forEach(thing -> fetchAndImportBlock(driver, web3j, thing));

        driver.close();
        web3j.shutdown();
    }


    private static void fetchAndImportBlock(Driver driver, Web3j web3j, DefaultBlockParameterNumber thing) {
        var fullTransactionObjects = true;

        try {
            // show progress to continue later
            if (thing.getBlockNumber().mod(BigInteger.valueOf(10000)).intValue() == 0) {
                System.out.println(thing.getBlockNumber());
            }
            var ethBlock = web3j.ethGetBlockByNumber(thing, fullTransactionObjects).send();
            // BlockTransactions containing 0..n transactions
            BlockTransactions blockTransactions = new BlockTransactions(
                    // create all transaction objects for one block
                    ethBlock.getBlock().getTransactions().stream().map(tx -> {
                    var txObject = (EthBlock.TransactionObject) tx;
                    String from = txObject.getFrom();
                    String to = txObject.getTo();
                    String txHash = txObject.getHash();
                    String txBlockNumber = txObject.getBlockNumberRaw();
                    return new Transaction(from, to, txHash, txBlockNumber);
            }).filter(Transaction::isValid).collect(Collectors.toList()));

            Map<String, Object> dataBlock = blockTransactions.toMap();

            // only make a request to the database if there is any data to persist
            if (blockTransactions.hasData()) {
                try (var session = driver.session()) {
                    session.writeTransaction(tx -> {
                        tx.run("""
                                UNWIND $params as row
                                    MERGE (from:Identity{address:row.addressFrom})
                                    MERGE (to:Identity{address:row.addressTo})
                                    CREATE (from)-[:SENDS_TO{blockNumber: row.blockNumber, transactionHash: row.transactionHash}]->(to)
                                """,
                            dataBlock
                        ).consume();
                        return null;
                    });
                }
            }
        } catch (Exception e) {
            // silence
        }
    }

}
