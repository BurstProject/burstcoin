package brs.db.firebird;

import brs.db.sql.Db;
import brs.db.store.*;

public class FirebirdStores implements Stores {
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

    public FirebirdStores() {

        this.accountStore = new FirebirdAccountStore();
        this.aliasStore = new FirebirdAliasStore();
        this.assetStore = new FirebirdAssetStore();
        this.assetTransferStore = new FirebirdAssetTransferStore();
        this.atStore = new FirebirdATStore();
        this.blockchainStore = new FirebirdBlockchainStore();
        this.digitalGoodsStoreStore = new FirebirdDigitalGoodsStoreStore();
        this.escrowStore = new FirebirdEscrowStore();
        this.orderStore = new FirebirdOrderStore();
        this.pollStore = new FirebirdPollStore();
        this.tradeStore = new FirebirdTradeStore();
        this.voteStore = new FirebirdVoteStore();
        this.transactionProcessorStore = new FirebirdTransactionProcessorStore();
        this.subscriptionStore = new FirebirdSubscriptionStore();
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
