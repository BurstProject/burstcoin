package nxt.db.mariadb;

import nxt.db.sql.Db;
import nxt.db.store.*;

public class MariadbStores implements Stores {
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

    public MariadbStores() {

        this.accountStore = new MariadbAccountStore();
        this.aliasStore = new MariadbAliasStore();
        this.assetStore = new MariadbAssetStore();
        this.assetTransferStore = new MariadbAssetTransferStore();
        this.atStore = new MariadbATStore();
        this.blockchainStore = new MariadbBlockchainStore();
        this.digitalGoodsStoreStore = new MariadbDigitalGoodsStoreStore();
        this.escrowStore = new MariadbEscrowStore();
        this.orderStore = new MariadbOrderStore();
        this.pollStore = new MariadbPollStore();
        this.tradeStore = new MariadbTradeStore();
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
}
