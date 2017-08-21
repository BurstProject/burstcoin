package nxt.db.mariadb;

import nxt.db.BlockDb;
import nxt.db.store.*;

public class MariadbStores implements Stores {
    private final AccountStore accountStore;
    private final AliasStore aliasStore;
    private final AssetTransferStore assetTransferStore;
    private final AssetStore assetStore;
    private final ATStore atStore;


    public MariadbStores() {

        this.accountStore = new MariadbAccountStore();
        this.aliasStore = new MariadbAliasStore();
        this.assetStore = new MariadbAssetStore();
        this.assetTransferStore = new MariadbAssetTransferStore();
        this.atStore = new MariadbATStore();
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

}
