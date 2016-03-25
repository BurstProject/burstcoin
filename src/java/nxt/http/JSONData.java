package nxt.http;

import nxt.AT;
import nxt.Account;
import nxt.Alias;
import nxt.Appendix;
import nxt.Asset;
import nxt.AssetTransfer;
import nxt.Block;
import nxt.DigitalGoodsStore;
import nxt.Constants;
import nxt.Escrow;
import nxt.Nxt;
import nxt.Order;
import nxt.Poll;
import nxt.Subscription;
import nxt.Token;
import nxt.Trade;
import nxt.Transaction;
import nxt.at.AT_API_Helper;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.peer.Hallmark;
import nxt.peer.Peer;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

public final class JSONData {

    static JSONObject alias(Alias alias) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", alias.getAccountId());
        json.put("aliasName", alias.getAliasName());
        json.put("aliasURI", alias.getAliasURI());
        json.put("timestamp", alias.getTimestamp());
        json.put("alias", Convert.toUnsignedLong(alias.getId()));
        Alias.Offer offer = Alias.getOffer(alias);
        if (offer != null) {
            json.put("priceNQT", String.valueOf(offer.getPriceNQT()));
            if (offer.getBuyerId() != 0) {
                json.put("buyer", Convert.toUnsignedLong(offer.getBuyerId()));
            }
        }
        return json;
    }

    static JSONObject accountBalance(Account account) {
        JSONObject json = new JSONObject();
        if (account == null) {
            json.put("balanceNQT", "0");
            json.put("unconfirmedBalanceNQT", "0");
            json.put("effectiveBalanceNXT", "0");
            json.put("forgedBalanceNQT", "0");
            json.put("guaranteedBalanceNQT", "0");
        } else {
            json.put("balanceNQT", String.valueOf(account.getBalanceNQT()));
            json.put("unconfirmedBalanceNQT", String.valueOf(account.getUnconfirmedBalanceNQT()));
            json.put("effectiveBalanceNXT", account.getBalanceNQT());
            json.put("forgedBalanceNQT", String.valueOf(account.getForgedBalanceNQT()));
            json.put("guaranteedBalanceNQT", String.valueOf(account.getBalanceNQT()));
        }
        return json;
    }

    static JSONObject asset(Asset asset) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", asset.getAccountId());
        json.put("name", asset.getName());
        json.put("description", asset.getDescription());
        json.put("decimals", asset.getDecimals());
        json.put("quantityQNT", String.valueOf(asset.getQuantityQNT()));
        json.put("asset", Convert.toUnsignedLong(asset.getId()));
        json.put("numberOfTrades", Trade.getTradeCount(asset.getId()));
        json.put("numberOfTransfers", AssetTransfer.getTransferCount(asset.getId()));
        json.put("numberOfAccounts", Account.getAssetAccountsCount(asset.getId()));
        return json;
    }
    
    static JSONObject accountAsset(Account.AccountAsset accountAsset) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", accountAsset.getAccountId());
        json.put("asset", Convert.toUnsignedLong(accountAsset.getAssetId()));
        json.put("quantityQNT", String.valueOf(accountAsset.getQuantityQNT()));
        json.put("unconfirmedQuantityQNT", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
        return json;
    }

    static JSONObject askOrder(Order.Ask order) {
        JSONObject json = order(order);
        json.put("type", "ask");
        return json;
    }

    static JSONObject bidOrder(Order.Bid order) {
        JSONObject json = order(order);
        json.put("type", "bid");
        return json;
    }

    static JSONObject order(Order order) {
        JSONObject json = new JSONObject();
        json.put("order", Convert.toUnsignedLong(order.getId()));
        json.put("asset", Convert.toUnsignedLong(order.getAssetId()));
        putAccount(json, "account", order.getAccountId());
        json.put("quantityQNT", String.valueOf(order.getQuantityQNT()));
        json.put("priceNQT", String.valueOf(order.getPriceNQT()));
        json.put("height", order.getHeight());
        return json;
    }

    static JSONObject block(Block block, boolean includeTransactions) {
        JSONObject json = new JSONObject();
        json.put("block", block.getStringId());
        json.put("height", block.getHeight());
        putAccount(json, "generator", block.getGeneratorId());
        json.put("generatorPublicKey", Convert.toHexString(block.getGeneratorPublicKey()));
        json.put("nonce", Convert.toUnsignedLong(block.getNonce()));
        json.put("scoopNum", block.getScoopNum());
        json.put("timestamp", block.getTimestamp());
        json.put("numberOfTransactions", block.getTransactions().size());
        json.put("totalAmountNQT", String.valueOf(block.getTotalAmountNQT()));
        json.put("totalFeeNQT", String.valueOf(block.getTotalFeeNQT()));
        json.put("blockReward", Convert.toUnsignedLong(block.getBlockReward() / Constants.ONE_NXT));
        json.put("payloadLength", block.getPayloadLength());
        json.put("version", block.getVersion());
        json.put("baseTarget", Convert.toUnsignedLong(block.getBaseTarget()));
        if (block.getPreviousBlockId() != 0) {
            json.put("previousBlock", Convert.toUnsignedLong(block.getPreviousBlockId()));
        }
        if (block.getNextBlockId() != 0) {
            json.put("nextBlock", Convert.toUnsignedLong(block.getNextBlockId()));
        }
        json.put("payloadHash", Convert.toHexString(block.getPayloadHash()));
        json.put("generationSignature", Convert.toHexString(block.getGenerationSignature()));
        if (block.getVersion() > 1) {
            json.put("previousBlockHash", Convert.toHexString(block.getPreviousBlockHash()));
        }
        json.put("blockSignature", Convert.toHexString(block.getBlockSignature()));
        JSONArray transactions = new JSONArray();
        for (Transaction transaction : block.getTransactions()) {
            transactions.add(includeTransactions ? transaction(transaction) : Convert.toUnsignedLong(transaction.getId()));
        }
        json.put("transactions", transactions);
        return json;
    }

    static JSONObject encryptedData(EncryptedData encryptedData) {
        JSONObject json = new JSONObject();
        json.put("data", Convert.toHexString(encryptedData.getData()));
        json.put("nonce", Convert.toHexString(encryptedData.getNonce()));
        return json;
    }
    
    static JSONObject escrowTransaction(Escrow escrow) {
    	JSONObject json = new JSONObject();
    	json.put("id", Convert.toUnsignedLong(escrow.getId()));
    	json.put("sender", Convert.toUnsignedLong(escrow.getSenderId()));
    	json.put("senderRS", Convert.rsAccount(escrow.getSenderId()));
    	json.put("recipient", Convert.toUnsignedLong(escrow.getRecipientId()));
    	json.put("recipientRS", Convert.rsAccount(escrow.getRecipientId()));
    	json.put("amountNQT", Convert.toUnsignedLong(Escrow.getEscrowTransaction(escrow.getId()).getAmountNQT()));
    	json.put("requiredSigners", escrow.getRequiredSigners());
    	json.put("deadline", escrow.getDeadline());
    	json.put("deadlineAction", Escrow.decisionToString(escrow.getDeadlineAction()));
		
		JSONArray signers = new JSONArray();
		for(Escrow.Decision decision : escrow.getDecisions()) {
			if(decision.getAccountId().equals(escrow.getSenderId()) ||
			   decision.getAccountId().equals(escrow.getRecipientId())) {
				continue;
			}
			JSONObject signerDetails = new JSONObject();
			signerDetails.put("id", Convert.toUnsignedLong(decision.getAccountId()));
			signerDetails.put("idRS", Convert.rsAccount(decision.getAccountId()));
			signerDetails.put("decision", Escrow.decisionToString(decision.getDecision()));
			signers.add(signerDetails);
		}
		json.put("signers", signers);
    	return json;
    }

    static JSONObject goods(DigitalGoodsStore.Goods goods) {
        JSONObject json = new JSONObject();
        json.put("goods", Convert.toUnsignedLong(goods.getId()));
        json.put("name", goods.getName());
        json.put("description", goods.getDescription());
        json.put("quantity", goods.getQuantity());
        json.put("priceNQT", String.valueOf(goods.getPriceNQT()));
        putAccount(json, "seller", goods.getSellerId());
        json.put("tags", goods.getTags());
        json.put("delisted", goods.isDelisted());
        json.put("timestamp", goods.getTimestamp());
        return json;
    }

    static JSONObject hallmark(Hallmark hallmark) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", Account.getId(hallmark.getPublicKey()));
        json.put("host", hallmark.getHost());
        json.put("weight", hallmark.getWeight());
        String dateString = Hallmark.formatDate(hallmark.getDate());
        json.put("date", dateString);
        json.put("valid", hallmark.isValid());
        return json;
    }

    static JSONObject token(Token token) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", Account.getId(token.getPublicKey()));
        json.put("timestamp", token.getTimestamp());
        json.put("valid", token.isValid());
        return json;
    }

    static JSONObject peer(Peer peer) {
        JSONObject json = new JSONObject();
        json.put("state", peer.getState().ordinal());
        json.put("announcedAddress", peer.getAnnouncedAddress());
        json.put("shareAddress", peer.shareAddress());
        if (peer.getHallmark() != null) {
            json.put("hallmark", peer.getHallmark().getHallmarkString());
        }
        json.put("weight", peer.getWeight());
        json.put("downloadedVolume", peer.getDownloadedVolume());
        json.put("uploadedVolume", peer.getUploadedVolume());
        json.put("application", peer.getApplication());
        json.put("version", peer.getVersion());
        json.put("platform", peer.getPlatform());
        json.put("blacklisted", peer.isBlacklisted());
        json.put("lastUpdated", peer.getLastUpdated());
        return json;
    }

    static JSONObject poll(Poll poll) {
        JSONObject json = new JSONObject();
        json.put("name", poll.getName());
        json.put("description", poll.getDescription());
        JSONArray options = new JSONArray();
        Collections.addAll(options, poll.getOptions());
        json.put("options", options);
        json.put("minNumberOfOptions", poll.getMinNumberOfOptions());
        json.put("maxNumberOfOptions", poll.getMaxNumberOfOptions());
        json.put("optionsAreBinary", poll.isOptionsAreBinary());
        JSONArray voters = new JSONArray();
        for (Long voterId : poll.getVoters().keySet()) {
            voters.add(Convert.toUnsignedLong(voterId));
        }
        json.put("voters", voters);
        return json;
    }

    static JSONObject purchase(DigitalGoodsStore.Purchase purchase) {
        JSONObject json = new JSONObject();
        json.put("purchase", Convert.toUnsignedLong(purchase.getId()));
        json.put("goods", Convert.toUnsignedLong(purchase.getGoodsId()));
        json.put("name", purchase.getName());
        putAccount(json, "seller", purchase.getSellerId());
        json.put("priceNQT", String.valueOf(purchase.getPriceNQT()));
        json.put("quantity", purchase.getQuantity());
        putAccount(json, "buyer", purchase.getBuyerId());
        json.put("timestamp", purchase.getTimestamp());
        json.put("deliveryDeadlineTimestamp", purchase.getDeliveryDeadlineTimestamp());
        if (purchase.getNote() != null) {
            json.put("note", encryptedData(purchase.getNote()));
        }
        json.put("pending", purchase.isPending());
        if (purchase.getEncryptedGoods() != null) {
            json.put("goodsData", encryptedData(purchase.getEncryptedGoods()));
            json.put("goodsIsText", purchase.goodsIsText());
        }
        if (purchase.getFeedbackNotes() != null) {
            JSONArray feedbacks = new JSONArray();
            for (EncryptedData encryptedData : purchase.getFeedbackNotes()) {
                feedbacks.add(encryptedData(encryptedData));
            }
            json.put("feedbackNotes", feedbacks);
        }
        if (purchase.getPublicFeedback() != null) {
            JSONArray publicFeedbacks = new JSONArray();
            for (String publicFeedback : purchase.getPublicFeedback()) {
                publicFeedbacks.add(publicFeedback);
            }
            json.put("publicFeedbacks", publicFeedbacks);
        }
        if (purchase.getRefundNote() != null) {
            json.put("refundNote", encryptedData(purchase.getRefundNote()));
        }
        if (purchase.getDiscountNQT() > 0) {
            json.put("discountNQT", String.valueOf(purchase.getDiscountNQT()));
        }
        if (purchase.getRefundNQT() > 0) {
            json.put("refundNQT", String.valueOf(purchase.getRefundNQT()));
        }
        return json;
    }
    
    static JSONObject subscription(Subscription subscription) {
    	JSONObject json = new JSONObject();
    	json.put("id", Convert.toUnsignedLong(subscription.getId()));
    	putAccount(json, "sender", subscription.getSenderId());
    	putAccount(json, "recipient", subscription.getRecipientId());
    	json.put("amountNQT", Convert.toUnsignedLong(subscription.getAmountNQT()));
    	json.put("frequency", subscription.getFrequency());
    	json.put("timeNext", subscription.getTimeNext());
    	return json;
    }

    static JSONObject trade(Trade trade, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        json.put("timestamp", trade.getTimestamp());
        json.put("quantityQNT", String.valueOf(trade.getQuantityQNT()));
        json.put("priceNQT", String.valueOf(trade.getPriceNQT()));
        json.put("asset", Convert.toUnsignedLong(trade.getAssetId()));
        json.put("askOrder", Convert.toUnsignedLong(trade.getAskOrderId()));
        json.put("bidOrder", Convert.toUnsignedLong(trade.getBidOrderId()));
        json.put("askOrderHeight", trade.getAskOrderHeight());
        json.put("bidOrderHeight", trade.getBidOrderHeight());
        putAccount(json, "seller", trade.getSellerId());
        putAccount(json, "buyer", trade.getBuyerId());
        json.put("block", Convert.toUnsignedLong(trade.getBlockId()));
        json.put("height", trade.getHeight());
        json.put("tradeType", trade.isBuy() ? "buy" : "sell");
        if (includeAssetInfo) {
            Asset asset = Asset.getAsset(trade.getAssetId());
            json.put("name", asset.getName());
            json.put("decimals", asset.getDecimals());
        }
        return json;
    }
    
    static JSONObject assetTransfer(AssetTransfer assetTransfer, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        json.put("assetTransfer", Convert.toUnsignedLong(assetTransfer.getId()));
        json.put("asset", Convert.toUnsignedLong(assetTransfer.getAssetId()));
        putAccount(json, "sender", assetTransfer.getSenderId());
        putAccount(json, "recipient", assetTransfer.getRecipientId());
        json.put("quantityQNT", String.valueOf(assetTransfer.getQuantityQNT()));
        json.put("height", assetTransfer.getHeight());
        json.put("timestamp", assetTransfer.getTimestamp());
        if (includeAssetInfo) {
            Asset asset = Asset.getAsset(assetTransfer.getAssetId());
            json.put("name", asset.getName());
            json.put("decimals", asset.getDecimals());
        }
        return json;
    }

    static JSONObject unconfirmedTransaction(Transaction transaction) {
        JSONObject json = new JSONObject();
        json.put("type", transaction.getType().getType());
        json.put("subtype", transaction.getType().getSubtype());
        json.put("timestamp", transaction.getTimestamp());
        json.put("deadline", transaction.getDeadline());
        json.put("senderPublicKey", Convert.toHexString(transaction.getSenderPublicKey()));
        if (transaction.getRecipientId() != 0) {
            putAccount(json, "recipient", transaction.getRecipientId());
        }
        json.put("amountNQT", String.valueOf(transaction.getAmountNQT()));
        json.put("feeNQT", String.valueOf(transaction.getFeeNQT()));
        if (transaction.getReferencedTransactionFullHash() != null) {
            json.put("referencedTransactionFullHash", transaction.getReferencedTransactionFullHash());
        }
        byte[] signature = Convert.emptyToNull(transaction.getSignature());
        if (signature != null) {
            json.put("signature", Convert.toHexString(signature));
            json.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(signature)));
            json.put("fullHash", transaction.getFullHash());
            json.put("transaction", transaction.getStringId());
        }
        else if (!transaction.getType().isSigned()) {
        	json.put("fullHash", transaction.getFullHash());
            json.put("transaction", transaction.getStringId());
        }
        JSONObject attachmentJSON = new JSONObject();
        for (Appendix appendage : transaction.getAppendages()) {
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (! attachmentJSON.isEmpty()) {
            modifyAttachmentJSON(attachmentJSON);
            json.put("attachment", attachmentJSON);
        }
        putAccount(json, "sender", transaction.getSenderId());
        json.put("height", transaction.getHeight());
        json.put("version", transaction.getVersion());
        if (transaction.getVersion() > 0) {
            json.put("ecBlockId", Convert.toUnsignedLong(transaction.getECBlockId()));
            json.put("ecBlockHeight", transaction.getECBlockHeight());
        }

        return json;
    }

    public static JSONObject transaction(Transaction transaction) {
        JSONObject json = unconfirmedTransaction(transaction);
        json.put("block", Convert.toUnsignedLong(transaction.getBlockId()));
        json.put("confirmations", Nxt.getBlockchain().getHeight() - transaction.getHeight());
        json.put("blockTimestamp", transaction.getBlockTimestamp());
        return json;
    }

    // ugly, hopefully temporary
    private static void modifyAttachmentJSON(JSONObject json) {
        Long quantityQNT = (Long) json.remove("quantityQNT");
        if (quantityQNT != null) {
            json.put("quantityQNT", String.valueOf(quantityQNT));
        }
        Long priceNQT = (Long) json.remove("priceNQT");
        if (priceNQT != null) {
            json.put("priceNQT", String.valueOf(priceNQT));
        }
        Long discountNQT = (Long) json.remove("discountNQT");
        if (discountNQT != null) {
            json.put("discountNQT", String.valueOf(discountNQT));
        }
        Long refundNQT = (Long) json.remove("refundNQT");
        if (refundNQT != null) {
            json.put("refundNQT", String.valueOf(refundNQT));
        }
    }

    static void putAccount(JSONObject json, String name, long accountId) {
        json.put(name, Convert.toUnsignedLong(accountId));
        json.put(name + "RS", Convert.rsAccount(accountId));
    }

    static JSONObject at(AT at) {
        JSONObject json = new JSONObject();
        ByteBuffer bf = ByteBuffer.allocate( 8 );
        bf.order( ByteOrder.LITTLE_ENDIAN );
        
        bf.put( at.getCreator() );
        bf.clear();
        putAccount(json, "creator", bf.getLong() );
        bf.clear();
        bf.put( at.getId() , 0 , 8 );
        long id = bf.getLong(0);
        json.put("at", Convert.toUnsignedLong( id ));
        json.put("atRS", Convert.rsAccount(id));
        json.put("atVersion", at.getVersion());        
        json.put("name", at.getName());
        json.put("description", at.getDescription());
        json.put("creator", Convert.toUnsignedLong(AT_API_Helper.getLong(at.getCreator())));
        json.put("creatorRS", Convert.rsAccount(AT_API_Helper.getLong(at.getCreator())));
        json.put("machineCode", Convert.toHexString(at.getApCode()));
        json.put("machineData", Convert.toHexString(at.getApData()));
        json.put("balanceNQT", Convert.toUnsignedLong(Account.getAccount(id).getBalanceNQT()));
        json.put("prevBalanceNQT", Convert.toUnsignedLong(at.getP_balance()));
        json.put("nextBlock", at.nextHeight());
        json.put("frozen", at.freezeOnSameBalance());
        json.put("running", at.getMachineState().isRunning());
        json.put("stopped", at.getMachineState().isStopped());
        json.put("finished", at.getMachineState().isFinished());
        json.put("dead", at.getMachineState().isDead());
        json.put("minActivation", Convert.toUnsignedLong(at.minActivationAmount()));
        json.put("creationBlock", at.getCreationBlockHeight());
        return json;
    }
    
    static JSONObject hex2long(String longString){
    	JSONObject json = new JSONObject();
        json.put("hex2long", longString);
        return json;
    }
   
    
    
    private JSONData() {} // never

}
