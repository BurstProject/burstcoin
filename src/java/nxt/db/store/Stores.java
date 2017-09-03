package nxt.db.store;


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

    PollStore getPollStore();

    TradeStore getTradeStore();

    VoteStore getVoteStore();

    TransactionProcessorStore getTransactionProcessorStore();

    void beginTransaction();

    void commitTransaction();

    void rollbackTransaction();
    
    void endTransaction();

    boolean isInTransaction();
}
