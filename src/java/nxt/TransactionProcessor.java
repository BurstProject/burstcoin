package nxt;

import nxt.util.Observable;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;

public interface TransactionProcessor extends Observable<List<Transaction>,TransactionProcessor.Event> {

    public static enum Event {
        REMOVED_UNCONFIRMED_TRANSACTIONS,
        ADDED_UNCONFIRMED_TRANSACTIONS,
        ADDED_CONFIRMED_TRANSACTIONS,
        ADDED_DOUBLESPENDING_TRANSACTIONS
    }

    Collection<? extends Transaction> getAllUnconfirmedTransactions();

    Transaction getUnconfirmedTransaction(Long transactionId);

    void broadcast(Transaction transaction) throws NxtException.ValidationException;

    void processPeerTransactions(JSONObject request) throws NxtException.ValidationException;

    Transaction parseTransaction(byte[] bytes) throws NxtException.ValidationException;

    Transaction parseTransaction(JSONObject json) throws NxtException.ValidationException;

    Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment)
            throws NxtException.ValidationException;

}
