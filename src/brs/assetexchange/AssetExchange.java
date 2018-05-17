package brs.assetexchange;

import brs.Account.AccountAsset;
import brs.Asset;
import brs.AssetTransfer;
import brs.Attachment.ColoredCoinsAskOrderPlacement;
import brs.Attachment.ColoredCoinsAssetIssuance;
import brs.Attachment.ColoredCoinsAssetTransfer;
import brs.Attachment.ColoredCoinsBidOrderPlacement;
import brs.Order;
import brs.Order.Ask;
import brs.Order.Bid;
import brs.Trade;
import brs.Trade.Event;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.util.Listener;

public interface AssetExchange {

  BurstIterator<Asset> getAllAssets(int from, int to);

  Asset getAsset(long assetId);

  int getTradeCount(long assetId);

  int getTransferCount(long id);

  int getAssetAccountsCount(long id);

  void addTradeListener(Listener<Trade> listener, Event trade);

  Ask getAskOrder(long orderId);

  void addAsset(Transaction transaction, ColoredCoinsAssetIssuance attachment);

  void addAssetTransfer(Transaction transaction, ColoredCoinsAssetTransfer attachment);

  void addAskOrder(Transaction transaction, ColoredCoinsAskOrderPlacement attachment);

  void addBidOrder(Transaction transaction, ColoredCoinsBidOrderPlacement attachment);

  void removeAskOrder(long orderId);

  Order.Bid getBidOrder(long orderId);

  void removeBidOrder(long orderId);

  BurstIterator<Trade> getAllTrades(int i, int i1);

  BurstIterator<Trade> getTrades(long assetId, int from, int to);

  BurstIterator<Trade> getAccountTrades(long accountId, int from, int to);

  BurstIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

  BurstIterator<AccountAsset> getAccountAssetsOverview(long accountId, int height, int from, int to);

  BurstIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);

  BurstIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to);

  BurstIterator<AssetTransfer> getAccountAssetTransfers(long id, long id1, int from, int to);

  int getAssetsCount();

  int getAskCount();

  int getBidCount();

  int getTradesCount();

  int getAssetTransferCount();

  BurstIterator<Ask> getAskOrdersByAccount(long accountId, int from, int to);

  BurstIterator<Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to);

  BurstIterator<Bid> getBidOrdersByAccount(long accountId, int from, int to);

  BurstIterator<Bid> getBidOrdersByAccountAsset(long accountId, long assetId, int from, int to);

  BurstIterator<Ask> getAllAskOrders(int from, int to);

  BurstIterator<Bid> getAllBidOrders(int from, int to);

  BurstIterator<Ask> getSortedAskOrders(long assetId, int from, int to);

  BurstIterator<Bid> getSortedBidOrders(long assetId, int from, int to);

}
