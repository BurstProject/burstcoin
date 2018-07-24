package brs;

import brs.fluxcapacitor.FluxCapacitor;
import brs.services.TimeService;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brs.crypto.Crypto;
import brs.crypto.hash.Shabal256;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Listeners;
import brs.util.MiningPlot;
import brs.util.ThreadPool;

public final class GeneratorImpl implements Generator {

  private static final Logger logger = LoggerFactory.getLogger(GeneratorImpl.class);

  private static final Listeners<GeneratorState, Event> listeners = new Listeners<>();

  private static final ConcurrentMap<Long, GeneratorStateImpl> generators = new ConcurrentHashMap<>();
  private static final Collection<? extends GeneratorState> allGenerators = Collections.unmodifiableCollection(generators.values());

  private Blockchain blockchain;

  private final Runnable generateBlockThread(BlockchainProcessor blockchainProcessor) {
    return () -> {

      try {
        if (blockchainProcessor.isScanning()) {
          return;
        }
        try {
          long currentBlock = blockchain.getLastBlock().getHeight();
          Iterator<Entry<Long, GeneratorStateImpl>> it = generators.entrySet().iterator();
          while (it.hasNext()) {
            Entry<Long, GeneratorStateImpl> generator = it.next();
            if (currentBlock < generator.getValue().getBlock()) {
              generator.getValue().forge(blockchainProcessor);
            } else {
              it.remove();
            }
          }
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
          logger.debug("Error in block generation thread", e);
        }
      } catch (Throwable t) {
        logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
        System.exit(1);
      }

    };
  }
  private TimeService timeService;
  private FluxCapacitor fluxCapacitor;

  public GeneratorImpl(Blockchain blockchain, TimeService timeService, FluxCapacitor fluxCapacitor) {
    this.blockchain = blockchain;
    this.timeService = timeService;
    this.fluxCapacitor = fluxCapacitor;
  }

  @Override
  public void generateForBlockchainProcessor(ThreadPool threadPool, BlockchainProcessor blockchainProcessor) {
    threadPool.scheduleThread("GenerateBlocks", generateBlockThread(blockchainProcessor), 500, TimeUnit.MILLISECONDS);
  }

  void clear() {
  }

  @Override
  public boolean addListener(Listener<GeneratorState> listener, Event eventType) {
    return listeners.addListener(listener, eventType);
  }

  @Override
  public boolean removeListener(Listener<GeneratorState> listener, Event eventType) {
    return listeners.removeListener(listener, eventType);
  }

  @Override
  public GeneratorState addNonce(String secretPhrase, Long nonce) {
    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    return addNonce(secretPhrase, nonce, publicKey);
  }

  @Override
  public GeneratorState addNonce(String secretPhrase, Long nonce, byte[] publicKey) {
    byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
    Long id = Convert.fullHashToId(publicKeyHash);

    GeneratorStateImpl generator = new GeneratorStateImpl(secretPhrase, nonce, publicKey, id);
    GeneratorStateImpl curGen = generators.get(id);
    if (curGen == null || generator.getBlock() > curGen.getBlock() || generator.getDeadline().compareTo(curGen.getDeadline()) < 0) {
      generators.put(id, generator);
      listeners.notify(generator, Event.START_FORGING);
      logger.debug("Account " + Convert.toUnsignedLong(id) + " started mining, deadline " + generator.getDeadline() + " seconds");
    } else {
      logger.debug("Account " + Convert.toUnsignedLong(id) + " already has better nonce");
    }

    return generator;
  }

  @Override
  public Collection<? extends GeneratorState> getAllGenerators() {
    return allGenerators;
  }

  @Override
  public byte[] calculateGenerationSignature(byte[] lastGenSig, long lastGenId) {
    ByteBuffer gensigbuf = ByteBuffer.allocate(32 + 8);
    gensigbuf.put(lastGenSig);
    gensigbuf.putLong(lastGenId);

    Shabal256 md = new Shabal256();
    md.update(gensigbuf.array());
    return md.digest();
  }

  @Override
  public int calculateScoop(byte[] genSig, long height) {
    ByteBuffer posbuf = ByteBuffer.allocate(32 + 8);
    posbuf.put(genSig);
    posbuf.putLong(height);

    Shabal256 md = new Shabal256();
    md.update(posbuf.array());
    BigInteger hashnum = new BigInteger(1, md.digest());
    return hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
  }

