package nxt.db.mariadb;

import nxt.db.store.AccountStore;
import nxt.db.store.Stores;

public class MariadbStores implements Stores {
    private final AccountStore accountStore;


    public MariadbStores() {
        this.accountStore = new MariadbAccountStore();

    }

    public AccountStore getAccountStore() {
        return accountStore;
    }
}
