package nxt.db.mariadb;

import nxt.db.sql.SqlAssetStore;
import nxt.db.store.AccountStore;
import nxt.db.store.AliasStore;
import nxt.db.store.Stores;

public class MariadbStores implements Stores {
    private final AccountStore accountStore;
    private final AliasStore aliasStore;

    private final SqlAssetStore assetStore;

    public MariadbStores() {
        this.accountStore = new MariadbAccountStore();
        this.aliasStore = new MariadbAliasStore();
        this.assetStore = new MariadbAssetStore();
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
    public SqlAssetStore getAssetStore() {
        return assetStore;
    }

}
