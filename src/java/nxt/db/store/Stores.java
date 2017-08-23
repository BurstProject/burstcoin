package nxt.db.store;


public interface Stores {

    AccountStore getAccountStore();

    AliasStore getAliasStore();

    AssetStore getAssetStore();

    AssetTransferStore getAssetTransferStore();

    ATStore getAtStore();

    BlockchainStore getBlockchainStore();
}
