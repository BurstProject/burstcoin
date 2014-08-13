package nxt;

import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DebugTrace {

    static final String QUOTE = Nxt.getStringProperty("nxt.debugTraceQuote", "");
    static final String SEPARATOR = Nxt.getStringProperty("nxt.debugTraceSeparator", "\t");
    static final boolean LOG_UNCONFIRMED = Nxt.getBooleanProperty("nxt.debugLogUnconfirmed");

    static void init() {
        List<String> accountIds = Nxt.getStringListProperty("nxt.debugTraceAccounts");
        String logName = Nxt.getStringProperty("nxt.debugTraceLog");
        if (accountIds.isEmpty() || logName == null) {
            return;
        }
        addDebugTrace(accountIds, logName);
        Logger.logDebugMessage("Debug tracing of " + (accountIds.contains("*") ? "ALL"
                : String.valueOf(accountIds.size())) + " accounts enabled");
    }

    public static void addDebugTrace(List<String> accountIds, String logName) {
        final DebugTrace debugTrace = new DebugTrace(accountIds, logName);
        final Map<String, String> headers = new HashMap<>();
        for (String entry : columns) {
            headers.put(entry, entry);
        }
        debugTrace.log(headers);
        Trade.addListener(new Listener<Trade>() {
            @Override
            public void notify(Trade trade) {
                debugTrace.trace(trade);
            }
        }, Trade.Event.TRADE);
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                debugTrace.trace(account, false);
            }
        }, Account.Event.BALANCE);
        if (LOG_UNCONFIRMED) {
            Account.addListener(new Listener<Account>() {
                @Override
                public void notify(Account account) {
                    debugTrace.trace(account, true);
                }
            }, Account.Event.UNCONFIRMED_BALANCE);
        }
        Account.addAssetListener(new Listener<Account.AccountAsset>() {
            @Override
            public void notify(Account.AccountAsset accountAsset) {
                debugTrace.trace(accountAsset, false);
            }
        }, Account.Event.ASSET_BALANCE);
        if (LOG_UNCONFIRMED) {
            Account.addAssetListener(new Listener<Account.AccountAsset>() {
                @Override
                public void notify(Account.AccountAsset accountAsset) {
                    debugTrace.trace(accountAsset, true);
                }
            }, Account.Event.UNCONFIRMED_ASSET_BALANCE);
        }
        Account.addLeaseListener(new Listener<Account.AccountLease>() {
            @Override
            public void notify(Account.AccountLease accountLease) {
                debugTrace.trace(accountLease, true);
            }
        }, Account.Event.LEASE_STARTED);
        Account.addLeaseListener(new Listener<Account.AccountLease>() {
            @Override
            public void notify(Account.AccountLease accountLease) {
                debugTrace.trace(accountLease, false);
            }
        }, Account.Event.LEASE_ENDED);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.resetLog();
                debugTrace.log(headers);
            }
        }, BlockchainProcessor.Event.RESCAN_BEGIN);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.trace(block, false);
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.trace(block, true);
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_UNDO);
    }

    private static final String[] columns = {"height", "event", "account", "asset", "balance", "unconfirmed balance",
            "asset balance", "unconfirmed asset balance",
            "transaction amount", "transaction fee", "generation fee",
            "order", "order price", "order quantity", "order cost",
            "trade price", "trade quantity", "trade cost",
            "asset quantity", "transaction", "lessee", "lessor guaranteed balance",
            "sender", "recipient", "block", "timestamp"};

    private final Set<Long> accountIds;
    private final String logName;
    private PrintWriter log;

    private DebugTrace(List<String> accountIds, String logName) {
        this.accountIds = new HashSet<>();
        for (String accountId : accountIds) {
            if ("*".equals(accountId)) {
                this.accountIds.clear();
                break;
            }
            this.accountIds.add(Convert.parseUnsignedLong(accountId));
        }
        this.logName = logName;
        resetLog();
    }

    void resetLog() {
        if (log != null) {
            log.close();
        }
        try {
            log = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logName)))), true);
        } catch (IOException e) {
            Logger.logDebugMessage("Debug tracing to " + logName + " not possible", e);
            throw new RuntimeException(e);
        }
    }

    private boolean include(Long accountId) {
        return accountIds.isEmpty() || accountIds.contains(accountId);
    }

    // Note: Trade events occur before the change in account balances
    private void trace(Trade trade) {
        Long askAccountId = Order.Ask.getAskOrder(trade.getAskOrderId()).getAccount().getId();
        Long bidAccountId = Order.Bid.getBidOrder(trade.getBidOrderId()).getAccount().getId();
        if (include(askAccountId)) {
            log(getValues(askAccountId, trade, true));
        }
        if (include(bidAccountId)) {
            log(getValues(bidAccountId, trade, false));
        }
    }

    private void trace(Account account, boolean unconfirmed) {
        if (include(account.getId())) {
            log(getValues(account.getId(), unconfirmed));
        }
    }

    private void trace(Account.AccountAsset accountAsset, boolean unconfirmed) {
        if (! include(accountAsset.accountId)) {
            return;
        }
        log(getValues(accountAsset.accountId, accountAsset, unconfirmed));
    }

    private void trace(Account.AccountLease accountLease, boolean start) {
        if (! include(accountLease.lesseeId) && ! include(accountLease.lessorId)) {
            return;
        }
        log(getValues(accountLease.lessorId, accountLease, start));
    }

    private void trace(Block block, boolean isUndo) {
        Long generatorId = block.getGeneratorId();
        if (include(generatorId)) {
            log(getValues(generatorId, block, isUndo));
        }
        for (Long accountId : accountIds) {
            Account account = Account.getAccount(accountId);
            if (account != null) {
                for (Long lessorId : account.getLessorIds()) {
                    log(lessorGuaranteedBalance(lessorId, accountId));
                }
            }
        }
        for (Transaction transaction : block.getTransactions()) {
            Long senderId = transaction.getSenderId();
            if (include(senderId)) {
                log(getValues(senderId, transaction, false, isUndo));
                log(getValues(senderId, transaction, transaction.getAttachment(), false, isUndo));
            }
            Long recipientId = transaction.getRecipientId();
            if (include(recipientId)) {
                log(getValues(recipientId, transaction, true, isUndo));
                log(getValues(recipientId, transaction, transaction.getAttachment(), true, isUndo));
            }
        }
    }

    private Map<String,String> lessorGuaranteedBalance(Long accountId, Long lesseeId) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        Account account = Account.getAccount(accountId);
        // use 1441 instead of 1440 as at this point the newly generated block has already been pushed
        map.put("lessor guaranteed balance", String.valueOf(account.getGuaranteedBalanceNQT(1441)));
        map.put("lessee", Convert.toUnsignedLong(lesseeId));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getLastBlock().getHeight()));
        map.put("event", "lessor guaranteed balance");
        return map;
    }

    private Map<String,String> getValues(Long accountId, boolean unconfirmed) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        Account account = Account.getAccount(accountId);
        map.put("balance", String.valueOf(account != null ? account.getBalanceNQT() : 0));
        map.put("unconfirmed balance", String.valueOf(account != null ? account.getUnconfirmedBalanceNQT() : 0));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getLastBlock().getHeight()));
        map.put("event", unconfirmed ? "unconfirmed balance" : "balance");
        return map;
    }

    private Map<String,String> getValues(Long accountId, Trade trade, boolean isAsk) {
        Map<String,String> map = getValues(accountId, false);
        map.put("asset", Convert.toUnsignedLong(trade.getAssetId()));
        map.put("trade quantity", String.valueOf(isAsk ? - trade.getQuantityQNT() : trade.getQuantityQNT()));
        map.put("trade price", String.valueOf(trade.getPriceNQT()));
        long tradeCost = Convert.safeMultiply(trade.getQuantityQNT(), trade.getPriceNQT());
        map.put("trade cost", String.valueOf((isAsk ? tradeCost : - tradeCost)));
        map.put("event", "trade");
        return map;
    }

    private Map<String,String> getValues(Long accountId, Transaction transaction, boolean isRecipient, boolean isUndo) {
        long amount = transaction.getAmountNQT();
        long fee = transaction.getFeeNQT();
        if (isRecipient) {
            fee = 0; // fee doesn't affect recipient account
            if (isUndo) {
                amount = - amount;
            }
        } else {
            // for sender the amounts are subtracted
            if (! isUndo) {
                amount = - amount;
                fee = - fee;
            }
        }
        if (fee == 0 && amount == 0) {
            return Collections.emptyMap();
        }
        Map<String,String> map = getValues(accountId, false);
        map.put("transaction amount", String.valueOf(amount));
        map.put("transaction fee", String.valueOf(fee));
        map.put("transaction", transaction.getStringId());
        if (isRecipient) {
            map.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
        } else {
            map.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
        }
        map.put("event", "transaction" + (isUndo ? " undo" : ""));
        return map;
    }

    private Map<String,String> getValues(Long accountId, Block block, boolean isUndo) {
        long fee = block.getTotalFeeNQT();
        if (fee == 0) {
            return Collections.emptyMap();
        }
        if (isUndo) {
            fee = - fee;
        }
        Map<String,String> map = getValues(accountId, false);
        map.put("generation fee", String.valueOf(fee));
        map.put("block", block.getStringId());
        map.put("event", "block" + (isUndo ? " undo" : ""));
        return map;
    }

    private Map<String,String> getValues(Long accountId, Account.AccountAsset accountAsset, boolean unconfirmed) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        map.put("asset", Convert.toUnsignedLong(accountAsset.assetId));
        map.put(unconfirmed ? "unconfirmed asset balance" : "asset balance", String.valueOf(Convert.nullToZero(accountAsset.quantityQNT)));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getLastBlock().getHeight()));
        map.put("event", "asset balance");
        return map;
    }

    private Map<String,String> getValues(Long accountId, Account.AccountLease accountLease, boolean start) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        map.put("event", start ? "lease begin" : "lease end");
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getLastBlock().getHeight()));
        map.put("lessee", Convert.toUnsignedLong(accountLease.lesseeId));
        return map;
    }

    private Map<String,String> getValues(Long accountId, Transaction transaction, Attachment attachment, boolean isRecipient, boolean isUndo) {
        Map<String,String> map = getValues(accountId, false);
        if (attachment instanceof Attachment.ColoredCoinsOrderPlacement) {
            if (isRecipient) {
                return Collections.emptyMap();
            }
            Attachment.ColoredCoinsOrderPlacement orderPlacement = (Attachment.ColoredCoinsOrderPlacement)attachment;
            boolean isAsk = orderPlacement instanceof Attachment.ColoredCoinsAskOrderPlacement;
            map.put("asset", Convert.toUnsignedLong(orderPlacement.getAssetId()));
            map.put("order", transaction.getStringId());
            map.put("order price", String.valueOf(orderPlacement.getPriceNQT()));
            long quantity = orderPlacement.getQuantityQNT();
            if (isAsk) {
                if (! isUndo) {
                    quantity = - quantity;
                }
            } else {
                if (isUndo) {
                    quantity = - quantity;
                }
            }
            map.put("order quantity", String.valueOf(quantity));
            BigInteger orderCost = BigInteger.valueOf(orderPlacement.getPriceNQT()).multiply(BigInteger.valueOf(orderPlacement.getQuantityQNT()));
            if (isAsk) {
                if (isUndo) {
                    orderCost = orderCost.negate();
                }
            } else {
                if (! isUndo) {
                    orderCost = orderCost.negate();
                }
            }
            map.put("order cost", orderCost.toString());
            String event = (isAsk ? "ask" : "bid") + " order" + (isUndo ? " undo" : "");
            map.put("event", event);
        } else if (attachment instanceof Attachment.ColoredCoinsAssetIssuance) {
            if (isRecipient) {
                return Collections.emptyMap();
            }
            Attachment.ColoredCoinsAssetIssuance assetIssuance = (Attachment.ColoredCoinsAssetIssuance)attachment;
            map.put("asset", transaction.getStringId());
            map.put("asset quantity", String.valueOf(isUndo ? -assetIssuance.getQuantityQNT() : assetIssuance.getQuantityQNT()));
            map.put("event", "asset issuance" + (isUndo ? " undo" : ""));
        } else if (attachment instanceof Attachment.ColoredCoinsAssetTransfer) {
            Attachment.ColoredCoinsAssetTransfer assetTransfer = (Attachment.ColoredCoinsAssetTransfer)attachment;
            map.put("asset", Convert.toUnsignedLong(assetTransfer.getAssetId()));
            long quantity = assetTransfer.getQuantityQNT();
            if (isRecipient) {
                if (isUndo) {
                    quantity = - quantity;
                }
            } else {
                if (! isUndo) {
                    quantity = - quantity;
                }
            }
            map.put("asset quantity", String.valueOf(quantity));
            map.put("event", "asset transfer" + (isUndo ? " undo" : ""));
        } else if (attachment instanceof Attachment.ColoredCoinsOrderCancellation) {
            Attachment.ColoredCoinsOrderCancellation orderCancellation = (Attachment.ColoredCoinsOrderCancellation)attachment;
            map.put("order", Convert.toUnsignedLong(orderCancellation.getOrderId()));
            map.put("event", "order cancel");
        } else if (attachment instanceof Attachment.MessagingArbitraryMessage) {
            map = new HashMap<>();
            map.put("account", Convert.toUnsignedLong(accountId));
            map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
            map.put("height", String.valueOf(Nxt.getBlockchain().getLastBlock().getHeight()));
            map.put("event", "message");
            if (isRecipient) {
                map.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
            } else {
                map.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
            }
        } else {
            return Collections.emptyMap();
        }
        return map;
    }

    private void log(Map<String,String> map) {
        if (map.isEmpty()) {
            return;
        }
        StringBuilder buf = new StringBuilder();
        for (String column : columns) {
            if (!LOG_UNCONFIRMED && column.startsWith("unconfirmed")) {
                continue;
            }
            String value = map.get(column);
            if (value != null) {
                buf.append(QUOTE).append(value).append(QUOTE);
            }
            buf.append(SEPARATOR);
        }
        log.println(buf.toString());
    }

}
