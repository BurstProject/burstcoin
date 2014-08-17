package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.MiningPlot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import fr.cryptohash.Shabal256;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final Long previousBlockId;
    private final byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final long totalAmountNQT;
    private final long totalFeeNQT;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private final List<Long> transactionIds;
    private final List<TransactionImpl> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget = Constants.INITIAL_BASE_TARGET;
    private volatile Long nextBlockId;
    private int height = -1;
    private volatile Long id;
    private volatile String stringId = null;
    private volatile Long generatorId;
    private long nonce;


    BlockImpl(int version, int timestamp, Long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions, long nonce)
            throws NxtException.ValidationException {

        if (transactions.size() > Constants.MAX_NUMBER_OF_TRANSACTIONS) {
            throw new NxtException.ValidationException("attempted to create a block with " + transactions.size() + " transactions");
        }

        if (payloadLength > Constants.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
            throw new NxtException.ValidationException("attempted to create a block with payloadLength " + payloadLength);
        }

        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmountNQT = totalAmountNQT;
        this.totalFeeNQT = totalFeeNQT;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;

        this.previousBlockHash = previousBlockHash;
        this.blockTransactions = Collections.unmodifiableList(transactions);
        List<Long> transactionIds = new ArrayList<>(this.blockTransactions.size());
        Long previousId = Long.MIN_VALUE;
        for (Transaction transaction : this.blockTransactions) {
            if (transaction.getId() < previousId) {
                throw new NxtException.ValidationException("Block transactions are not sorted!");
            }
            transactionIds.add(transaction.getId());
            previousId = transaction.getId();
        }
        this.transactionIds = Collections.unmodifiableList(transactionIds);
        this.nonce = nonce;
    }

    BlockImpl(int version, int timestamp, Long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength,
              byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, List<TransactionImpl> transactions, BigInteger cumulativeDifficulty,
              long baseTarget, Long nextBlockId, int height, Long id, long nonce)
            throws NxtException.ValidationException {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, previousBlockHash, transactions, nonce);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public Long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public long getTotalAmountNQT() {
        return totalAmountNQT;
    }

    @Override
    public long getTotalFeeNQT() {
        return totalFeeNQT;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public List<Long> getTransactionIds() {
        return transactionIds;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<TransactionImpl> getTransactions() {
        return blockTransactions;
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public Long getNextBlockId() {
        return nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public Long getId() {
        if (id == null) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
            byte[] hash = Crypto.sha256().digest(getBytes());
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Convert.toUnsignedLong(id);
            }
        }
        return stringId;
    }

    @Override
    public Long getGeneratorId() {
        if (generatorId == null) {
            generatorId = Account.getId(generatorPublicKey);
        }
        return generatorId;
    }
    
    @Override
    public Long getNonce() {
    	return nonce;
    }
    
    @Override
    public int getScoopNum() {
    	ByteBuffer posbuf = ByteBuffer.allocate(32 + 8);
		posbuf.put(generationSignature);
		posbuf.putLong(getHeight());
		Shabal256 md = new Shabal256();
		md.update(posbuf.array());
		BigInteger hashnum = new BigInteger(1, md.digest());
		int scoopNum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
		return scoopNum;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId().equals(((BlockImpl)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Convert.toUnsignedLong(previousBlockId));
        json.put("totalAmountNQT", totalAmountNQT);
        json.put("totalFeeNQT", totalFeeNQT);
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorPublicKey", Convert.toHexString(generatorPublicKey));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        if (version > 1) {
            json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        }
        json.put("blockSignature", Convert.toHexString(blockSignature));
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : blockTransactions) {
            transactionsData.add(transaction.getJSONObject());
        }
        json.put("transactions", transactionsData);
        json.put("nonce", Convert.toUnsignedLong(nonce));
        return json;
    }

    byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (version < 3 ? (4 + 4) : (8 + 8)) + 4 + 32 + 32 + (32 + 32) + 8 + 64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(Convert.nullToZero(previousBlockId));
        buffer.putInt(blockTransactions.size());
        if (version < 3) {
            buffer.putInt((int)(totalAmountNQT / Constants.ONE_NXT));
            buffer.putInt((int)(totalFeeNQT / Constants.ONE_NXT));
        } else {
            buffer.putLong(totalAmountNQT);
            buffer.putLong(totalFeeNQT);
        }
        buffer.putInt(payloadLength);
        buffer.put(payloadHash);
        buffer.put(generatorPublicKey);
        buffer.put(generationSignature);
        if (version > 1) {
            buffer.put(previousBlockHash);
        }
        buffer.putLong(nonce);
        buffer.put(blockSignature);
        return buffer.array();
    }

    void sign(String secretPhrase) {
        if (blockSignature != null) {
            throw new IllegalStateException("Block already signed");
        }
        blockSignature = new byte[64];
        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);
        blockSignature = Crypto.sign(data2, secretPhrase);
    }

    boolean verifyBlockSignature() {
        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);

        return Crypto.verify(blockSignature, data2, generatorPublicKey, version >= 3);

    }

    boolean verifyGenerationSignature() throws BlockchainProcessor.BlockOutOfOrderException {

        try {

            BlockImpl previousBlock = (BlockImpl)Nxt.getBlockchain().getBlock(this.previousBlockId);
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing");
            }

            //Account account = Account.getAccount(getGeneratorId());

            ByteBuffer gensigbuf = ByteBuffer.allocate(32 + 8);
            gensigbuf.put(previousBlock.getGenerationSignature());
            gensigbuf.putLong(previousBlock.getGeneratorId());
            
            Shabal256 md = new Shabal256();
            md.update(gensigbuf.array());
            byte[] correctGenerationSignature = md.digest();
            if(!Arrays.equals(generationSignature, correctGenerationSignature)) {
            	return false;
            }
            
            // verify poc also
            MiningPlot plot = new MiningPlot(getGeneratorId(), nonce);
            
            ByteBuffer posbuf = ByteBuffer.allocate(32 + 8);
    		posbuf.put(correctGenerationSignature);
    		posbuf.putLong(previousBlock.getHeight() + 1);
    		md.reset();
    		md.update(posbuf.array());
    		BigInteger hashnum = new BigInteger(1, md.digest());
    		int scoopNum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
    		
    		md.reset();
            md.update(correctGenerationSignature);
            plot.hashScoop(md, scoopNum);
            byte[] hash = md.digest();
            BigInteger hit = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            BigInteger hitTime = hit.divide(BigInteger.valueOf(previousBlock.getBaseTarget()));
            
            int elapsedTime = timestamp - previousBlock.timestamp;
            
            return BigInteger.valueOf(elapsedTime).compareTo(hitTime) > 0;

        } catch (RuntimeException e) {

            Logger.logMessage("Error verifying block generation signature", e);
            return false;

        }

    }

    void apply() {
        Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
        generatorAccount.apply(generatorPublicKey, this.height);
        generatorAccount.addToBalanceAndUnconfirmedBalanceNQT(totalFeeNQT + getBlockReward());
        generatorAccount.addToForgedBalanceNQT(totalFeeNQT + getBlockReward());
    }

    void undo() {
        Account generatorAccount = Account.getAccount(getGeneratorId());
        generatorAccount.undo(getHeight());
        generatorAccount.addToBalanceAndUnconfirmedBalanceNQT(-totalFeeNQT + (getBlockReward() * -1));
        generatorAccount.addToForgedBalanceNQT(-totalFeeNQT + (getBlockReward() * -1));
    }
    
    @Override
    public long getBlockReward() {
    	if(this.height == 0 || this.height >= 1944000) {
    		return 0;
    	}
    	int month = this.height / 10800;
    	long reward = BigInteger.valueOf(10000)
    			.multiply(BigInteger.valueOf(95).pow(month))
    			.divide(BigInteger.valueOf(100).pow(month)).longValue() * Constants.ONE_NXT;
    	
    	return reward;
    }

    void setPrevious(BlockImpl previousBlock) {
        if (previousBlock != null) {
            if (! previousBlock.getId().equals(getPreviousBlockId())) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = previousBlock.getHeight() + 1;
            this.calculateBaseTarget(previousBlock);
        } else {
            this.height = 0;
        }
        for (TransactionImpl transaction : blockTransactions) {
            transaction.setBlock(this);
        }
    }

    private void calculateBaseTarget(BlockImpl previousBlock) {

        if (this.getId().equals(Genesis.GENESIS_BLOCK_ID) && previousBlockId == null) {
            baseTarget = Constants.INITIAL_BASE_TARGET;
            cumulativeDifficulty = BigInteger.ZERO;
        } else if(this.height < 4) {
        	baseTarget = Constants.INITIAL_BASE_TARGET;
        	cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET)));
        } else if(this.height < Constants.BURST_DIFF_ADJUST_CHANGE_BLOCK){
        	Block itBlock = previousBlock;
        	BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
        	do {
        		itBlock = Nxt.getBlockchain().getBlock(itBlock.getPreviousBlockId());
        		avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(itBlock.getBaseTarget()));
        	} while(itBlock.getHeight() > this.height - 4);
        	avgBaseTarget = avgBaseTarget.divide(BigInteger.valueOf(4));
        	long difTime = this.timestamp - itBlock.getTimestamp();
        	
            long curBaseTarget = avgBaseTarget.longValue();
            long newBaseTarget = BigInteger.valueOf(curBaseTarget)
                    .multiply(BigInteger.valueOf(difTime))
                    .divide(BigInteger.valueOf(240 * 4)).longValue();
            if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
                newBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (newBaseTarget < (curBaseTarget * 9 / 10)) {
            	newBaseTarget = curBaseTarget * 9 / 10;
            }
            if (newBaseTarget == 0) {
                newBaseTarget = 1;
            }
            long twofoldCurBaseTarget = curBaseTarget * 11 / 10;
            if (twofoldCurBaseTarget < 0) {
                twofoldCurBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (newBaseTarget > twofoldCurBaseTarget) {
                newBaseTarget = twofoldCurBaseTarget;
            }
            baseTarget = newBaseTarget;
            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
        }
        else {
        	Block itBlock = previousBlock;
        	BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
        	int blockCounter = 1;
        	do {
        		itBlock = Nxt.getBlockchain().getBlock(itBlock.getPreviousBlockId());
        		blockCounter++;
        		avgBaseTarget = (avgBaseTarget.multiply(BigInteger.valueOf(blockCounter))
        							.add(BigInteger.valueOf(itBlock.getBaseTarget())))
        							.divide(BigInteger.valueOf(blockCounter + 1));
        	} while(blockCounter < 24);
        	long difTime = this.timestamp - itBlock.getTimestamp();
        	long targetTimespan = 24 * 4 * 60;
        	
        	if(difTime < targetTimespan /2) {
        		difTime = targetTimespan /2;
        	}
        	
        	if(difTime > targetTimespan * 2) {
        		difTime = targetTimespan * 2;
        	}
        	
        	long curBaseTarget = previousBlock.getBaseTarget();
            long newBaseTarget = avgBaseTarget
                    .multiply(BigInteger.valueOf(difTime))
                    .divide(BigInteger.valueOf(targetTimespan)).longValue();
            
            if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
                newBaseTarget = Constants.MAX_BASE_TARGET;
            }
            
            if (newBaseTarget == 0) {
                newBaseTarget = 1;
            }
            
            if(newBaseTarget < curBaseTarget * 8 / 10) {
            	newBaseTarget = curBaseTarget * 8 / 10;
            }
            
            if(newBaseTarget > curBaseTarget * 12 / 10) {
            	newBaseTarget = curBaseTarget * 12 / 10;
            }
            
            baseTarget = newBaseTarget;
            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
        }
    }

}