  @Override
  public BigInteger calculateHit(long accountId, long nonce, byte[] genSig, int scoop, int blockHeight) {

    MiningPlot plot = new MiningPlot(accountId, nonce, blockHeight, fluxCapacitor);

    Shabal256 md = new Shabal256();
    md.update(genSig);
    plot.hashScoop(md, scoop);
    byte[] hash = md.digest();
    return new BigInteger(1, new byte[] { hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0] });
  }

  @Override
  public BigInteger calculateHit(long accountId, long nonce, byte[] genSig, byte[] scoopData) {
    Shabal256 md = new Shabal256();
    md.update(genSig);
    md.update(scoopData);
    byte[] hash = md.digest();
    return new BigInteger(1, new byte[] { hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0] });
  }

  @Override
  public BigInteger calculateDeadline(long accountId, long nonce, byte[] genSig, int scoop, long baseTarget, int blockHeight) {
    BigInteger hit = calculateHit(accountId, nonce, genSig, scoop, blockHeight);
    return hit.divide(BigInteger.valueOf(baseTarget));
  }

  public class GeneratorStateImpl implements GeneratorState {
    private final Long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile BigInteger deadline;
    private final long nonce;
    private final long block;

    private GeneratorStateImpl(String secretPhrase, Long nonce, byte[] publicKey, Long account) {
      this.secretPhrase = secretPhrase;
      this.publicKey = publicKey;
      // need to store publicKey in addition to accountId, because the account may not have had its publicKey set yet
      this.accountId = account;
      this.nonce = nonce;

      Block lastBlock = blockchain.getLastBlock();

      this.block = (long) lastBlock.getHeight() + 1;

      byte[] lastGenSig = lastBlock.getGenerationSignature();
      Long lastGenerator = lastBlock.getGeneratorId();

      byte[] newGenSig = calculateGenerationSignature(lastGenSig, lastGenerator);

      int scoopNum = calculateScoop(newGenSig, lastBlock.getHeight() + 1L);

      deadline = calculateDeadline(accountId, nonce, newGenSig, scoopNum, lastBlock.getBaseTarget(), lastBlock.getHeight() + 1);
    }

    @Override
    public byte[] getPublicKey() {
      return publicKey;
    }

    @Override
    public Long getAccountId() {
      return accountId;
    }

    @Override
    public BigInteger getDeadline() {
      return deadline;
    }

    @Override
    public long getBlock() {
      return block;
    }

    private void forge(BlockchainProcessor blockchainProcessor) throws BlockchainProcessor.BlockNotAcceptedException {
      Block lastBlock = blockchain.getLastBlock();

      int elapsedTime = timeService.getEpochTime() - lastBlock.getTimestamp();
      if (BigInteger.valueOf(elapsedTime).compareTo(deadline) > 0) {
        blockchainProcessor.generateBlock(secretPhrase, publicKey, nonce);
      }
    }
  }

  public static class MockGeneratorImpl implements Generator {

    private final Listeners<GeneratorState, Event> listeners = new Listeners<>();

    private BlockchainProcessor blockchainProcessor;

    @Override
    public void generateForBlockchainProcessor(ThreadPool threadPool, BlockchainProcessor blockchainProcessor) {
      this.blockchainProcessor = blockchainProcessor;
    }

    @Override
    public boolean addListener(Listener<GeneratorState> listener, Event eventType) {
      return listeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<GeneratorState> listener, Event eventType) {
      return listeners.removeListener(listener, eventType);
    }

    @Override
    public GeneratorState addNonce(String secretPhrase, Long nonce) {
      byte[] publicKey = Crypto.getPublicKey(secretPhrase);
      return addNonce(secretPhrase, nonce, publicKey);
    }

    @Override
    public GeneratorState addNonce(String secretPhrase, Long nonce, byte[] publicKey) {
      try {
        blockchainProcessor.generateBlock(secretPhrase, publicKey, nonce);
      } catch (BlockchainProcessor.BlockNotAcceptedException e) {
        logger.info(e.getMessage(), e);
      }
      return new MockGeneratorStateImpl();
    }

    @Override
    public Collection<? extends GeneratorState> getAllGenerators() {
      return Collections.EMPTY_LIST;
    }

    @Override
    public byte[] calculateGenerationSignature(byte[] lastGenSig, long lastGenId) {
      return new byte[32];
    }

    @Override
    public int calculateScoop(byte[] genSig, long height) {
      return 0;
    }

    @Override
    public BigInteger calculateHit(long accountId, long nonce, byte[] genSig, int scoop, int blockHeight) {
      return BigInteger.valueOf(0);
    }

    @Override
    public BigInteger calculateHit(long accountId, long nonce, byte[] genSig, byte[] scoopData) {
      return BigInteger.ZERO;
    }

    @Override
    public BigInteger calculateDeadline(long accountId, long nonce, byte[] genSig, int scoop, long baseTarget, int blockHeight) {
      return BigInteger.valueOf(0);
    }

    public class MockGeneratorStateImpl implements GeneratorState {

      @Override
      public byte[] getPublicKey() {
        return new byte[32];
      }

      @Override
      public Long getAccountId() {
        return 0L;
      }

      @Override
      public BigInteger getDeadline() {
        return new BigInteger("0");
      }

      @Override
      public long getBlock() {
        return 0;
      }
    }
  }
}
