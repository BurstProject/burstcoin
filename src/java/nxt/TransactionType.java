package nxt;

import nxt.Attachment.AbstractAttachment;
import nxt.Attachment.AutomatedTransactionsCreation;
import nxt.NxtException.NotValidException;
import nxt.NxtException.ValidationException;
import nxt.at.AT_Constants;
import nxt.at.AT_Controller;
import nxt.at.AT_Exception;
import nxt.util.Convert;

import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class TransactionType {

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;
    private static final byte TYPE_DIGITAL_GOODS = 3;
    private static final byte TYPE_ACCOUNT_CONTROL = 4;
    
    private static final byte TYPE_BURST_MINING = 20; // jump some for easier nxt updating
    private static final byte TYPE_ADVANCED_PAYMENT = 21;
    private static final byte TYPE_AUTOMATED_TRANSACTIONS = 22;

    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    private static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    private static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    private static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    private static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    private static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;

    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;

    private static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    private static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    private static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    private static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    private static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    private static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;
    
    private static final byte SUBTYPE_AT_CREATION = 0;
    private static final byte SUBTYPE_AT_NXT_PAYMENT = 1;

    private static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;
    
    private static final byte SUBTYPE_BURST_MINING_REWARD_RECIPIENT_ASSIGNMENT = 0;
    
    private static final byte SUBTYPE_ADVANCED_PAYMENT_ESCROW_CREATION = 0;
    private static final byte SUBTYPE_ADVANCED_PAYMENT_ESCROW_SIGN = 1;
    private static final byte SUBTYPE_ADVANCED_PAYMENT_ESCROW_RESULT = 2;
    private static final byte SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_SUBSCRIBE = 3;
    private static final byte SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_CANCEL = 4;
    private static final byte SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_PAYMENT = 5;

    private static final int BASELINE_FEE_HEIGHT = 1; // At release time must be less than current block - 1440
    private static final Fee BASELINE_FEE = new Fee(Constants.ONE_NXT, 0);
    private static final Fee BASELINE_ASSET_ISSUANCE_FEE = new Fee(1000 * Constants.ONE_NXT, 0);
    private static final int NEXT_FEE_HEIGHT = Integer.MAX_VALUE;
    private static final Fee NEXT_FEE = new Fee(Constants.ONE_NXT, 0);
    private static final Fee NEXT_ASSET_ISSUANCE_FEE = new Fee(1000 * Constants.ONE_NXT, 0);

    public static TransactionType findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Payment.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Messaging.ALIAS_ASSIGNMENT;
                    case SUBTYPE_MESSAGING_POLL_CREATION:
                        return Messaging.POLL_CREATION;
                    case SUBTYPE_MESSAGING_VOTE_CASTING:
                        return Messaging.VOTE_CASTING;
                    case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                        return Messaging.HUB_ANNOUNCEMENT;
                    case SUBTYPE_MESSAGING_ACCOUNT_INFO:
                        return Messaging.ACCOUNT_INFO;
                    case SUBTYPE_MESSAGING_ALIAS_SELL:
                        return Messaging.ALIAS_SELL;
                    case SUBTYPE_MESSAGING_ALIAS_BUY:
                        return Messaging.ALIAS_BUY;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return ColoredCoins.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return ColoredCoins.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return ColoredCoins.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return ColoredCoins.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return ColoredCoins.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return ColoredCoins.BID_ORDER_CANCELLATION;
                    default:
                        return null;
                }
            case TYPE_DIGITAL_GOODS:
                switch (subtype) {
                    case SUBTYPE_DIGITAL_GOODS_LISTING:
                        return DigitalGoods.LISTING;
                    case SUBTYPE_DIGITAL_GOODS_DELISTING:
                        return DigitalGoods.DELISTING;
                    case SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE:
                        return DigitalGoods.PRICE_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE:
                        return DigitalGoods.QUANTITY_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_PURCHASE:
                        return DigitalGoods.PURCHASE;
                    case SUBTYPE_DIGITAL_GOODS_DELIVERY:
                        return DigitalGoods.DELIVERY;
                    case SUBTYPE_DIGITAL_GOODS_FEEDBACK:
                        return DigitalGoods.FEEDBACK;
                    case SUBTYPE_DIGITAL_GOODS_REFUND:
                        return DigitalGoods.REFUND;
                    default:
                        return null;
                }
            case TYPE_ACCOUNT_CONTROL:
                switch (subtype) {
                    case SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING:
                        return AccountControl.EFFECTIVE_BALANCE_LEASING;
                    default:
                        return null;
                }
            case TYPE_BURST_MINING:
            	switch (subtype) {
            		case SUBTYPE_BURST_MINING_REWARD_RECIPIENT_ASSIGNMENT:
            			return BurstMining.REWARD_RECIPIENT_ASSIGNMENT;
            		default:
            			return null;
            	}
            case TYPE_ADVANCED_PAYMENT:
            	switch (subtype) {
	            	case SUBTYPE_ADVANCED_PAYMENT_ESCROW_CREATION:
	            		return AdvancedPayment.ESCROW_CREATION;
	            	case SUBTYPE_ADVANCED_PAYMENT_ESCROW_SIGN:
	            		return AdvancedPayment.ESCROW_SIGN;
	            	case SUBTYPE_ADVANCED_PAYMENT_ESCROW_RESULT:
	            		return AdvancedPayment.ESCROW_RESULT;
	            	case SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_SUBSCRIBE:
	            		return AdvancedPayment.SUBSCRIPTION_SUBSCRIBE;
	            	case SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_CANCEL:
	            		return AdvancedPayment.SUBSCRIPTION_CANCEL;
	            	case SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_PAYMENT:
	            		return AdvancedPayment.SUBSCRIPTION_PAYMENT;
            		default:
            			return null;
            	}
            case TYPE_AUTOMATED_TRANSACTIONS:
            	switch (subtype) {
            		case SUBTYPE_AT_CREATION:
            			return AutomatedTransactions.AUTOMATED_TRANSACTION_CREATION;
            		case SUBTYPE_AT_NXT_PAYMENT:
            			return AutomatedTransactions.AT_PAYMENT;
            		default:
            			return null;
            	}
            default:
                return null;
        }
    }

    private TransactionType() {
    }

    public abstract byte getType();

    public abstract byte getSubtype();

    abstract Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException;

    abstract Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException;

    abstract void validateAttachment(Transaction transaction) throws NxtException.ValidationException;

    // return false iff double spending
    final boolean applyUnconfirmed(Transaction transaction, Account senderAccount) {
        long totalAmountNQT = Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT());
        if (transaction.getReferencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            totalAmountNQT = Convert.safeAdd(totalAmountNQT, Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        if (senderAccount.getUnconfirmedBalanceNQT() < totalAmountNQT) {
            return false;
        }
        senderAccount.addToUnconfirmedBalanceNQT(-totalAmountNQT);
        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            senderAccount.addToUnconfirmedBalanceNQT(totalAmountNQT);
            return false;
        }
        return true;
    }

    abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        senderAccount.addToBalanceNQT(- (Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT())));
        if (transaction.getReferencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addToUnconfirmedBalanceNQT(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        if (recipientAccount != null) {
            recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(transaction.getAmountNQT());
        }
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    final void undoUnconfirmed(Transaction transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        senderAccount.addToUnconfirmedBalanceNQT(Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT()));
        if (transaction.getReferencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addToUnconfirmedBalanceNQT(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
    }

    abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
        return false;
    }

    static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Set<String>> duplicates) {
        Set<String> typeDuplicates = duplicates.get(uniqueType);
        if (typeDuplicates == null) {
            typeDuplicates = new HashSet<>();
            duplicates.put(uniqueType, typeDuplicates);
        }
        return ! typeDuplicates.add(key);
    }

    public abstract boolean hasRecipient();
    
    public boolean isSigned() {
    	return true;
    }

    @Override
    public final String toString() {
        return "type: " + getType() + ", subtype: " + getSubtype();
    }

    /*
    Collection<TransactionType> getPhasingTransactionTypes() {
        return Collections.emptyList();
    }

    Collection<TransactionType> getPhasedTransactionTypes() {
        return Collections.emptyList();
    }
    */

    public static abstract class Payment extends TransactionType {

        private Payment() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_PAYMENT;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        final public boolean hasRecipient() {
            return true;
        }

        public static final TransactionType ORDINARY = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT() <= 0 || transaction.getAmountNQT() >= Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid ordinary payment");
                }
            }

        };

    }

    public static abstract class Messaging extends TransactionType {

        private Messaging() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_MESSAGING;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment attachment = transaction.getAttachment();
                if (transaction.getAmountNQT() != 0) {
                    throw new NxtException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
                }
                if (Nxt.getBlockchain().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK && transaction.getMessage() == null) {
                	throw new NxtException.NotCurrentlyValidException("Missing message appendix not allowed before DGS block");
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
            }

            @Override
            Attachment.MessagingAliasAssignment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasAssignment(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAliasAssignment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasAssignment(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                Alias.addOrUpdateAlias(transaction, attachment);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                if (attachment.getAliasName().length() == 0
                        || Convert.toBytes(attachment.getAliasName()).length > Constants.MAX_ALIAS_LENGTH
                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                    throw new NxtException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
                }
                String normalizedAlias = attachment.getAliasName().toLowerCase();
                for (int i = 0; i < normalizedAlias.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid alias name: " + normalizedAlias);
                    }
                }
                Alias alias = Alias.getAlias(normalizedAlias);
                if (alias != null && alias.getAccountId() != transaction.getSenderId()) {
                    throw new NxtException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType ALIAS_SELL = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
            }

            @Override
            Attachment.MessagingAliasSell parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasSell(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAliasSell parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasSell(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                Alias.sellAlias(transaction, attachment);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            	if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
            		throw new NxtException.NotYetEnabledException("Alias transfer not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            	}
            	if (transaction.getAmountNQT() != 0) {
                    throw new NxtException.NotValidException("Invalid sell alias transaction: " +
                            transaction.getJSONObject());
                }
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new NxtException.NotValidException("Missing alias name");
                }
                long priceNQT = attachment.getPriceNQT();
                if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid alias sell price: " + priceNQT);
                }
                if (priceNQT == 0) {
                    if (Genesis.CREATOR_ID == transaction.getRecipientId()) {
                        throw new NxtException.NotValidException("Transferring aliases to Genesis account not allowed");
                    } else if (transaction.getRecipientId() == 0) {
                        throw new NxtException.NotValidException("Missing alias transfer recipient");
                    }
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.NotCurrentlyValidException("Alias hasn't been registered yet: " + aliasName);
                } else if (alias.getAccountId() != transaction.getSenderId()) {
                    throw new NxtException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType ALIAS_BUY = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
            }

            @Override
            Attachment.MessagingAliasBuy parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasBuy(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAliasBuy parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasBuy(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                Alias.changeOwner(transaction.getSenderId(), aliasName, transaction.getBlockTimestamp());
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.MessagingAliasBuy attachment = (Attachment.MessagingAliasBuy) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            	if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
            		throw new NxtException.NotYetEnabledException("Alias transfer not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
            	}
            	final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.NotCurrentlyValidException("Alias hasn't been registered yet: " + aliasName);
                } else if (alias.getAccountId() != transaction.getRecipientId()) {
                    throw new NxtException.NotCurrentlyValidException("Alias is owned by account other than recipient: "
                            + Convert.toUnsignedLong(alias.getAccountId()));
                }
                Alias.Offer offer = Alias.getOffer(alias);
                if (offer == null) {
                    throw new NxtException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
                }
                if (transaction.getAmountNQT() < offer.getPriceNQT()) {
                    String msg = "Price is too low for: " + aliasName + " ("
                            + transaction.getAmountNQT() + " < " + offer.getPriceNQT() + ")";
                    throw new NxtException.NotCurrentlyValidException(msg);
                }
                if (offer.getBuyerId() != 0 && offer.getBuyerId() != transaction.getSenderId()) {
                    throw new NxtException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": "
                            + Convert.toUnsignedLong(transaction.getSenderId()) + " expected: "
                            + Convert.toUnsignedLong(offer.getBuyerId()));
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public final static TransactionType POLL_CREATION = new Messaging() {
            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            Attachment.MessagingPollCreation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingPollCreation(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingPollCreation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingPollCreation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                Poll.addPoll(transaction, attachment);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                for (int i = 0; i < attachment.getPollOptions().length; i++) {
                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH) {
                        throw new NxtException.NotValidException("Invalid poll options length: " + attachment.getJSONObject());
                    }
                }
                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
                        || attachment.getPollOptions().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.NotValidException("Invalid poll attachment: " + attachment.getJSONObject());
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public final static TransactionType VOTE_CASTING = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
            }

            @Override
            Attachment.MessagingVoteCasting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingVoteCasting(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingVoteCasting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingVoteCasting(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                Poll poll = Poll.getPoll(attachment.getPollId());
                if (poll != null) {
                    Vote.addVote(transaction, attachment);
                }
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                if (attachment.getPollId() == 0 || attachment.getPollVote() == null
                        || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.NotValidException("Invalid vote casting attachment: " + attachment.getJSONObject());
                }
                if (Poll.getPoll(attachment.getPollId()) == null) {
                    throw new NxtException.NotCurrentlyValidException("Invalid poll: " + Convert.toUnsignedLong(attachment.getPollId()));
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType HUB_ANNOUNCEMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT;
            }

            @Override
            Attachment.MessagingHubAnnouncement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingHubAnnouncement parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                Hub.addOrUpdateHub(transaction, attachment);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_7) {
                    throw new NxtException.NotYetEnabledException("Hub terminal announcement not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
                }
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                if (attachment.getMinFeePerByteNQT() < 0 || attachment.getMinFeePerByteNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getUris().length > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                    // cfb: "0" is allowed to show that another way to determine the min fee should be used
                    throw new NxtException.NotValidException("Invalid hub terminal announcement: " + attachment.getJSONObject());
                }
                for (String uri : attachment.getUris()) {
                    if (uri.length() > Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH) {
                        throw new NxtException.NotValidException("Invalid URI length: " + uri.length());
                    }
                    //TODO: also check URI validity here?
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final Messaging ACCOUNT_INFO = new Messaging() {

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(buffer, transactionVersion);
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo)transaction.getAttachment();
                if (Convert.toBytes(attachment.getName()).length > Constants.MAX_ACCOUNT_NAME_LENGTH
                        || Convert.toBytes(attachment.getDescription()).length > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH
                        ) {
                    throw new NxtException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                senderAccount.setAccountInfo(attachment.getName(), attachment.getDescription());
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

    }

    public static abstract class ColoredCoins extends TransactionType {

        private ColoredCoins() {}

        @Override
        public final byte getType() {
            return TransactionType.TYPE_COLORED_COINS;
        }

        public static final TransactionType ASSET_ISSUANCE = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
            }

            @Override
            public Fee getBaselineFee() {
                return BASELINE_ASSET_ISSUANCE_FEE;
            }

            @Override
            public Fee getNextFee() {
                return NEXT_ASSET_ISSUANCE_FEE;
            }

            @Override
            Attachment.ColoredCoinsAssetIssuance parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAssetIssuance parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                long assetId = transaction.getId();
                Asset.addAsset(transaction, attachment);
                senderAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, attachment.getQuantityQNT());
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                if (attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH
                        || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH
                        || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                        || attachment.getQuantityQNT() <= 0
                        || attachment.getQuantityQNT() > Constants.MAX_ASSET_QUANTITY_QNT
                        ) {
                    throw new NxtException.NotValidException("Invalid asset issuance: " + attachment.getJSONObject());
                }
                String normalizedName = attachment.getName().toLowerCase();
                for (int i = 0; i < normalizedName.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid asset name: " + normalizedName);
                    }
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType ASSET_TRANSFER = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
            }

            @Override
            Attachment.ColoredCoinsAssetTransfer parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAssetTransfer parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                recipientAccount.addToAssetAndUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
                AssetTransfer.addAssetTransfer(transaction, attachment);
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                if (transaction.getAmountNQT() != 0
                        || attachment.getComment() != null && attachment.getComment().length() > Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH
                        || attachment.getAssetId() == 0) {
                    throw new NxtException.NotValidException("Invalid asset transfer amount or comment: " + attachment.getJSONObject());
                }
                if (transaction.getVersion() > 0 && attachment.getComment() != null) {
                    throw new NxtException.NotValidException("Asset transfer comments no longer allowed, use message " +
                            "or encrypted message appendix instead");
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getQuantityQNT())) {
                    throw new NxtException.NotValidException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new NxtException.NotCurrentlyValidException("Asset " + Convert.toUnsignedLong(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

            @Override
            final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.getAttachment();
                if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getAssetId() == 0) {
                    throw new NxtException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getQuantityQNT())) {
                    throw new NxtException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new NxtException.NotCurrentlyValidException("Asset " + Convert.toUnsignedLong(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            final public boolean hasRecipient() {
                return false;
            }

        }

        public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
            }

            @Override
            Attachment.ColoredCoinsAskOrderPlacement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAskOrderPlacement parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Ask.addOrder(transaction, attachment);
                }
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            }

        };

        public final static TransactionType BID_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
            }

            @Override
            Attachment.ColoredCoinsBidOrderPlacement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsBidOrderPlacement parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT())) {
                    senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT()));
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                if (Asset.getAsset(attachment.getAssetId()) != null) {
                    Order.Bid.addOrder(transaction, attachment);
                }
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getQuantityQNT(), attachment.getPriceNQT()));
            }

        };

        abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

            @Override
            final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        }

        public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
            }

            @Override
            Attachment.ColoredCoinsAskOrderCancellation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsAskOrderCancellation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                Order order = Order.Ask.getAskOrder(attachment.getOrderId());
                Order.Ask.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(order.getAssetId(), order.getQuantityQNT());
                }
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                Order ask = Order.Ask.getAskOrder(attachment.getOrderId());
                if(ask == null) {
                    throw new NxtException.NotCurrentlyValidException("Invalid ask order: " + Convert.toUnsignedLong(attachment.getOrderId()));
                }
                if(ask.getAccountId() != transaction.getSenderId()) {
                	throw new NxtException.NotValidException("Order " + Convert.toUnsignedLong(attachment.getOrderId()) + " was created by account "
                			+ Convert.toUnsignedLong(ask.getAccountId()));
                }
            }

        };

        public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
            }

            @Override
            Attachment.ColoredCoinsBidOrderCancellation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(buffer, transactionVersion);
            }

            @Override
            Attachment.ColoredCoinsBidOrderCancellation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                Order order = Order.Bid.getBidOrder(attachment.getOrderId());
                Order.Bid.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(order.getQuantityQNT(), order.getPriceNQT()));
                }
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                Order bid = Order.Bid.getBidOrder(attachment.getOrderId());
                if(bid == null) {
                    throw new NxtException.NotCurrentlyValidException("Invalid bid order: " + Convert.toUnsignedLong(attachment.getOrderId()));
                }
                if(bid.getAccountId() != transaction.getSenderId()) {
                	throw new NxtException.NotValidException("Order " + Convert.toUnsignedLong(attachment.getOrderId()) + " was created by account "
                			+ Convert.toUnsignedLong(bid.getAccountId()));
                }
            }

        };
    }

    public static abstract class DigitalGoods extends TransactionType {

        private DigitalGoods() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DIGITAL_GOODS;
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
        	if (Nxt.getBlockchain().getLastBlock().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
        		throw new NxtException.NotYetEnabledException("Digital goods listing not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
        	}
        	if (transaction.getAmountNQT() != 0) {
                throw new NxtException.NotValidException("Invalid digital goods transaction");
            }
            doValidateAttachment(transaction);
        }

        abstract void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException;


        public static final TransactionType LISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_LISTING;
            }

            @Override
            Attachment.DigitalGoodsListing parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsListing(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsListing parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsListing(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                DigitalGoodsStore.listGoods(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                if (attachment.getName().length() == 0
                        || attachment.getName().length() > Constants.MAX_DGS_LISTING_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH
                        || attachment.getTags().length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH
                        || attachment.getQuantity() < 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid digital goods listing: " + attachment.getJSONObject());
                }
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType DELISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING;
            }

            @Override
            Attachment.DigitalGoodsDelisting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsDelisting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.delistGoods(attachment.getGoodsId());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (goods != null && transaction.getSenderId() != goods.getSellerId()) {
                    throw new NxtException.NotValidException("Invalid digital goods delisting - seller is different: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELISTING, Convert.toUnsignedLong(attachment.getGoodsId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType PRICE_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            Attachment.DigitalGoodsPriceChange parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsPriceChange parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.changePrice(attachment.getGoodsId(), attachment.getPriceNQT());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new NxtException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Convert.toUnsignedLong(attachment.getGoodsId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType QUANTITY_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            Attachment.DigitalGoodsQuantityChange parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsQuantityChange parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.changeQuantity(attachment.getGoodsId(), attachment.getDeltaQuantity());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new NxtException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Convert.toUnsignedLong(attachment.getGoodsId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return false;
            }

        };

        public static final TransactionType PURCHASE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE;
            }

            @Override
            Attachment.DigitalGoodsPurchase parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsPurchase parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT())) {
                    senderAccount.addToUnconfirmedBalanceNQT(-Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.purchase(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(attachment.getGoodsId());
                if (attachment.getQuantity() <= 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || (goods != null && goods.getSellerId() != transaction.getRecipientId())) {
                    throw new NxtException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Convert.toUnsignedLong(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
                if (attachment.getQuantity() > goods.getQuantity() || attachment.getPriceNQT() != goods.getPriceNQT()) {
                    throw new NxtException.NotCurrentlyValidException("Goods price or quantity changed: " + attachment.getJSONObject());
                }
                if (attachment.getDeliveryDeadlineTimestamp() <= Nxt.getBlockchain().getLastBlock().getTimestamp()) {
                    throw new NxtException.NotCurrentlyValidException("Delivery deadline has already expired: " + attachment.getDeliveryDeadlineTimestamp());
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType DELIVERY = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY;
            }

            @Override
            Attachment.DigitalGoodsDelivery parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsDelivery parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery)transaction.getAttachment();
                DigitalGoodsStore.deliver(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPendingPurchase(attachment.getPurchaseId());
                if (attachment.getGoods().getData().length > Constants.MAX_DGS_GOODS_LENGTH
                        || attachment.getGoods().getData().length == 0
                        || attachment.getGoods().getNonce().length != 32
                        || attachment.getDiscountNQT() < 0 || attachment.getDiscountNQT() > Constants.MAX_BALANCE_NQT
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()
                                || attachment.getDiscountNQT() > Convert.safeMultiply(purchase.getPriceNQT(), purchase.getQuantity())))) {
                    throw new NxtException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                }
                if (purchase == null || purchase.getEncryptedGoods() != null) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist yet, or already delivered: "
                            + attachment.getJSONObject());
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELIVERY, Convert.toUnsignedLong(attachment.getPurchaseId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType FEEDBACK = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            Attachment.DigitalGoodsFeedback parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsFeedback parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback)transaction.getAttachment();
                DigitalGoodsStore.feedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(attachment.getPurchaseId());
                if (purchase != null &&
                        (purchase.getSellerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getBuyerId())) {
                    throw new NxtException.NotValidException("Invalid digital goods feedback: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() == null && transaction.getMessage() == null) {
                    throw new NxtException.NotValidException("Missing feedback message");
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (transaction.getMessage() != null && ! transaction.getMessage().isText()) {
                    throw new NxtException.NotValidException("Only text public messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist yet or not yet delivered");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                return isDuplicate(DigitalGoods.FEEDBACK, Convert.toUnsignedLong(attachment.getPurchaseId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

        public static final TransactionType REFUND = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND;
            }

            @Override
            Attachment.DigitalGoodsRefund parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsRefund(buffer, transactionVersion);
            }

            @Override
            Attachment.DigitalGoodsRefund parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsRefund(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= attachment.getRefundNQT()) {
                    senderAccount.addToUnconfirmedBalanceNQT(-attachment.getRefundNQT());
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceNQT(attachment.getRefundNQT());
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.refund(transaction.getSenderId(), attachment.getPurchaseId(),
                        attachment.getRefundNQT(), transaction.getEncryptedMessage());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(attachment.getPurchaseId());
                if (attachment.getRefundNQT() < 0 || attachment.getRefundNQT() > Constants.MAX_BALANCE_NQT
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()))) {
                    throw new NxtException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundNQT() != 0) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                return isDuplicate(DigitalGoods.REFUND, Convert.toUnsignedLong(attachment.getPurchaseId()), duplicates);
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

    }

    public static abstract class AccountControl extends TransactionType {

        private AccountControl() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_ACCOUNT_CONTROL;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public static final TransactionType EFFECTIVE_BALANCE_LEASING = new AccountControl() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
            }

            @Override
            Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(buffer, transactionVersion);
            }

            @Override
            Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
                //Account.getAccount(transaction.getSenderId()).leaseEffectiveBalance(transaction.getRecipientId(), attachment.getPeriod());
                // TODO: check if anyone's used this or if it's even possible to use this, and eliminate it if possible
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing)transaction.getAttachment();
                Account recipientAccount = Account.getAccount(transaction.getRecipientId());
                if (transaction.getSenderId() == transaction.getRecipientId()
                        || transaction.getAmountNQT() != 0
                        || attachment.getPeriod() < 1440) {
                    throw new NxtException.NotValidException("Invalid effective balance leasing: "
                            + transaction.getJSONObject() + " transaction " + transaction.getStringId());
                }
                if (recipientAccount == null
                        || (recipientAccount.getPublicKey() == null && ! transaction.getStringId().equals("5081403377391821646"))) {
                    throw new NxtException.NotCurrentlyValidException("Invalid effective balance leasing: "
                            + " recipient account " + transaction.getRecipientId() + " not found or no public key published");
                }
            }

            @Override
            public boolean hasRecipient() {
                return true;
            }

        };

    }
    
    public static abstract class BurstMining extends TransactionType {
    	
    	private BurstMining() {}
    	
    	@Override
    	public final byte getType() {
    		return TransactionType.TYPE_BURST_MINING;
    	}
    	
    	@Override
    	final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    		return true;
    	}
    	
    	@Override
    	final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}
    	
    	public static final TransactionType REWARD_RECIPIENT_ASSIGNMENT = new BurstMining() {
    		
    		@Override
    		public final byte getSubtype() {
    			return TransactionType.SUBTYPE_BURST_MINING_REWARD_RECIPIENT_ASSIGNMENT;
    		}
    		
    		@Override
            Attachment.BurstMiningRewardRecipientAssignment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new Attachment.BurstMiningRewardRecipientAssignment(buffer, transactionVersion);
            }

            @Override
            Attachment.BurstMiningRewardRecipientAssignment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.BurstMiningRewardRecipientAssignment(attachmentData);
            }
    		
    		@Override
    		void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
    			senderAccount.setRewardRecipientAssignment(recipientAccount.getId());
    		}
    		
    		@Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
    			if(Nxt.getBlockchain().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
    				return false; // sync fails after 7007 without this
    			}
                return isDuplicate(BurstMining.REWARD_RECIPIENT_ASSIGNMENT, Convert.toUnsignedLong(transaction.getSenderId()), duplicates);
            }
    		
    		@Override
    		void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
    			long height = Nxt.getBlockchain().getLastBlock().getHeight() + 1;
    			Account sender = Account.getAccount(transaction.getSenderId());
    			Account.RewardRecipientAssignment rewardAssignment = sender.getRewardRecipientAssignment();
    			if(rewardAssignment != null && rewardAssignment.getFromHeight() >= height) {
    				throw new NxtException.NotCurrentlyValidException("Cannot reassign reward recipient before previous goes into effect: " + transaction.getJSONObject());
    			}
    			Account recip = Account.getAccount(transaction.getRecipientId());
    			if(recip == null || recip.getPublicKey() == null) {
    				throw new NxtException.NotValidException("Reward recipient must have public key saved in blockchain: " + transaction.getJSONObject());
    			}
    			if(transaction.getAmountNQT() != 0 || transaction.getFeeNQT() != Constants.ONE_NXT) {
    				throw new NxtException.NotValidException("Reward recipient assisnment transaction must have 0 send amount and 1 fee: " + transaction.getJSONObject());
    			}
    			if(height < Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_START_BLOCK) {
    				throw new NxtException.NotCurrentlyValidException("Reward recipient assignment not allowed before block " + Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_START_BLOCK);
    			}
    		}
    		
    		@Override
            public boolean hasRecipient() {
                return true;
            }
    	};
    }

	public static abstract class AdvancedPayment extends TransactionType {
		
		private AdvancedPayment() {}
		
		@Override
		public final byte getType() {
			return TransactionType.TYPE_ADVANCED_PAYMENT;
		}
		
		public final static TransactionType ESCROW_CREATION = new AdvancedPayment() {
			
			@Override
			public final byte getSubtype() {
				return TransactionType.SUBTYPE_ADVANCED_PAYMENT_ESCROW_CREATION;
			}
			
			@Override
			Attachment.AdvancedPaymentEscrowCreation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentEscrowCreation(buffer, transactionVersion);
			}
			
			@Override
			Attachment.AdvancedPaymentEscrowCreation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentEscrowCreation(attachmentData);
			}
			
			@Override
			final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
				Attachment.AdvancedPaymentEscrowCreation attachment = (Attachment.AdvancedPaymentEscrowCreation) transaction.getAttachment();
				Long totalAmountNQT = Convert.safeAdd(attachment.getAmountNQT(), attachment.getTotalSigners() * Constants.ONE_NXT);
				if(senderAccount.getUnconfirmedBalanceNQT() < totalAmountNQT.longValue()) {
					return false;
				}
				senderAccount.addToUnconfirmedBalanceNQT(-totalAmountNQT);
				return true;
			}
			
			@Override
			final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
				Attachment.AdvancedPaymentEscrowCreation attachment = (Attachment.AdvancedPaymentEscrowCreation) transaction.getAttachment();
				Long totalAmountNQT = Convert.safeAdd(attachment.getAmountNQT(), attachment.getTotalSigners() * Constants.ONE_NXT);
				senderAccount.addToBalanceNQT(-totalAmountNQT);
				Collection<Long> signers = attachment.getSigners();
				for(Long signer : signers) {
					Account.addOrGetAccount(signer).addToBalanceAndUnconfirmedBalanceNQT(Constants.ONE_NXT);
				}
				Escrow.addEscrowTransaction(senderAccount,
											recipientAccount,
											transaction.getId(),
											attachment.getAmountNQT(),
											attachment.getRequiredSigners(),
											attachment.getSigners(),
											transaction.getTimestamp() + attachment.getDeadline(),
											attachment.getDeadlineAction());
			}
			
			@Override
			final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
				Attachment.AdvancedPaymentEscrowCreation attachment = (Attachment.AdvancedPaymentEscrowCreation) transaction.getAttachment();
				Long totalAmountNQT = Convert.safeAdd(attachment.getAmountNQT(), attachment.getTotalSigners() * Constants.ONE_NXT);
				senderAccount.addToUnconfirmedBalanceNQT(totalAmountNQT);
			}
			
			@Override
			boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
				return false;
			}
			
			@Override
			void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
				Attachment.AdvancedPaymentEscrowCreation attachment = (Attachment.AdvancedPaymentEscrowCreation) transaction.getAttachment();
				Long totalAmountNQT = Convert.safeAdd(attachment.getAmountNQT(), transaction.getFeeNQT());
				if(transaction.getSenderId() == transaction.getRecipientId()) {
					throw new NxtException.NotValidException("Escrow must have different sender and recipient");
				}
				totalAmountNQT = Convert.safeAdd(totalAmountNQT, attachment.getTotalSigners() * Constants.ONE_NXT);
				if(transaction.getAmountNQT() != 0) {
					throw new NxtException.NotValidException("Transaction sent amount must be 0 for escrow");
				}
				if(totalAmountNQT.compareTo(0L) < 0 ||
				   totalAmountNQT.compareTo(Constants.MAX_BALANCE_NQT) > 0)
				{
					throw new NxtException.NotValidException("Invalid escrow creation amount");
				}
				if(transaction.getFeeNQT() < Constants.ONE_NXT) {
					throw new NxtException.NotValidException("Escrow transaction must have a fee at least 1 burst");
				}
				if(attachment.getRequiredSigners() < 1 || attachment.getRequiredSigners() > 10) {
					throw new NxtException.NotValidException("Escrow required signers much be 1 - 10");
				}
				if(attachment.getRequiredSigners() > attachment.getTotalSigners()) {
					throw new NxtException.NotValidException("Cannot have more required than signers on escrow");
				}
				if(attachment.getTotalSigners() < 1 || attachment.getTotalSigners() > 10) {
					throw new NxtException.NotValidException("Escrow transaction requires 1 - 10 signers");
				}
				if(attachment.getDeadline() < 1 || attachment.getDeadline() > 7776000) { // max deadline 3 months
					throw new NxtException.NotValidException("Escrow deadline must be 1 - 7776000 seconds");
				}
				if(attachment.getDeadlineAction() == null || attachment.getDeadlineAction() == Escrow.DecisionType.UNDECIDED) {
					throw new NxtException.NotValidException("Invalid deadline action for escrow");
				}
				if(attachment.getSigners().contains(transaction.getSenderId()) ||
				   attachment.getSigners().contains(transaction.getRecipientId())) {
					throw new NxtException.NotValidException("Escrow sender and recipient cannot be signers");
				}
				if(!Escrow.isEnabled()) {
					throw new NxtException.NotYetEnabledException("Escrow not yet enabled");
				}
			}
			
			@Override
			final public boolean hasRecipient() {
				return true;
			}
		};
		
		public final static TransactionType ESCROW_SIGN = new AdvancedPayment() {
			
			@Override
			public final byte getSubtype() {
				return TransactionType.SUBTYPE_ADVANCED_PAYMENT_ESCROW_SIGN;
			}
			
			@Override
			Attachment.AdvancedPaymentEscrowSign parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentEscrowSign(buffer, transactionVersion);
			}
			
			@Override
			Attachment.AdvancedPaymentEscrowSign parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentEscrowSign(attachmentData);
			}
			
			@Override
			final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
				return true;
			}
			
			@Override
			final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
				Attachment.AdvancedPaymentEscrowSign attachment = (Attachment.AdvancedPaymentEscrowSign) transaction.getAttachment();
				Escrow escrow = Escrow.getEscrowTransaction(attachment.getEscrowId());
				escrow.sign(senderAccount.getId(), attachment.getDecision());
			}
			
			@Override
			final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
			}
			
			@Override
			boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
				Attachment.AdvancedPaymentEscrowSign attachment = (Attachment.AdvancedPaymentEscrowSign) transaction.getAttachment();
				String uniqueString = Convert.toUnsignedLong(attachment.getEscrowId()) + ":" +
									  Convert.toUnsignedLong(transaction.getSenderId());
				return isDuplicate(AdvancedPayment.ESCROW_SIGN, uniqueString, duplicates);
			}
			
			@Override
			void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
				Attachment.AdvancedPaymentEscrowSign attachment = (Attachment.AdvancedPaymentEscrowSign) transaction.getAttachment();
				if(transaction.getAmountNQT() != 0 || transaction.getFeeNQT() != Constants.ONE_NXT) {
					throw new NxtException.NotValidException("Escrow signing must have amount 0 and fee of 1");
				}
				if(attachment.getEscrowId() == null || attachment.getDecision() == null) {
					throw new NxtException.NotValidException("Escrow signing requires escrow id and decision set");
				}
				Escrow escrow = Escrow.getEscrowTransaction(attachment.getEscrowId());
				if(escrow == null) {
					throw new NxtException.NotValidException("Escrow transaction not found");
				}
				if(!escrow.isIdSigner(transaction.getSenderId()) &&
				   !escrow.getSenderId().equals(transaction.getSenderId()) &&
				   !escrow.getRecipientId().equals(transaction.getSenderId())) {
					throw new NxtException.NotValidException("Sender is not a participant in specified escrow");
				}
				if(escrow.getSenderId().equals(transaction.getSenderId()) && attachment.getDecision() != Escrow.DecisionType.RELEASE) {
					throw new NxtException.NotValidException("Escrow sender can only release");
				}
				if(escrow.getRecipientId().equals(transaction.getSenderId()) && attachment.getDecision() != Escrow.DecisionType.REFUND) {
					throw new NxtException.NotValidException("Escrow recipient can only refund");
				}
				if(!Escrow.isEnabled()) {
					throw new NxtException.NotYetEnabledException("Escrow not yet enabled");
				}
			}
			
			@Override
			final public boolean hasRecipient() {
				return false;
			}
		};
		
		public final static TransactionType ESCROW_RESULT = new AdvancedPayment() {
			
			@Override
			public final byte getSubtype() {
				return TransactionType.SUBTYPE_ADVANCED_PAYMENT_ESCROW_RESULT;
			}
			
			@Override
			Attachment.AdvancedPaymentEscrowResult parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentEscrowResult(buffer, transactionVersion);
			}
			
			@Override
			Attachment.AdvancedPaymentEscrowResult parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentEscrowResult(attachmentData);
			}
			
			@Override
			final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
				return false;
			}
			
			@Override
			final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
			}
			
			@Override
			final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
			}
			
			@Override
			boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
				return true;
			}
			
			@Override
			void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
				throw new NxtException.NotValidException("Escrow result never validates");
			}
			
			@Override
			final public boolean hasRecipient() {
				return true;
			}
			
			@Override
			final public boolean isSigned() {
				return false;
			}
		};
		
		public final static TransactionType SUBSCRIPTION_SUBSCRIBE = new AdvancedPayment() {
			
			@Override
			public final byte getSubtype() {
				return TransactionType.SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_SUBSCRIBE;
			}
			
			@Override
			Attachment.AdvancedPaymentSubscriptionSubscribe parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentSubscriptionSubscribe(buffer, transactionVersion);
			}
			
			@Override
			Attachment.AdvancedPaymentSubscriptionSubscribe parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentSubscriptionSubscribe(attachmentData);
			}
			
			@Override
			final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
				return true;
			}
			
			@Override
			final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
				Attachment.AdvancedPaymentSubscriptionSubscribe attachment = (Attachment.AdvancedPaymentSubscriptionSubscribe) transaction.getAttachment();
				Subscription.addSubscription(senderAccount, recipientAccount, transaction.getId(), transaction.getAmountNQT(), transaction.getTimestamp(), attachment.getFrequency());
			}
			
			@Override
			final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
			}
			
			@Override
			boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
				return false;
			}
			
			@Override
			void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
				Attachment.AdvancedPaymentSubscriptionSubscribe attachment = (Attachment.AdvancedPaymentSubscriptionSubscribe) transaction.getAttachment();
				if(attachment.getFrequency() == null ||
				   attachment.getFrequency().intValue() < Constants.BURST_SUBSCRIPTION_MIN_FREQ ||
				   attachment.getFrequency().intValue() > Constants.BURST_SUBSCRIPTION_MAX_FREQ) {
					throw new NxtException.NotValidException("Invalid subscription frequency");
				}
				if(transaction.getAmountNQT() < Constants.ONE_NXT || transaction.getAmountNQT() > Constants.MAX_BALANCE_NQT) {
					throw new NxtException.NotValidException("Subscriptions must be at least one burst");
				}
				if(transaction.getSenderId() == transaction.getRecipientId()) {
					throw new NxtException.NotValidException("Cannot create subscription to same address");
				}
				if(!Subscription.isEnabled()) {
					throw new NxtException.NotYetEnabledException("Subscriptions not yet enabled");
				}
			}
			
			@Override
			final public boolean hasRecipient() {
				return true;
			}
		};
		
		public final static TransactionType SUBSCRIPTION_CANCEL = new AdvancedPayment() {
			
			@Override
			public final byte getSubtype() {
				return TransactionType.SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_CANCEL;
			}
			
			@Override
			Attachment.AdvancedPaymentSubscriptionCancel parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentSubscriptionCancel(buffer, transactionVersion);
			}
			
			@Override
			Attachment.AdvancedPaymentSubscriptionCancel parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentSubscriptionCancel(attachmentData);
			}
			
			@Override
			final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
				Attachment.AdvancedPaymentSubscriptionCancel attachment = (Attachment.AdvancedPaymentSubscriptionCancel) transaction.getAttachment();
				Subscription.addRemoval(attachment.getSubscriptionId());
				return true;
			}
			
			@Override
			final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
				Attachment.AdvancedPaymentSubscriptionCancel attachment = (Attachment.AdvancedPaymentSubscriptionCancel) transaction.getAttachment();
				Subscription.removeSubscription(attachment.getSubscriptionId());
			}
			
			@Override
			final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
			}
			
			@Override
			boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
				Attachment.AdvancedPaymentSubscriptionCancel attachment = (Attachment.AdvancedPaymentSubscriptionCancel) transaction.getAttachment();
				return isDuplicate(AdvancedPayment.SUBSCRIPTION_CANCEL, Convert.toUnsignedLong(attachment.getSubscriptionId()), duplicates);
			}
			
			@Override
			void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
				Attachment.AdvancedPaymentSubscriptionCancel attachment = (Attachment.AdvancedPaymentSubscriptionCancel) transaction.getAttachment();
				if(attachment.getSubscriptionId() == null) {
					throw new NxtException.NotValidException("Subscription cancel must include subscription id");
				}
				
				Subscription subscription = Subscription.getSubscription(attachment.getSubscriptionId());
				if(subscription == null) {
					throw new NxtException.NotValidException("Subscription cancel must contain current subscription id");
				}
				
				if(!subscription.getSenderId().equals(transaction.getSenderId()) &&
				   !subscription.getRecipientId().equals(transaction.getSenderId())) {
					throw new NxtException.NotValidException("Subscription cancel can only be done by participants");
				}
				
				if(!Subscription.isEnabled()) {
					throw new NxtException.NotYetEnabledException("Subscription cancel not yet enabled");
				}
			}
			
			@Override
			final public boolean hasRecipient() {
				return false;
			}
		};
		
		public final static TransactionType SUBSCRIPTION_PAYMENT = new AdvancedPayment() {
			
			@Override
			public final byte getSubtype() {
				return TransactionType.SUBTYPE_ADVANCED_PAYMENT_SUBSCRIPTION_PAYMENT;
			}
			
			@Override
			Attachment.AdvancedPaymentSubscriptionPayment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentSubscriptionPayment(buffer, transactionVersion);
			}
			
			@Override
			Attachment.AdvancedPaymentSubscriptionPayment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
				return new Attachment.AdvancedPaymentSubscriptionPayment(attachmentData);
			}
			
			@Override
			final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
				return false;
			}
			
			@Override
			final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
			}
			
			@Override
			final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
			}
			
			@Override
			boolean isDuplicate(Transaction transaction, Map<TransactionType, Set<String>> duplicates) {
				return true;
			}
			
			@Override
			void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
				throw new NxtException.NotValidException("Subscription payment never validates");
			}
			
			@Override
			final public boolean hasRecipient() {
				return true;
			}
			
			@Override
			final public boolean isSigned() {
				return false;
			}
		};
	}
	
	  public static abstract class AutomatedTransactions extends TransactionType{
	    	private AutomatedTransactions() {

	    	}
	    	
	    	@Override
	    	public final byte getType(){
	    		return TransactionType.TYPE_AUTOMATED_TRANSACTIONS;
	    	}
	    	
	    	@Override
	    	boolean applyAttachmentUnconfirmed(Transaction transaction,Account senderAccount){
	    		return true;
	    	}
	    	
	    	@Override
	    	void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount){
	    		
	    	}
	    	
	    	 @Override
	         final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
	             if (transaction.getAmountNQT() != 0) {
	                 throw new NxtException.NotValidException("Invalid automated transaction transaction");
	             }
	             doValidateAttachment(transaction);
	         }

	         abstract void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException;

	    	
	    	public static final TransactionType AUTOMATED_TRANSACTION_CREATION = new AutomatedTransactions(){

				@Override
				public byte getSubtype() {
					return TransactionType.SUBTYPE_AT_CREATION;
				}

				@Override
				AbstractAttachment parseAttachment(ByteBuffer buffer,
						byte transactionVersion) throws NotValidException {
					// TODO Auto-generated method stub
					//System.out.println("parsing byte AT attachment");
					AutomatedTransactionsCreation attachment = new Attachment.AutomatedTransactionsCreation(buffer,transactionVersion);
					//System.out.println("byte AT attachment parsed");
					return attachment;
				}

				@Override
				AbstractAttachment parseAttachment(JSONObject attachmentData)
						throws NotValidException {
					// TODO Auto-generated method stub
					//System.out.println("parsing at attachment");
					Attachment.AutomatedTransactionsCreation atCreateAttachment = new Attachment.AutomatedTransactionsCreation(attachmentData);
					//System.out.println("attachment parsed");
					return atCreateAttachment;
				}

				@Override
				void doValidateAttachment(Transaction transaction)
						throws ValidationException {
					//System.out.println("validating attachment");
					if (Nxt.getBlockchain().getLastBlock().getHeight()< Constants.AUTOMATED_TRANSACTION_BLOCK){
						throw new NxtException.NotYetEnabledException("Automated Transactions not yet enabled at height " + Nxt.getBlockchain().getLastBlock().getHeight());
					}
					if (transaction.getSignature() != null && Account.getAccount(transaction.getId()) != null) {
						Account existingAccount = Account.getAccount(transaction.getId());
						if(existingAccount.getPublicKey() != null && !Arrays.equals(existingAccount.getPublicKey(), new byte[32]))
							throw new NxtException.NotValidException("Account with id already exists");
					}
					Attachment.AutomatedTransactionsCreation attachment = (Attachment.AutomatedTransactionsCreation) transaction.getAttachment();
					long totalPages = 0;
					try {
						totalPages = AT_Controller.checkCreationBytes(attachment.getCreationBytes(), Nxt.getBlockchain().getHeight());
					}
					catch(AT_Exception e) {
						throw new NxtException.NotCurrentlyValidException("Invalid AT creation bytes", e);
					}
					long requiredFee = totalPages * AT_Constants.getInstance().COST_PER_PAGE( transaction.getHeight() );
					if (transaction.getFeeNQT() <  requiredFee){
						throw new NxtException.NotValidException("Insufficient fee for AT creation. Minimum: " + Convert.toUnsignedLong(requiredFee / Constants.ONE_NXT));
					}
					if(Nxt.getBlockchain().getHeight() >= Constants.AT_FIX_BLOCK_3) {
						if(attachment.getName().length() > Constants.MAX_AUTOMATED_TRANSACTION_NAME_LENGTH) {
							throw new NxtException.NotValidException("Name of automated transaction over size limit");
						}
						if(attachment.getDescription().length() > Constants.MAX_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH) {
							throw new NxtException.NotValidException("Description of automated transaction over size limit");
						}
					}
					//System.out.println("validating success");
				}

				@Override
				void applyAttachment(Transaction transaction,
						Account senderAccount, Account recipientAccount) {
					// TODO Auto-generated method stub
	                Attachment.AutomatedTransactionsCreation attachment = (Attachment.AutomatedTransactionsCreation) transaction.getAttachment();
	                Long atId = transaction.getId();
	                //System.out.println("Applying AT attachent");
	                AT.addAT( transaction.getId() , transaction.getSenderId() , attachment.getName() , attachment.getDescription() , attachment.getCreationBytes() , transaction.getHeight() ); 
	                //System.out.println("At with id "+atId+" successfully applied");
				}


				@Override
				public boolean hasRecipient() {
					// TODO Auto-generated method stub
					return false;
				}
	    	};
	    	
	    	public static final TransactionType AT_PAYMENT = new AutomatedTransactions() {
	    		@Override
	            public final byte getSubtype() {
	                return TransactionType.SUBTYPE_AT_NXT_PAYMENT;
	            }

	            @Override
	            AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
	                return Attachment.AT_PAYMENT;
	            }

	            @Override
	            AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
	                return Attachment.AT_PAYMENT;
	            }

	            @Override
	            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
	                /*if (transaction.getAmountNQT() <= 0 || transaction.getAmountNQT() >= Constants.MAX_BALANCE_NQT) {
	                    throw new NxtException.NotValidException("Invalid ordinary payment");
	                }*/
	            	throw new NxtException.NotValidException("AT payment never validates");
	            }

				@Override
				void applyAttachment(Transaction transaction,
						Account senderAccount, Account recipientAccount) {
					// TODO Auto-generated method stub
					
				}


				@Override
				public boolean hasRecipient() {
					return true;
				}
				
				@Override
				final public boolean isSigned() {
					return false;
				}
	    	};
	    	
	    }

    long minimumFeeNQT(int height, int appendagesSize) {
        if (height < BASELINE_FEE_HEIGHT) {
            return 0; // No need to validate fees before baseline block
        }
        Fee fee;
        if (height >= NEXT_FEE_HEIGHT) {
            fee = getNextFee();
        } else {
            fee = getBaselineFee();
        }
        return Convert.safeAdd(fee.getConstantFee(), Convert.safeMultiply(appendagesSize, fee.getAppendagesFee()));
    }

    protected Fee getBaselineFee() {
        return BASELINE_FEE;
    }

    protected Fee getNextFee() {
        return NEXT_FEE;
    }

    public static final class Fee {
        private final long constantFee;
        private final long appendagesFee;

        public Fee(long constantFee, long appendagesFee) {
            this.constantFee = constantFee;
            this.appendagesFee = appendagesFee;
        }

        public long getConstantFee() {
            return constantFee;
        }

        public long getAppendagesFee() {
            return appendagesFee;
        }

        @Override
        public String toString() {
            return "Fee{" +
                    "constantFee=" + constantFee +
                    ", appendagesFee=" + appendagesFee +
                    '}';
        }
    }

}
