package nxt.db.store;


import nxt.db.sql.SqlAssetStore;

public interface Stores {

    AccountStore getAccountStore();

    AliasStore getAliasStore();

    SqlAssetStore getAssetStore();

    AssetTransferStore getAssetTransferStore();
}
