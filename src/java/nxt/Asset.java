package nxt;

import nxt.util.Convert;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Asset {

    private static final ConcurrentMap<Long, Asset> assets = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, List<Asset>> accountAssets = new ConcurrentHashMap<>();
    private static final Collection<Asset> allAssets = Collections.unmodifiableCollection(assets.values());

    public static Collection<Asset> getAllAssets() {
        return allAssets;
    }

    public static Asset getAsset(Long id) {
        return assets.get(id);
    }

    public static List<Asset> getAssetsIssuedBy(Long accountId) {
        List<Asset> assets = accountAssets.get(accountId);
        if (assets == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(assets);
    }

    static void addAsset(Long assetId, Long senderAccountId, String name, String description, long quantityQNT, byte decimals) {
        Asset asset = new Asset(assetId, senderAccountId, name, description, quantityQNT, decimals);
        if (Asset.assets.putIfAbsent(assetId, asset) != null) {
            throw new IllegalStateException("Asset with id " + Convert.toUnsignedLong(assetId) + " already exists");
        }
        List<Asset> accountAssetsList = accountAssets.get(senderAccountId);
        if (accountAssetsList == null) {
            accountAssetsList = new CopyOnWriteArrayList<>();
            accountAssets.put(senderAccountId, accountAssetsList);
        }
        accountAssetsList.add(asset);
    }

    static void removeAsset(Long assetId) {
        Asset asset = Asset.assets.remove(assetId);
        List<Asset> accountAssetList = accountAssets.get(asset.getAccountId());
        accountAssetList.remove(asset);
    }

    static void clear() {
        Asset.assets.clear();
        Asset.accountAssets.clear();
    }

    private final Long assetId;
    private final Long accountId;
    private final String name;
    private final String description;
    private final long quantityQNT;
    private final byte decimals;

    private Asset(Long assetId, Long accountId, String name, String description, long quantityQNT, byte decimals) {
        this.assetId = assetId;
        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.quantityQNT = quantityQNT;
        this.decimals = decimals;
    }

    public Long getId() {
        return assetId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public byte getDecimals() {
        return decimals;
    }

}
