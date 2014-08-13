package nxt;

import nxt.crypto.XoredData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

public interface Attachment {

    public int getSize();
    public byte[] getBytes();
    public JSONObject getJSONObject();

    TransactionType getTransactionType();


    public final static class MessagingArbitraryMessage implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final byte[] message;

        public MessagingArbitraryMessage(byte[] message) {

            this.message = message;

        }

        @Override
        public int getSize() {
            return 4 + message.length;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(message.length);
            buffer.put(message);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("message", message == null ? null : Convert.toHexString(message));

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

        public byte[] getMessage() {
            return message;
        }
    }

    public final static class MessagingAliasAssignment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String aliasName;
        private final String aliasURI;

        public MessagingAliasAssignment(String aliasName, String aliasURI) {

            this.aliasName = aliasName.trim().intern();
            this.aliasURI = aliasURI.trim().intern();

        }

        @Override
        public int getSize() {
            try {
                return 1 + aliasName.getBytes("UTF-8").length + 2 + aliasURI.getBytes("UTF-8").length;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {

                byte[] alias = this.aliasName.getBytes("UTF-8");
                byte[] uri = this.aliasURI.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(1 + alias.length + 2 + uri.length);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)alias.length);
                buffer.put(alias);
                buffer.putShort((short)uri.length);
                buffer.put(uri);

                return buffer.array();

            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;

            }

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_ASSIGNMENT;
        }

        public String getAliasName() {
            return aliasName;
        }

        public String getAliasURI() {
            return aliasURI;
        }
    }

    public final static class MessagingPollCreation implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;
        private final byte minNumberOfOptions, maxNumberOfOptions;
        private final boolean optionsAreBinary;

        public MessagingPollCreation(String pollName, String pollDescription, String[] pollOptions, byte minNumberOfOptions, byte maxNumberOfOptions, boolean optionsAreBinary) {

            this.pollName = pollName;
            this.pollDescription = pollDescription;
            this.pollOptions = pollOptions;
            this.minNumberOfOptions = minNumberOfOptions;
            this.maxNumberOfOptions = maxNumberOfOptions;
            this.optionsAreBinary = optionsAreBinary;

        }

        @Override
        public int getSize() {
            try {
                int size = 2 + pollName.getBytes("UTF-8").length + 2 + pollDescription.getBytes("UTF-8").length + 1;
                for (String pollOption : pollOptions) {
                    size += 2 + pollOption.getBytes("UTF-8").length;
                }
                size +=  1 + 1 + 1;
                return size;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {

                byte[] name = this.pollName.getBytes("UTF-8");
                byte[] description = this.pollDescription.getBytes("UTF-8");
                byte[][] options = new byte[this.pollOptions.length][];
                for (int i = 0; i < this.pollOptions.length; i++) {
                    options[i] = this.pollOptions[i].getBytes("UTF-8");
                }

                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putShort((short)name.length);
                buffer.put(name);
                buffer.putShort((short)description.length);
                buffer.put(description);
                buffer.put((byte)options.length);
                for (byte[] option : options) {
                    buffer.putShort((short) option.length);
                    buffer.put(option);
                }
                buffer.put(this.minNumberOfOptions);
                buffer.put(this.maxNumberOfOptions);
                buffer.put(this.optionsAreBinary ? (byte)1 : (byte)0);

                return buffer.array();

            } catch (RuntimeException | UnsupportedEncodingException e) {

                Logger.logMessage("Error in getBytes", e);
                return null;

            }

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", this.pollName);
            attachment.put("description", this.pollDescription);
            JSONArray options = new JSONArray();
            if (this.pollOptions != null) {
                Collections.addAll(options, this.pollOptions);
            }
            attachment.put("options", options);
            attachment.put("minNumberOfOptions", this.minNumberOfOptions);
            attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);
            attachment.put("optionsAreBinary", this.optionsAreBinary);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.POLL_CREATION;
        }

        public String getPollName() { return pollName; }

        public String getPollDescription() { return pollDescription; }

        public String[] getPollOptions() { return pollOptions; }

        public byte getMinNumberOfOptions() { return minNumberOfOptions; }

        public byte getMaxNumberOfOptions() { return maxNumberOfOptions; }

        public boolean isOptionsAreBinary() { return optionsAreBinary; }

    }

    public final static class MessagingVoteCasting implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long pollId;
        private final byte[] pollVote;

        public MessagingVoteCasting(Long pollId, byte[] pollVote) {

            this.pollId = pollId;
            this.pollVote = pollVote;

        }

        @Override
        public int getSize() {
            return 8 + 1 + this.pollVote.length;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(this.pollId);
            buffer.put((byte)this.pollVote.length);
            buffer.put(this.pollVote);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("pollId", Convert.toUnsignedLong(this.pollId));
            JSONArray vote = new JSONArray();
            if (this.pollVote != null) {
                for (byte aPollVote : this.pollVote) {
                    vote.add(aPollVote);
                }
            }
            attachment.put("vote", vote);
            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.VOTE_CASTING;
        }

        public Long getPollId() { return pollId; }

        public byte[] getPollVote() { return pollVote; }

    }

    public final static class MessagingHubAnnouncement implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final long minFeePerByteNQT;
        private final String[] uris;

        public MessagingHubAnnouncement(long minFeePerByteNQT, String[] uris) {
            this.minFeePerByteNQT = minFeePerByteNQT;
            this.uris = uris;
        }

        @Override
        public int getSize() {
            try {
                int size = 8 + 1;
                for (String uri : uris) {
                    size += 2 + uri.getBytes("UTF-8").length;
                }
                return size;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(minFeePerByteNQT);
                buffer.put((byte) uris.length);
                for (String uri : uris) {
                    byte[] uriBytes = uri.getBytes("UTF-8");
                    buffer.putShort((short)uriBytes.length);
                    buffer.put(uriBytes);
                }
                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("minFeePerByteNQT", minFeePerByteNQT);
            JSONArray uris = new JSONArray();
            Collections.addAll(uris, this.uris);
            attachment.put("uris", uris);
            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.HUB_ANNOUNCEMENT;
        }

        public long getMinFeePerByteNQT() {
            return minFeePerByteNQT;
        }

        public String[] getUris() {
            return uris;
        }

    }

    public final static class MessagingAccountInfo implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String name;
        private final String description;

        public MessagingAccountInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public int getSize() {
            try {
                return 1 + name.getBytes("UTF-8").length + 2 + description.getBytes("UTF-8").length;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {
            try {
                byte[] name = this.name.getBytes("UTF-8");
                byte[] description = this.description.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)name.length);
                buffer.put(name);
                buffer.putShort((short)description.length);
                buffer.put(description);

                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_INFO;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

    }

    public final static class ColoredCoinsAssetIssuance implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String name;
        private final String description;
        private final long quantityQNT;
        private final byte decimals;

        public ColoredCoinsAssetIssuance(String name, String description, long quantityQNT, byte decimals) {

            this.name = name;
            this.description = Convert.nullToEmpty(description);
            this.quantityQNT = quantityQNT;
            this.decimals = decimals;

        }

        @Override
        public int getSize() {
            try {
                return 1 + name.getBytes("UTF-8").length + 2 + description.getBytes("UTF-8").length + 8 + 1;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {
                byte[] name = this.name.getBytes("UTF-8");
                byte[] description = this.description.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length + 8 + 1);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)name.length);
                buffer.put(name);
                buffer.putShort((short)description.length);
                buffer.put(description);
                buffer.putLong(quantityQNT);
                buffer.put(decimals);

                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("decimals", decimals);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_ISSUANCE;
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

    public final static class ColoredCoinsAssetTransfer implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long assetId;
        private final long quantityQNT;
        private final String comment;

        public ColoredCoinsAssetTransfer(Long assetId, long quantityQNT, String comment) {

            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.comment = Convert.nullToEmpty(comment);

        }

        @Override
        public int getSize() {
            try {
                return 8 + 8 + 2 + comment.getBytes("UTF-8").length;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {
                byte[] commentBytes = this.comment.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 2 + commentBytes.length);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(Convert.nullToZero(assetId));
                buffer.putLong(quantityQNT);
                buffer.putShort((short) commentBytes.length);
                buffer.put(commentBytes);

                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("comment", comment);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_TRANSFER;
        }

        public Long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public String getComment() {
            return comment;
        }

    }

    abstract static class ColoredCoinsOrderPlacement implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long assetId;
        private final long quantityQNT;
        private final long priceNQT;

        private ColoredCoinsOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {

            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.priceNQT = priceNQT;

        }

        @Override
        public int getSize() {
            return 8 + 8 + 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(assetId));
            buffer.putLong(quantityQNT);
            buffer.putLong(priceNQT);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("priceNQT", priceNQT);

            return attachment;

        }

        public Long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getPriceNQT() {
            return priceNQT;
        }
    }

    public final static class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        static final long serialVersionUID = 0;

        public ColoredCoinsAskOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    public final static class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        static final long serialVersionUID = 0;

        public ColoredCoinsBidOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract static class ColoredCoinsOrderCancellation implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long orderId;

        private ColoredCoinsOrderCancellation(Long orderId) {
            this.orderId = orderId;
        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(orderId));

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("order", Convert.toUnsignedLong(orderId));

            return attachment;

        }

        public Long getOrderId() {
            return orderId;
        }
    }

    public final static class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        static final long serialVersionUID = 0;

        public ColoredCoinsAskOrderCancellation(Long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    public final static class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        static final long serialVersionUID = 0;

        public ColoredCoinsBidOrderCancellation(Long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

    public final static class DigitalGoodsListing implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long priceNQT;

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceNQT) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
        }

        @Override
        public int getSize() {
            try {
                return 2 + name.getBytes("UTF-8").length + 2 + description.getBytes("UTF-8").length + 2
                        + tags.getBytes("UTF-8").length + 4 + 8;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                byte[] nameBytes = name.getBytes("UTF-8");
                buffer.putShort((short)nameBytes.length);
                buffer.put(nameBytes);
                byte[] descriptionBytes = description.getBytes("UTF-8");
                buffer.putShort((short)descriptionBytes.length);
                buffer.put(descriptionBytes);
                byte[] tagsBytes = tags.getBytes("UTF-8");
                buffer.putShort((short)tagsBytes.length);
                buffer.put(tagsBytes);
                buffer.putInt(quantity);
                buffer.putLong(priceNQT);
                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("tags", tags);
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getTags() { return tags; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

    }

    public final static class DigitalGoodsDelisting implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;

        public DigitalGoodsDelisting(Long goodsId) {
            this.goodsId = goodsId;
        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

        public Long getGoodsId() { return goodsId; }

    }

    public final static class DigitalGoodsPriceChange implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;
        private final long priceNQT;

        public DigitalGoodsPriceChange(Long goodsId, long priceNQT) {
            this.goodsId = goodsId;
            this.priceNQT = priceNQT;
        }

        @Override
        public int getSize() {
            return 8 + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                buffer.putLong(priceNQT);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("priceNQT", priceNQT);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

        public Long getGoodsId() { return goodsId; }

        public long getPriceNQT() { return priceNQT; }

    }

    public final static class DigitalGoodsQuantityChange implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;
        private final int deltaQuantity;

        public DigitalGoodsQuantityChange(Long goodsId, int deltaQuantity) {
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        public int getSize() {
            return 8 + 4;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                buffer.putInt(deltaQuantity);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("deltaQuantity", deltaQuantity);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

        public Long getGoodsId() { return goodsId; }

        public int getDeltaQuantity() { return deltaQuantity; }

    }

    public final static class DigitalGoodsPurchase implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long goodsId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadline;
        private final XoredData note;

        public DigitalGoodsPurchase(Long goodsId, int quantity, long priceNQT, int deliveryDeadline, XoredData note) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadline = deliveryDeadline;
            this.note = note;
        }

        @Override
        public int getSize() {
            return 8 + 4 + 8 + 4 + 2 + note.getData().length + 32;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                buffer.putInt(quantity);
                buffer.putLong(priceNQT);
                buffer.putInt(deliveryDeadline);
                buffer.putShort((short)note.getData().length);
                buffer.put(note.getData());
                buffer.put(note.getNonce());
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
            attachment.put("deliveryDeadline", deliveryDeadline);
            attachment.put("note", Convert.toHexString(note.getData()));
            attachment.put("noteNonce", Convert.toHexString(note.getNonce()));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

        public Long getGoodsId() { return goodsId; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

        public int getDeliveryDeadline() { return deliveryDeadline; }

        public XoredData getNote() { return note; }

    }

    public final static class DigitalGoodsDelivery implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long purchaseId;
        private final XoredData goods;
        private final long discountNQT;

        public DigitalGoodsDelivery(Long purchaseId, XoredData goods, long discountNQT) {
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discountNQT = discountNQT;
        }

        @Override
        public int getSize() {
            return 8 + 2 + goods.getData().length + 32 + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(purchaseId);
                buffer.putShort((short)goods.getData().length);
                buffer.put(goods.getData());
                buffer.put(goods.getNonce());
                buffer.putLong(discountNQT);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("goods", Convert.toHexString(goods.getData()));
            attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
            attachment.put("discountNQT", discountNQT);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

        public Long getPurchaseId() { return purchaseId; }

        public XoredData getGoods() { return goods; }

        public long getDiscountNQT() { return discountNQT; }

    }

    public final static class DigitalGoodsFeedback implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long purchaseId;
        private final XoredData note;

        public DigitalGoodsFeedback(Long purchaseId, XoredData note) {
            this.purchaseId = purchaseId;
            this.note = note;
        }

        @Override
        public int getSize() {
            try {
                return 8 + 2 + note.getData().length + 32;
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(purchaseId);
                buffer.putShort((short)note.getData().length);
                buffer.put(note.getData());
                buffer.put(note.getNonce());
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("note", Convert.toHexString(note.getData()));
            attachment.put("noteNonce", Convert.toHexString(note.getNonce()));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.FEEDBACK;
        }

        public Long getPurchaseId() { return purchaseId; }

        public XoredData getNote() { return note; }

    }

    public final static class DigitalGoodsRefund implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final Long purchaseId;
        private final long refundNQT;
        private final XoredData note;

        public DigitalGoodsRefund(Long purchaseId, long refundNQT, XoredData note) {
            this.purchaseId = purchaseId;
            this.refundNQT = refundNQT;
            this.note = note;
        }

        @Override
        public int getSize() {
            try {
                return 8 + 8 + 2 + note.getData().length + 32;
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(purchaseId);
                buffer.putLong(refundNQT);
                buffer.putShort((short)note.getData().length);
                buffer.put(note.getData());
                buffer.put(note.getNonce());
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("refundNQT", refundNQT);
            attachment.put("note", Convert.toHexString(note.getData()));
            attachment.put("noteNonce", Convert.toHexString(note.getNonce()));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

        public Long getPurchaseId() { return purchaseId; }

        public long getRefundNQT() { return refundNQT; }

        public XoredData getNote() { return note; }

    }

    public final static class AccountControlEffectiveBalanceLeasing implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        private final short period;

        public AccountControlEffectiveBalanceLeasing(short period) {
            this.period = period;
        }

        @Override
        public int getSize() {
            return 2;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putShort(period);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("period", period);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
        }

        public short getPeriod() {
            return period;
        }

    }

}
