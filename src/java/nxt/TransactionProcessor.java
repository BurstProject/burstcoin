package nxt;

import nxt.db.DbIterator;
import nxt.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface TransactionProcessor extends Observable<List<? extends Transaction>,TransactionProcessor.Event> {

    public static enum Event {
        REMOVED_UNCONFIRMED_TRANSACTIONS,
        ADDED_UNCONFIRMED_TRANSACTIONS,
        ADDED_CONFIRMED_TRANSACTIONS,
        ADDED_DOUBLESPENDING_TRANSACTIONS
    }

    DbIterator<? extends Transaction> getAllUnconfirmedTransactions();

    Transaction getUnconfirmedTransaction(long transactionId);

    void broadcast(Transaction transaction) throws NxtException.ValidationException;

    void processPeerTransactions(JSONObject request) throws NxtException.ValidationException;

    Transaction parseTransaction(byte[] bytes) throws NxtException.ValidationException;

    Transaction parseTransaction(JSONObject json) throws NxtException.ValidationException;

    Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment);

}
