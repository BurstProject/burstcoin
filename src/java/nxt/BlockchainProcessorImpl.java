package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.DbIterator;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import fr.cryptohash.Shabal256;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

final class BlockchainProcessorImpl implements BlockchainProcessor {
    private static final BlockchainProcessorImpl instance = new BlockchainProcessorImpl();

    static BlockchainProcessorImpl getInstance() {
        return instance;
    }

    private final BlockchainImpl blockchain = BlockchainImpl.getInstance();
    private final TransactionProcessorImpl transactionProcessor = TransactionProcessorImpl.getInstance();

    private final Listeners<Block, Event> blockListeners = new Listeners<>();
    private volatile Peer lastBlockchainFeeder;
    private volatile int lastBlockchainFeederHeight;

    private volatile boolean isScanning;

    private final Runnable getMoreBlocksThread = new Runnable() {

        private final JSONStreamAware getCumulativeDifficultyRequest;

        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getCumulativeDifficulty");
            getCumulativeDifficultyRequest = JSON.prepareRequest(request);
        }

        private boolean peerHasMore;

        @Override
        public void run() {

            try {
                try {
                    peerHasMore = true;
                    Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    lastBlockchainFeeder = peer;
                    JSONObject response = peer.send(getCumulativeDifficultyRequest);
                    if (response == null) {
                        return;
                    }
                    BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
                    String peerCumulativeDifficulty = (String) response.get("cumulativeDifficulty");
                    if (peerCumulativeDifficulty == null) {
                        return;
                    }
                    BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                    if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) <= 0) {
                        return;
                    }
                    if (response.get("blockchainHeight") != null) {
                        lastBlockchainFeederHeight = ((Long) response.get("blockchainHeight")).intValue();
                    }

                    Long commonBlockId = Genesis.GENESIS_BLOCK_ID;

                    if (! blockchain.getLastBlock().getId().equals(Genesis.GENESIS_BLOCK_ID)) {
                        commonBlockId = getCommonMilestoneBlockId(peer);
                    }
                    if (commonBlockId == null || !peerHasMore) {
                        return;
                    }

                    commonBlockId = getCommonBlockId(peer, commonBlockId);
                    if (commonBlockId == null || !peerHasMore) {
                        return;
                    }

                    final Block commonBlock = BlockDb.findBlock(commonBlockId);
                    if (blockchain.getLastBlock().getHeight() - commonBlock.getHeight() >= 720) {
                        return;
                    }

                    Long currentBlockId = commonBlockId;
                    List<BlockImpl> forkBlocks = new ArrayList<>();

                    boolean processedAll = true;
                    outer:
                    while (true) {

                        JSONArray nextBlocks = getNextBlocks(peer, currentBlockId);
                        if (nextBlocks == null || nextBlocks.size() == 0) {
                            break;
                        }

                        synchronized (blockchain) {

                            for (Object o : nextBlocks) {
                                JSONObject blockData = (JSONObject) o;
                                BlockImpl block;
                                try {
                                    block = parseBlock(blockData);
                                } catch (NxtException.ValidationException e) {
                                    Logger.logDebugMessage("Cannot validate block: " + e.toString()
                                            + ", will try again later", e);
                                    processedAll = false;
                                    break outer;
                                } catch (RuntimeException e) {
                                    Logger.logDebugMessage("Failed to parse block: " + e.toString(), e);
                                    peer.blacklist();
                                    return;
                                }
                                currentBlockId = block.getId();

                                if (blockchain.getLastBlock().getId().equals(block.getPreviousBlockId())) {
                                    try {
                                        pushBlock(block);
                                    } catch (BlockNotAcceptedException e) {
                                        peer.blacklist(e);
                                        return;
                                    }
                                } else if (! BlockDb.hasBlock(block.getId())) {
                                    forkBlocks.add(block);
                                }

                            }

                        } //synchronized

                    }

                    if (forkBlocks.size() > 0) {
                        processedAll = false;
                    }

                    if (! processedAll && blockchain.getLastBlock().getHeight() - commonBlock.getHeight() < 720) {
                        processFork(peer, forkBlocks, commonBlock);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error in milestone blocks processing thread", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

        private Long getCommonMilestoneBlockId(Peer peer) {

            String lastMilestoneBlockId = null;

            while (true) {
                JSONObject milestoneBlockIdsRequest = new JSONObject();
                milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                if (lastMilestoneBlockId == null) {
                    milestoneBlockIdsRequest.put("lastBlockId", blockchain.getLastBlock().getStringId());
                } else {
                    milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
                }

                JSONObject response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest));
                if (response == null) {
                    return null;
                }
                JSONArray milestoneBlockIds = (JSONArray) response.get("milestoneBlockIds");
                if (milestoneBlockIds == null) {
                    return null;
                }
                if (milestoneBlockIds.isEmpty()) {
                    return Genesis.GENESIS_BLOCK_ID;
                }
                // prevent overloading with blockIds
                if (milestoneBlockIds.size() > 20) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many milestoneBlockIds, blacklisting");
                    peer.blacklist();
                    return null;
                }
                if (Boolean.TRUE.equals(response.get("last"))) {
                    peerHasMore = false;
                }
                for (Object milestoneBlockId : milestoneBlockIds) {
                    Long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                    if (BlockDb.hasBlock(blockId)) {
                        if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
                            peerHasMore = false;
                        }
                        return blockId;
                    }
                    lastMilestoneBlockId = (String) milestoneBlockId;
                }
            }

        }

        private Long getCommonBlockId(Peer peer, Long commonBlockId) {

            while (true) {
                JSONObject request = new JSONObject();
                request.put("requestType", "getNextBlockIds");
                request.put("blockId", Convert.toUnsignedLong(commonBlockId));
                JSONObject response = peer.send(JSON.prepareRequest(request));
                if (response == null) {
                    return null;
                }
                JSONArray nextBlockIds = (JSONArray) response.get("nextBlockIds");
                if (nextBlockIds == null || nextBlockIds.size() == 0) {
                    return null;
                }
                // prevent overloading with blockIds
                if (nextBlockIds.size() > 1440) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlockIds, blacklisting");
                    peer.blacklist();
                    return null;
                }

                for (Object nextBlockId : nextBlockIds) {
                    Long blockId = Convert.parseUnsignedLong((String) nextBlockId);
                    if (! BlockDb.hasBlock(blockId)) {
                        return commonBlockId;
                    }
                    commonBlockId = blockId;
                }
            }

        }

        private JSONArray getNextBlocks(Peer peer, Long curBlockId) {

            JSONObject request = new JSONObject();
            request.put("requestType", "getNextBlocks");
            request.put("blockId", Convert.toUnsignedLong(curBlockId));
            JSONObject response = peer.send(JSON.prepareRequest(request));
            if (response == null) {
                return null;
            }

            JSONArray nextBlocks = (JSONArray) response.get("nextBlocks");
            if (nextBlocks == null) {
                return null;
            }
            // prevent overloading with blocks
            if (nextBlocks.size() > 1440) {
                Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlocks, blacklisting");
                peer.blacklist();
                return null;
            }

            return nextBlocks;

        }

        private void processFork(Peer peer, final List<BlockImpl> forkBlocks, final Block commonBlock) {

            synchronized (blockchain) {
                BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();

                try {
                    Long lastBlockId = blockchain.getLastBlock().getId();
                    while (! lastBlockId.equals(commonBlock.getId()) && ! lastBlockId.equals(Genesis.GENESIS_BLOCK_ID)) {
                        lastBlockId = popLastBlock();
                    }
                } catch (TransactionType.UndoNotSupportedException e) {
                    Logger.logDebugMessage(e.getMessage());
                    Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
                    resetTo(commonBlock);
                }

                int pushedForkBlocks = 0;
                if (blockchain.getLastBlock().getId().equals(commonBlock.getId())) {
                    for (BlockImpl block : forkBlocks) {
                        if (blockchain.getLastBlock().getId().equals(block.getPreviousBlockId())) {
                            try {
                                pushBlock(block);
                                pushedForkBlocks += 1;
                            } catch (BlockNotAcceptedException e) {
                                peer.blacklist(e);
                                break;
                            }
                        }
                    }
                }

                if (pushedForkBlocks > 0 && blockchain.getLastBlock().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {
                    Logger.logDebugMessage("Rescan caused by peer " + peer.getPeerAddress() + ", blacklisting");
                    peer.blacklist();
                    resetTo(commonBlock);
                }
            } // synchronized

        }

        private void resetTo(Block commonBlock) {
            if (commonBlock.getNextBlockId() != null) {
                Logger.logDebugMessage("Last block is " + blockchain.getLastBlock().getStringId() + " at " + blockchain.getLastBlock().getHeight());
                Logger.logDebugMessage("Deleting blocks after height " + commonBlock.getHeight());
                BlockDb.deleteBlocksFrom(commonBlock.getNextBlockId());
            }
            Logger.logMessage("Will do a re-scan");
            blockListeners.notify(commonBlock, BlockchainProcessor.Event.RESCAN_BEGIN);
            scan();
            blockListeners.notify(commonBlock, BlockchainProcessor.Event.RESCAN_END);
            Logger.logDebugMessage("Last block is " + blockchain.getLastBlock().getStringId() + " at " + blockchain.getLastBlock().getHeight());
        }

    };

    private BlockchainProcessorImpl() {

        blockListeners.addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                if (block.getHeight() % 5000 == 0) {
                    Logger.logMessage("processed block " + block.getHeight());
                }
            }
        }, Event.BLOCK_SCANNED);

        ThreadPool.runBeforeStart(new Runnable() {
            @Override
            public void run() {
                addGenesisBlock();
                scan();
            }
        });

        ThreadPool.scheduleThread(getMoreBlocksThread, 1);

    }

    @Override
    public boolean addListener(Listener<Block> listener, BlockchainProcessor.Event eventType) {
        return blockListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<Block> listener, Event eventType) {
        return blockListeners.removeListener(listener, eventType);
    }

    @Override
    public Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    @Override
    public int getLastBlockchainFeederHeight() {
        return lastBlockchainFeederHeight;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public void processPeerBlock(JSONObject request) throws NxtException {
        BlockImpl block = parseBlock(request);
        pushBlock(block);
    }

    @Override
    public void fullReset() {
        synchronized (blockchain) {
            Logger.logMessage("Deleting blockchain...");
            //BlockDb.deleteBlock(Genesis.GENESIS_BLOCK_ID); // fails with stack overflow in H2
            BlockDb.deleteAll();
            addGenesisBlock();
            scan();
        }
    }

    private void addBlock(BlockImpl block) {
        try (Connection con = Db.getConnection()) {
            try {
                BlockDb.saveBlock(con, block);
                blockchain.setLastBlock(block);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void addGenesisBlock() {
        if (BlockDb.hasBlock(Genesis.GENESIS_BLOCK_ID)) {
            Logger.logMessage("Genesis block already in database");
            return;
        }
        Logger.logMessage("Genesis block not in database, starting from scratch");
        try {
            SortedMap<Long,TransactionImpl> transactionsMap = new TreeMap<>();

            MessageDigest digest = Crypto.sha256();
            for (Transaction transaction : transactionsMap.values()) {
                digest.update(transaction.getBytes());
            }

            BlockImpl genesisBlock = new BlockImpl(-1, 0, null, 0, 0, transactionsMap.size() * 128, digest.digest(),
                    Genesis.CREATOR_PUBLIC_KEY, new byte[32], Genesis.GENESIS_BLOCK_SIGNATURE, null, new ArrayList<>(transactionsMap.values()), 0);

            genesisBlock.setPrevious(null);

            addBlock(genesisBlock);

        } catch (NxtException.ValidationException e) {
            Logger.logMessage(e.getMessage());
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void pushBlock(final BlockImpl block) throws BlockNotAcceptedException {

        int curTime = Convert.getEpochTime();

        synchronized (blockchain) {
            try {

                BlockImpl previousLastBlock = blockchain.getLastBlock();

                if (! previousLastBlock.getId().equals(block.getPreviousBlockId())) {
                    throw new BlockOutOfOrderException("Previous block id doesn't match");
                }

                if (block.getVersion() != 1 && ! Arrays.equals(Crypto.sha256().digest(previousLastBlock.getBytes()), block.getPreviousBlockHash())) {
                    throw new BlockNotAcceptedException("Previous block hash doesn't match");
                }
                if (block.getTimestamp() > curTime + 15 || block.getTimestamp() <= previousLastBlock.getTimestamp()) {
                    throw new BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
                            + " current time is " + curTime + ", previous block timestamp is " + previousLastBlock.getTimestamp());
                }
                if (block.getId().equals(Long.valueOf(0L)) || BlockDb.hasBlock(block.getId())) {
                    throw new BlockNotAcceptedException("Duplicate block or invalid id");
                }
                if (! block.verifyGenerationSignature()) {
                    throw new BlockNotAcceptedException("Generation signature verification failed");
                }
                if (! block.verifyBlockSignature()) {
                    throw new BlockNotAcceptedException("Block signature verification failed");
                }

                Map<TransactionType, Set<String>> duplicates = new HashMap<>();
                long calculatedTotalAmount = 0;
                long calculatedTotalFee = 0;
                MessageDigest digest = Crypto.sha256();

                Set<Long> unappliedUnconfirmed = transactionProcessor.undoAllUnconfirmed();
                Set<Long> appliedUnconfirmed = new HashSet<>();

                try {

                    for (TransactionImpl transaction : block.getTransactions()) {

                        if (transaction.getTimestamp() > curTime + 15 || transaction.getTimestamp() > block.getTimestamp() + 15
                                || transaction.getExpiration() < block.getTimestamp()) {
                            throw new TransactionNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                                    + " for transaction " + transaction.getStringId() + ", current time is " + curTime
                                    + ", block timestamp is " + block.getTimestamp(), transaction);
                        }
                        if (TransactionDb.hasTransaction(transaction.getId())) {
                            throw new TransactionNotAcceptedException("Transaction " + transaction.getStringId()
                                    + " is already in the blockchain", transaction);
                        }
                        if (transaction.getReferencedTransactionFullHash() != null) {
                            if ((previousLastBlock.getHeight() < Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                                    && !TransactionDb.hasTransaction(Convert.fullHashToId(transaction.getReferencedTransactionFullHash())))
                                    || (previousLastBlock.getHeight() >= Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                                    && !hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0))) {
                                throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
                                        + transaction.getReferencedTransactionFullHash()
                                        + " for transaction " + transaction.getStringId(), transaction);
                            }
                        }
                        if (!transaction.verify()) {
                            throw new TransactionNotAcceptedException("Signature verification failed for transaction "
                                    + transaction.getStringId() + " at height " + previousLastBlock.getHeight(), transaction);
                        }
                        if (transaction.getId().equals(Long.valueOf(0L))) {
                            throw new TransactionNotAcceptedException("Invalid transaction id", transaction);
                        }
                        if (transaction.isDuplicate(duplicates)) {
                            throw new TransactionNotAcceptedException("Transaction is a duplicate: "
                                    + transaction.getStringId(), transaction);
                        }
                        try {
                            transaction.validateAttachment();
                        } catch (NxtException.ValidationException e) {
                            throw new TransactionNotAcceptedException(e.getMessage(), transaction);
                        }

                        if (transaction.applyUnconfirmed()) {
                            appliedUnconfirmed.add(transaction.getId());
                        } else {
                            throw new TransactionNotAcceptedException("Double spending transaction: " + transaction.getStringId(), transaction);
                        }

                        calculatedTotalAmount += transaction.getAmountNQT();

                        calculatedTotalFee += transaction.getFeeNQT();

                        digest.update(transaction.getBytes());

                    }

                    if (calculatedTotalAmount != block.getTotalAmountNQT() || calculatedTotalFee != block.getTotalFeeNQT()) {
                        throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals");
                    }
                    if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
                        throw new BlockNotAcceptedException("Payload hash doesn't match");
                    }

                    block.setPrevious(previousLastBlock);

                    addBlock(block);

                    unappliedUnconfirmed.removeAll(appliedUnconfirmed);

                } catch (TransactionNotAcceptedException | RuntimeException e) {
                    for (TransactionImpl transaction : block.getTransactions()) {
                        if (appliedUnconfirmed.contains(transaction.getId())) {
                            transaction.undoUnconfirmed();
                        }
                    }
                    throw e;
                } finally {
                    transactionProcessor.applyUnconfirmed(unappliedUnconfirmed);
                }

                blockListeners.notify(block, Event.BEFORE_BLOCK_APPLY);
                transactionProcessor.apply(block);
                blockListeners.notify(block, Event.AFTER_BLOCK_APPLY);
                blockListeners.notify(block, Event.BLOCK_PUSHED);
                transactionProcessor.updateUnconfirmedTransactions(block);

            } catch (RuntimeException e) {
                Logger.logMessage("Error pushing block", e);
                BlockDb.deleteBlocksFrom(block.getId());
                scan();
                throw new BlockNotAcceptedException(e.toString());
            }

        } // synchronized

        if (block.getTimestamp() >= Convert.getEpochTime() - 15) {
            Peers.sendToSomePeers(block);
        }

    }

    private Long popLastBlock() throws TransactionType.UndoNotSupportedException {
        try {
            synchronized (blockchain) {
                BlockImpl block = blockchain.getLastBlock();
                Logger.logDebugMessage("Will pop block " + block.getStringId() + " at height " + block.getHeight());
                if (block.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
                    throw new RuntimeException("Cannot pop off genesis block");
                }
                BlockImpl previousBlock = BlockDb.findBlock(block.getPreviousBlockId());
                blockListeners.notify(block, Event.BEFORE_BLOCK_UNDO);
                blockchain.setLastBlock(block, previousBlock);
                transactionProcessor.undo(block);
                BlockDb.deleteBlocksFrom(block.getId());
                blockListeners.notify(block, Event.BLOCK_POPPED);
                return previousBlock.getId();
            } // synchronized

        } catch (RuntimeException e) {
            Logger.logDebugMessage("Error popping last block: " + e.getMessage());
            throw new TransactionType.UndoNotSupportedException(e.getMessage());
        }
    }

    void generateBlock(String secretPhrase, Long nonce) {

        Set<TransactionImpl> sortedTransactions = new TreeSet<>();

        for (TransactionImpl transaction : transactionProcessor.getAllUnconfirmedTransactions()) {
            if (hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0)) {
                sortedTransactions.add(transaction);
            }
        }

        SortedMap<Long, TransactionImpl> newTransactions = new TreeMap<>();
        Map<TransactionType, Set<String>> duplicates = new HashMap<>();

        long totalAmountNQT = 0;
        long totalFeeNQT = 0;
        int payloadLength = 0;

        int blockTimestamp = Convert.getEpochTime();

        while (payloadLength <= Constants.MAX_PAYLOAD_LENGTH) {

            int prevNumberOfNewTransactions = newTransactions.size();

            for (TransactionImpl transaction : sortedTransactions) {

                int transactionLength = transaction.getSize();
                if (newTransactions.get(transaction.getId()) != null || payloadLength + transactionLength > Constants.MAX_PAYLOAD_LENGTH) {
                    continue;
                }

                if (transaction.getTimestamp() > blockTimestamp + 15 || (transaction.getExpiration() < blockTimestamp)) {
                    continue;
                }

                if (transaction.isDuplicate(duplicates)) {
                    continue;
                }

                try {
                    transaction.validateAttachment();
                } catch (NxtException.ValidationException e) {
                    continue;
                }

                newTransactions.put(transaction.getId(), transaction);
                payloadLength += transactionLength;
                totalAmountNQT += transaction.getAmountNQT();
                totalFeeNQT += transaction.getFeeNQT();

            }

            if (newTransactions.size() == prevNumberOfNewTransactions) {
                break;
            }
        }

        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        MessageDigest digest = Crypto.sha256();
        for (Transaction transaction : newTransactions.values()) {
            digest.update(transaction.getBytes());
        }

        byte[] payloadHash = digest.digest();

        BlockImpl previousBlock = blockchain.getLastBlock();
        if (previousBlock.getHeight() < Constants.ASSET_EXCHANGE_BLOCK) {
            return;
        }

        ByteBuffer gensigbuf = ByteBuffer.allocate(32 + 8);
        gensigbuf.put(previousBlock.getGenerationSignature());
        gensigbuf.putLong(previousBlock.getGeneratorId());
        
        Shabal256 md = new Shabal256();
        md.update(gensigbuf.array());
        byte[] generationSignature = md.digest();

        BlockImpl block;
        int version = previousBlock.getHeight() < Constants.NQT_BLOCK ? 2 : 3;
        byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());

        try {

            block = new BlockImpl(version, blockTimestamp, previousBlock.getId(), totalAmountNQT, totalFeeNQT, payloadLength,
                        payloadHash, publicKey, generationSignature, null, previousBlockHash, new ArrayList<>(newTransactions.values()), nonce);

        } catch (NxtException.ValidationException e) {
            // shouldn't happen because all transactions are already validated
            Logger.logMessage("Error generating block", e);
            return;
        }

        block.sign(secretPhrase);

        if (isScanning) {
            return;
        }

        block.setPrevious(previousBlock);

        try {
            pushBlock(block);
            blockListeners.notify(block, Event.BLOCK_GENERATED);
            Logger.logDebugMessage("Account " + Convert.toUnsignedLong(block.getGeneratorId()) + " generated block " + block.getStringId());
        } catch (TransactionNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
            Transaction transaction = e.getTransaction();
            Logger.logDebugMessage("Removing invalid transaction: " + transaction.getStringId());
            transactionProcessor.removeUnconfirmedTransactions(Collections.singletonList((TransactionImpl)transaction));
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
        }

    }

    private BlockImpl parseBlock(JSONObject blockData) throws NxtException.ValidationException {
        int version = ((Long)blockData.get("version")).intValue();
        int timestamp = ((Long)blockData.get("timestamp")).intValue();
        Long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
        long totalAmountNQT = ((Long)blockData.get("totalAmountNQT"));
        long totalFeeNQT = ((Long)blockData.get("totalFeeNQT"));
        int payloadLength = ((Long)blockData.get("payloadLength")).intValue();
        byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
        byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
        byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
        byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
        byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
        Long nonce = Convert.parseUnsignedLong((String)blockData.get("nonce"));

        SortedMap<Long, TransactionImpl> blockTransactions = new TreeMap<>();
        JSONArray transactionsData = (JSONArray)blockData.get("transactions");
        for (Object transactionData : transactionsData) {
            TransactionImpl transaction = transactionProcessor.parseTransaction((JSONObject) transactionData);
            if (blockTransactions.put(transaction.getId(), transaction) != null) {
                throw new NxtException.ValidationException("Block contains duplicate transactions: " + transaction.getStringId());
            }
        }

        return new BlockImpl(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey,
                generationSignature, blockSignature, previousBlockHash, new ArrayList<>(blockTransactions.values()), nonce);
    }

    private boolean hasAllReferencedTransactions(Transaction transaction, int timestamp, int count) {
        if (transaction.getReferencedTransactionFullHash() == null) {
            return timestamp - transaction.getTimestamp() < 60 * 1440 * 60 && count < 10;
        }
        transaction = TransactionDb.findTransactionByFullHash(transaction.getReferencedTransactionFullHash());
        return transaction != null && hasAllReferencedTransactions(transaction, timestamp, count + 1);
    }

    private volatile boolean validateAtScan = Nxt.getBooleanProperty("nxt.forceValidate");

    void validateAtNextScan() {
        validateAtScan = true;
    }

    private void scan() {
        synchronized (blockchain) {
            isScanning = true;
            Logger.logMessage("Scanning blockchain...");
            if (validateAtScan) {
                Logger.logDebugMessage("Also verifying signatures and validating transactions...");
            }
            Account.clear();
            Alias.clear();
            Asset.clear();
            Order.clear();
            Poll.clear();
            Trade.clear();
            Vote.clear();
            DigitalGoodsStore.clear();
            transactionProcessor.clear();
            Generator.clear();
            blockchain.setLastBlock(BlockDb.findBlock(Genesis.GENESIS_BLOCK_ID));
            //Account.addOrGetAccount(Genesis.CREATOR_ID).apply(Genesis.CREATOR_PUBLIC_KEY, 0);
            try (Connection con = Db.getConnection(); PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id ASC")) {
                Long currentBlockId = Genesis.GENESIS_BLOCK_ID;
                BlockImpl currentBlock;
                ResultSet rs = pstmt.executeQuery();
                try {
                    while (rs.next()) {
                        currentBlock = BlockDb.loadBlock(con, rs);
                        if (! currentBlock.getId().equals(currentBlockId)) {
                        	if(currentBlockId == Genesis.GENESIS_BLOCK_ID) {
                        		Logger.logDebugMessage("Wrong genesis block id set. Should be: " + currentBlock.getId().toString());
                        	}
                            throw new NxtException.ValidationException("Database blocks in the wrong order!");
                        }
                        if (validateAtScan && ! currentBlockId.equals(Genesis.GENESIS_BLOCK_ID)) {
                            if (!currentBlock.verifyBlockSignature() || !currentBlock.verifyGenerationSignature()) {
                                throw new NxtException.ValidationException("Invalid block signature");
                            }
                            for (TransactionImpl transaction : currentBlock.getTransactions()) {
                                if (!transaction.verify()) {
                                    throw new NxtException.ValidationException("Invalid transaction signature");
                                }
                                transaction.validateAttachment();
                            }
                        }
                        for (TransactionImpl transaction : currentBlock.getTransactions()) {
                            if (! transaction.applyUnconfirmed()) {
                                throw new TransactionNotAcceptedException("Double spending transaction: "
                                        + transaction.getStringId(), transaction);
                            }
                        }
                        blockchain.setLastBlock(currentBlock);
                        blockListeners.notify(currentBlock, Event.BEFORE_BLOCK_APPLY);
                        currentBlock.apply();
                        for (TransactionImpl transaction : currentBlock.getTransactions()) {
                            transaction.apply();
                        }
                        blockListeners.notify(currentBlock, Event.AFTER_BLOCK_APPLY);
                        blockListeners.notify(currentBlock, Event.BLOCK_SCANNED);
                        currentBlockId = currentBlock.getNextBlockId();
                    }
                } catch (NxtException|RuntimeException e) {
                    Logger.logDebugMessage(e.toString(), e);
                    Logger.logDebugMessage("Applying block " + Convert.toUnsignedLong(currentBlockId) + " failed, deleting from database");
                    BlockDb.deleteBlocksFrom(currentBlockId);
                    scan();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            validateAtScan = false;
            Logger.logMessage("...done");
            isScanning = false;
        } // synchronized
    }

}
