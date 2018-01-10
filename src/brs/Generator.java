package brs;

import java.math.BigInteger;
import java.util.Collection;

import brs.util.Listener;

public interface Generator {

  public enum Event {
    GENERATION_DEADLINE, START_FORGING, STOP_FORGING
  }

  void init();

  boolean addListener(Listener<GeneratorState> listener, Event eventType);

  boolean removeListener(Listener<GeneratorState> listener, Event eventType);

  GeneratorState addNonce(String secretPhrase, Long nonce);

  GeneratorState addNonce(String secretPhrase, Long nonce, byte[] publicKey);

  Collection<? extends GeneratorState> getAllGenerators();

  byte[] calculateGenerationSignature(byte[] lastGenSig, long lastGenId);

  int calculateScoop(byte[] genSig, long height);

  BigInteger calculateHit(long accountId, long nonce, byte[] genSig, int scoop, int blockHeight);

  BigInteger calculateHit(long accountId, long nonce, byte[] genSig, byte[] scoopData);

  BigInteger calculateDeadline(long accountId, long nonce, byte[] genSig, int scoop, long baseTarget, int blockHeight);

  interface GeneratorState {
    byte[] getPublicKey();

    Long getAccountId();

    BigInteger getDeadline();

    long getBlock();
  }
}
