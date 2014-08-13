package nxt.user;

import nxt.Account;
import nxt.Block;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import static nxt.user.JSONResponses.LOCK_ACCOUNT;

public final class UnlockAccount extends UserServlet.UserRequestHandler {

    static final UnlockAccount instance = new UnlockAccount();

    private UnlockAccount() {}

    private static final Comparator<JSONObject> myTransactionsComparator = new Comparator<JSONObject>() {
        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            int t1 = ((Number)o1.get("timestamp")).intValue();
            int t2 = ((Number)o2.get("timestamp")).intValue();
            if (t1 < t2) {
                return 1;
            }
            if (t1 > t2) {
                return -1;
            }
            String id1 = (String)o1.get("id");
            String id2 = (String)o2.get("id");
            return id2.compareTo(id1);
        }
    };

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        String secretPhrase = req.getParameter("secretPhrase");
        // lock all other instances of this account being unlocked
        for (User u : Users.getAllUsers()) {
            if (secretPhrase.equals(u.getSecretPhrase())) {
                u.lockAccount();
                if (! u.isInactive()) {
                    u.enqueue(LOCK_ACCOUNT);
                }
            }
        }

        Long accountId = user.unlockAccount(secretPhrase);

        JSONObject response = new JSONObject();
        response.put("response", "unlockAccount");
        response.put("account", Convert.toUnsignedLong(accountId));

        if (secretPhrase.length() < 30) {

            response.put("secretPhraseStrength", 1);

        } else {

            response.put("secretPhraseStrength", 5);

        }

        Account account = Account.getAccount(accountId);
        if (account == null) {

            response.put("balanceNQT", 0);

        } else {

            response.put("balanceNQT", account.getUnconfirmedBalanceNQT());

            JSONArray myTransactions = new JSONArray();
            byte[] accountPublicKey = account.getPublicKey();
            for (Transaction transaction : Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {

                if (Arrays.equals(transaction.getSenderPublicKey(), accountPublicKey)) {

                    JSONObject myTransaction = new JSONObject();
                    myTransaction.put("index", Users.getIndex(transaction));
                    myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    myTransaction.put("deadline", transaction.getDeadline());
                    myTransaction.put("account", Convert.toUnsignedLong(transaction.getRecipientId()));
                    myTransaction.put("sentAmountNQT", transaction.getAmountNQT());
                    if (accountId.equals(transaction.getRecipientId())) {
                        myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());
                    }
                    myTransaction.put("feeNQT", transaction.getFeeNQT());
                    myTransaction.put("numberOfConfirmations", -1);
                    myTransaction.put("id", transaction.getStringId());

                    myTransactions.add(myTransaction);

                } else if (accountId.equals(transaction.getRecipientId())) {

                    JSONObject myTransaction = new JSONObject();
                    myTransaction.put("index", Users.getIndex(transaction));
                    myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    myTransaction.put("deadline", transaction.getDeadline());
                    myTransaction.put("account", Convert.toUnsignedLong(transaction.getSenderId()));
                    myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());
                    myTransaction.put("feeNQT", transaction.getFeeNQT());
                    myTransaction.put("numberOfConfirmations", -1);
                    myTransaction.put("id", transaction.getStringId());

                    myTransactions.add(myTransaction);

                }

            }

            SortedSet<JSONObject> myTransactionsSet = new TreeSet<>(myTransactionsComparator);

            int blockchainHeight = Nxt.getBlockchain().getLastBlock().getHeight();
            try (DbIterator<? extends Block> blockIterator = Nxt.getBlockchain().getBlocks(account, 0)) {
                while (blockIterator.hasNext()) {
                    Block block = blockIterator.next();
                    if (block.getTotalFeeNQT() > 0) {
                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", "block" + Users.getIndex(block));
                        myTransaction.put("blockTimestamp", block.getTimestamp());
                        myTransaction.put("block", block.getStringId());
                        myTransaction.put("earnedAmountNQT", block.getTotalFeeNQT());
                        myTransaction.put("numberOfConfirmations", blockchainHeight - block.getHeight());
                        myTransaction.put("id", "-");
                        myTransaction.put("timestamp", block.getTimestamp());
                        myTransactionsSet.add(myTransaction);
                    }
                }
            }

            try (DbIterator<? extends Transaction> transactionIterator = Nxt.getBlockchain().getTransactions(account, (byte) -1, (byte) -1, 0, null)) {
                while (transactionIterator.hasNext()) {
                    Transaction transaction = transactionIterator.next();
                    if (transaction.getSenderId().equals(accountId)) {
                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", Users.getIndex(transaction));
                        myTransaction.put("blockTimestamp", transaction.getBlockTimestamp());
                        myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                        myTransaction.put("account", Convert.toUnsignedLong(transaction.getRecipientId()));
                        myTransaction.put("sentAmountNQT", transaction.getAmountNQT());
                        if (accountId.equals(transaction.getRecipientId())) {
                            myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());
                        }
                        myTransaction.put("feeNQT", transaction.getFeeNQT());
                        myTransaction.put("numberOfConfirmations", blockchainHeight - transaction.getHeight());
                        myTransaction.put("id", transaction.getStringId());
                        myTransaction.put("timestamp", transaction.getTimestamp());
                        myTransactionsSet.add(myTransaction);
                    } else if (transaction.getRecipientId().equals(accountId)) {
                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", Users.getIndex(transaction));
                        myTransaction.put("blockTimestamp", transaction.getBlockTimestamp());
                        myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                        myTransaction.put("account", Convert.toUnsignedLong(transaction.getSenderId()));
                        myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());
                        myTransaction.put("feeNQT", transaction.getFeeNQT());
                        myTransaction.put("numberOfConfirmations", blockchainHeight - transaction.getHeight());
                        myTransaction.put("id", transaction.getStringId());
                        myTransaction.put("timestamp", transaction.getTimestamp());
                        myTransactionsSet.add(myTransaction);
                    }
                }
            }

            Iterator<JSONObject> iterator = myTransactionsSet.iterator();
            while (myTransactions.size() < 1000 && iterator.hasNext()) {
                myTransactions.add(iterator.next());
            }

            if (myTransactions.size() > 0) {
                JSONObject response2 = new JSONObject();
                response2.put("response", "processNewData");
                response2.put("addedMyTransactions", myTransactions);
                user.enqueue(response2);
            }
        }
        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
