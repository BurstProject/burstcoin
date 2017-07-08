package nxt;

import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.List;

public interface Block {

    int getVersion();

    long getId();

    String getStringId();

    int getHeight();

    int getTimestamp();

    long getGeneratorId();
    
    Long getNonce();
    
    int getScoopNum();

    byte[] getGeneratorPublicKey();
    
    byte[] getBlockHash();

    long getPreviousBlockId();

    byte[] getPreviousBlockHash();

    long getNextBlockId();

    long getTotalAmountNQT();

    long getTotalFeeNQT();

    int getPayloadLength();

    byte[] getPayloadHash();

    List<? extends Transaction> getTransactions();

    byte[] getGenerationSignature();

    byte[] getBlockSignature();

    long getBaseTarget();
    
    long getBlockReward();

    BigInteger getCumulativeDifficulty();

    JSONObject getJSONObject();
    
    byte[] getBlockATs();


}
