package brs.db.store;

import brs.db.sql.*;
import brs.services.TimeService;

public class Stores {
  private final AccountStore accountStore;
  private final AliasStore aliasStore;
  private final AssetTransferStore assetTransferStore;
  private final AssetStore assetStore;
  private final ATStore atStore;
  private final BlockchainStore blockchainStore;
  private final DigitalGoodsStoreStore digitalGoodsStoreStore;
  private final EscrowStore escrowStore;
  private final OrderStore orderStore;
  private final TradeStore tradeStore;
  private final TransactionProcessorStore transactionProcessorStore;
  private final SubscriptionStore subscriptionStore;

  public Stores(DerivedTableManager derivedTableManager, TimeService timeService) {
    this.accountStore              = new SqlAccountStore(derivedTableManager);
    this.aliasStore                = new SqlAliasStore(derivedTableManager);
    this.assetStore                = new SqlAssetStore(derivedTableManager);
    this.assetTransferStore        = new SqlAssetTransferStore(derivedTableManager);
    this.atStore                   = new SqlATStore(derivedTableManager);
    this.blockchainStore           = new SqlBlockchainStore();
    this.digitalGoodsStoreStore    = new SqlDigitalGoodsStoreStore(derivedTableManager);
    this.escrowStore               = new SqlEscrowStore(derivedTableManager);
    this.orderStore                = new SqlOrderStore(derivedTableManager);
    this.tradeStore                = new SqlTradeStore(derivedTableManager);
    this.transactionProcessorStore = new SqlTransactionProcessorStore(derivedTableManager, timeService);
    this.subscriptionStore         = new SqlSubscriptionStore(derivedTableManager);
  }

  public AccountStore getAccountStore() {
    return accountStore;
  }

  public AliasStore getAliasStore() {
    return aliasStore;
  }

  public AssetStore getAssetStore() {
    return assetStore;
  }

  public AssetTransferStore getAssetTransferStore() {
    return assetTransferStore;
  }

  public ATStore getAtStore() {
    return atStore;
  }

  public BlockchainStore getBlockchainStore() {
    return blockchainStore;
  }

  public DigitalGoodsStoreStore getDigitalGoodsStoreStore() {
    return digitalGoodsStoreStore;
  }

  public void beginTransaction() {
    Db.beginTransaction();
  }

  public void commitTransaction() {
    Db.commitTransaction();
  }

  public void rollbackTransaction() {
    Db.rollbackTransaction();
  }

  public void endTransaction() {
    Db.endTransaction();
  }

  public boolean isInTransaction() {
    return Db.isInTransaction();
  }

  public EscrowStore getEscrowStore() {
    return escrowStore;
  }

  public OrderStore getOrderStore() {
    return orderStore;
  }

  public TradeStore getTradeStore() {
    return tradeStore;
  }

  public TransactionProcessorStore getTransactionProcessorStore() {
    return transactionProcessorStore;
  }

  public SubscriptionStore getSubscriptionStore() {
    return subscriptionStore;
  }

}
