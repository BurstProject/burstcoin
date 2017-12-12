package brs.db.store;


public interface Stores {

  AccountStore getAccountStore();

  AliasStore getAliasStore();

  AssetStore getAssetStore();

  AssetTransferStore getAssetTransferStore();

  ATStore getAtStore();

  BlockchainStore getBlockchainStore();

  DigitalGoodsStoreStore getDigitalGoodsStoreStore();

  EscrowStore getEscrowStore();

  OrderStore getOrderStore();

  TradeStore getTradeStore();

  TransactionProcessorStore getTransactionProcessorStore();

  SubscriptionStore getSubscriptionStore();

  void beginTransaction();

  void commitTransaction();

  void rollbackTransaction();
    
  void endTransaction();

  boolean isInTransaction();
}
