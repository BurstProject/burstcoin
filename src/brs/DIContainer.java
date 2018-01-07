package brs;

/**
 * Holds singleton services
 */
public class DIContainer {

  private DIContainer(){
  }

  public static TransactionProcessor getTransactionProcessor() {
    return TransactionProcessorImpl.getInstance();
  }

}
