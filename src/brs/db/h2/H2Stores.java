package brs.db.h2;

import brs.db.sql.Db;
import brs.db.store.*;

public class H2Stores implements Stores {
  private final AccountStore accountStore;
  private final AliasStore aliasStore;
  private final AssetTransferStore assetTransferStore;
  private final AssetStore assetStore;
  private final ATStore atStore;
  private final BlockchainStore blockchainStore;
  private final DigitalGoodsStoreStore digitalGoodsStoreStore;
  private final EscrowStore escrowStore;
  private final OrderStore orderStore;
  private final PollStore pollStore;
  private final TradeStore tradeStore;
  private final VoteStore voteStore;
  private final TransactionProcessorStore transactionProcessorStore;
  private final SubscriptionStore subscriptionStore;

  public H2Stores() {

    this.accountStore = new H2AccountStore();
    this.aliasStore = new H2AliasStore();
    this.assetStore = new H2AssetStore();
    this.assetTransferStore = new H2AssetTransferStore();
    this.atStore = new H2ATStore();
    this.blockchainStore = new H2BlockchainStore();
    this.digitalGoodsStoreStore = new H2DigitalGoodsStoreStore();
    this.escrowStore = new H2EscrowStore();
    this.orderStore = new H2OrderStore();
    this.pollStore = new H2PollStore();
    this.tradeStore = new H2TradeStore();
    this.voteStore = new H2VoteStore();
    this.transactionProcessorStore = new H2TransactionProcessorStore();
    this.subscriptionStore = new H2SubscriptionStore();
  }

  @Override
  public AccountStore getAccountStore() {
    return accountStore;
  }

  @Override
  public AliasStore getAliasStore() {
    return aliasStore;
  }

  @Override
  public AssetStore getAssetStore() {
    return assetStore;
  }

  @Override
  public AssetTransferStore getAssetTransferStore() {
    return assetTransferStore;
  }

  @Override
  public ATStore getAtStore() {
    return atStore;
  }

  @Override
  public BlockchainStore getBlockchainStore() {
    return blockchainStore;
  }

  @Override
  public DigitalGoodsStoreStore getDigitalGoodsStoreStore() {
    return digitalGoodsStoreStore;
  }

  @Override
  public void beginTransaction() {
    Db.beginTransaction();
  }

  @Override
  public void commitTransaction() {
    Db.commitTransaction();
  }

  @Override
  public void rollbackTransaction() {
    Db.rollbackTransaction();
  }

  @Override
  public void endTransaction() {
    Db.endTransaction();
  }

  @Override
  public boolean isInTransaction() {
    return Db.isInTransaction();
  }

  @Override
  public EscrowStore getEscrowStore() {
    return escrowStore;
  }

  @Override
  public OrderStore getOrderStore() {
    return orderStore;
  }

  @Override
  public PollStore getPollStore() {
    return pollStore;
  }

  @Override
  public TradeStore getTradeStore() {
    return tradeStore;
  }

  @Override
  public VoteStore getVoteStore() {
    return voteStore;
  }

  @Override
  public TransactionProcessorStore getTransactionProcessorStore() {
    return transactionProcessorStore;
  }

  @Override
  public SubscriptionStore getSubscriptionStore() {
    return subscriptionStore;
  }
}
