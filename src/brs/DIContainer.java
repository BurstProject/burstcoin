package brs;

import brs.db.store.Stores;

/**
 * Holds singleton services
 */
public class DIContainer {

  private DIContainer() {
  }

  public static TransactionProcessor getTransactionProcessor() {
    return TransactionProcessorImpl.getInstance();
  }

  public static Blockchain getBlockchain() {
    return BlockchainImpl.getInstance();
  }

  public static BlockchainProcessor getBlockchainProcessor() {
    return BlockchainProcessorImpl.getInstance();
  }

  public static Stores getStores() {
    return Burst.getStores();
  }
}
