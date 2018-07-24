package brs;

import brs.unconfirmedtransactions.TimedUnconfirmedTransactionOverview;
import brs.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface TransactionProcessor extends Observable<List<? extends Transaction>,TransactionProcessor.Event> {

  enum Event {
    REMOVED_UNCONFIRMED_TRANSACTIONS,
    ADDED_UNCONFIRMED_TRANSACTIONS,
    ADDED_CONFIRMED_TRANSACTIONS,
    ADDED_DOUBLESPENDING_TRANSACTIONS
  }

  TimedUnconfirmedTransactionOverview getAllUnconfirmedTransactions(Long lastUnconfirmedTransactionTimestamp, int limit);
  
  Transaction getUnconfirmedTransaction(long transactionId);

  void clearUnconfirmedTransactions();

  void broadcast(Transaction transaction) throws BurstException.ValidationException;

  void processPeerTransactions(JSONObject request) throws BurstException.ValidationException;

  Transaction parseTransaction(byte[] bytes) throws BurstException.ValidationException;

  Transaction parseTransaction(JSONObject json) throws BurstException.ValidationException;

  Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment);

}
