package nxt.db.store;


public final class Stores {

    private final AccountStore accountStore;


    public Stores(AccountStore accountStore) {
        this.accountStore = accountStore;
    }

    public AccountStore getAccountStore() {
        return accountStore;
    }
}
