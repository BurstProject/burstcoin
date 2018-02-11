package brs.util;

import brs.Burst;
import brs.services.PropertyService;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import brs.Block;
import brs.BlockImpl;
import brs.BlockchainImpl;
import brs.Constants;

public final class DownloadCacheImpl {
  public final int BLOCKCACHEMB;

  protected static final Map<Long, Block> blockCache = new LinkedHashMap<>();
  protected static final Map<Long, Long> reverseCache = new LinkedHashMap<>();
  protected static final List<Long> unverified = new LinkedList<>();

  private static final Logger logger = LoggerFactory.getLogger(DownloadCacheImpl.class);
  private static int blockCacheSize = 0;

  private Long LastBlockId = null;
  private int LastHeight = -1;
  BigInteger HigestCumulativeDifficulty = BigInteger.ZERO;

  private final StampedLock sl = new StampedLock();
  
  
  public DownloadCacheImpl() {
    PropertyService propertyService = Burst.getPropertyService();
    this.BLOCKCACHEMB = propertyService.getIntProperty("brs.blockCacheMB") == 0 ? 40 : propertyService.getIntProperty("brs.blockCacheMB");
  }

  public int getChainHeight() {
    long stamp = sl.tryOptimisticRead();
    int retVal = LastHeight;
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal = LastHeight;
      } finally {
         sl.unlockRead(stamp);
      }
   }
   if (retVal > -1) {
      return retVal;
    }
    return Burst.getBlockchain().getHeight();
  }
  
  public int getBlockCacheSize() {
    long stamp = sl.tryOptimisticRead();
    int retVal = blockCacheSize;
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal = blockCacheSize;
      } finally {
         sl.unlockRead(stamp);
      }
   }
    return retVal;
  }
 
  
  public boolean IsFull() {
    long stamp = sl.tryOptimisticRead();
    int retVal = blockCacheSize;
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal = blockCacheSize;
      } finally {
         sl.unlockRead(stamp);
      }
    }
    return retVal > BLOCKCACHEMB * 1024 * 1024;
  }

  public int getUnverifiedSize() {
    long stamp = sl.tryOptimisticRead();
    int retVal = unverified.size();
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal = unverified.size();
      } finally {
         sl.unlockRead(stamp);
      }
    }
    return retVal;
  }

  public BigInteger getCumulativeDifficulty() {
    long stamp = sl.tryOptimisticRead();
    Long lbID = LastBlockId;
    BigInteger retVal = HigestCumulativeDifficulty;
    
   
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        lbID = LastBlockId;
      } finally {
         sl.unlockRead(stamp);
      }
    }
    if (lbID != null) {
      return retVal;
    }
    setLastVars();
    stamp = sl.tryOptimisticRead();
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal = HigestCumulativeDifficulty;
      } finally {
         sl.unlockRead(stamp);
      }
    }
    return retVal;
  }

  public long GetUnverifiedBlockId(int BlockId) {
    long stamp = sl.tryOptimisticRead();
    long reVal = unverified.get(0);
    
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        reVal = unverified.get(0);
      } finally {
         sl.unlockRead(stamp);
      }
    }
    return reVal;
  }

  public void removeUnverified(long BlockId) {
    long stamp = sl.writeLock();
    try {
      unverified.remove(BlockId);
    } finally {
      sl.unlockWrite(stamp);
    }
  }

/*  public void VerifyCacheIntegrity() {
    if (blockCache.size() > 0) {
      long checkBlockId = getLastBlockId();
      while (checkBlockId != Burst.getBlockchain().getLastBlock().getId()) {
        if (blockCache.get(checkBlockId) == null) {
          ResetCache();
          break;
        }
        checkBlockId = blockCache.get(checkBlockId).getPreviousBlockId();
      }
    }
  }*/

  public void ResetCache() {
    long stamp = sl.writeLock();
    try {
      blockCache.clear();
      reverseCache.clear();
      unverified.clear();
      blockCacheSize = 0;
    } finally {
      sl.unlockWrite(stamp);
    }
    setLastVars();
  }
  public int getBlockHeight(long BlockId) {
    long stamp = sl.tryOptimisticRead();
    int retVal;
    
    if (blockCache.containsKey(BlockId)) {
      retVal = blockCache.get(BlockId).getHeight();
      if (sl.validate(stamp)) {
        return retVal;
      }
    }
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        if (blockCache.containsKey(BlockId)) {
          return blockCache.get(BlockId).getHeight();
        }
      } finally {
         sl.unlockRead(stamp);
      }
    }
    return Burst.getBlockchain().getBlock(BlockId).getHeight();
  }

  public BlockImpl GetBlock(long BlockId) {
    long stamp = sl.tryOptimisticRead();
    BlockImpl retVal = GetBlockInt(BlockId);
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal = GetBlockInt(BlockId);
      } finally {
         sl.unlockRead(stamp);
      }
    }
    if(retVal != null) {
      return retVal;
    }
    if (Burst.getBlockchain().hasBlock(BlockId)) {
      return Burst.getBlockchain().getBlock(BlockId);
    }
    return null;
  }
  private BlockImpl GetBlockInt(long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return (BlockImpl) blockCache.get(BlockId);
    }
    return null;
  }
    
/*  public BlockImpl GetBlock(Long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return (BlockImpl) blockCache.get(BlockId);
    }
    if (Burst.getBlockchain().hasBlock(BlockId)) {
      return Burst.getBlockchain().getBlock(BlockId);
    }

    return null;
  }
*/
  
  public BlockImpl GetNextBlock(long prevBlockId) {
    long stamp = sl.tryOptimisticRead();
    BlockImpl retVal = GetNextBlockInt(prevBlockId);
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal = GetNextBlockInt(prevBlockId);
      } finally {
        sl.unlockRead(stamp);
      }
    }
    return retVal;
  }
  private BlockImpl GetNextBlockInt(long prevBlockId) {
    if (reverseCache.containsKey(prevBlockId)) {
      return (BlockImpl) blockCache.get(reverseCache.get(prevBlockId));
    }
    return null;
  }

 /* public void WaitForMapToBlockChain() {
    synchronized (this) {
      while (!reverseCache.containsKey(Burst.getBlockchain().getLastBlock().getId())) {
        try {
          printDebug();
          this.wait(2000);
        } catch (InterruptedException ignore) {
          logger.trace("Interrupted: ", ignore);
          Thread.currentThread().interrupt();
        }
      }
    }
  }
*/
  
  public boolean HasBlock(long BlockId) {
    long stamp = sl.tryOptimisticRead();
    boolean retVal =  blockCache.containsKey(BlockId);
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        retVal =  blockCache.containsKey(BlockId);
      } finally {
        sl.unlockRead(stamp);
      }
    }
    if (retVal) {
      return true;
    }
    return Burst.getBlockchain().hasBlock(BlockId);
  }

  public boolean CanBeFork(long oldBlockId) {
    int curHeight = getChainHeight();
    BlockImpl block = null;
    long stamp = sl.tryOptimisticRead();
    block = GetBlockInt(oldBlockId);
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        block = GetBlockInt(oldBlockId);
      } finally {
         sl.unlockRead(stamp);
      }
    }
    if (block == null && Burst.getBlockchain().hasBlock(oldBlockId)) {
      block = Burst.getBlockchain().getBlock(oldBlockId);
    }
    if (block == null) {
      return false;
    }
    return (curHeight - block.getHeight()) <= Constants.MAX_ROLLBACK;
  }

  public void AddBlock(BlockImpl block) {
    long stamp = sl.writeLock();
    try {
      blockCache.put(block.getId(), block);
      reverseCache.put(block.getPreviousBlockId(), block.getId());
      unverified.add(block.getId());
      blockCacheSize += block.getByteLength();
      LastBlockId = block.getId();
      LastHeight = block.getHeight();
      HigestCumulativeDifficulty = block.getCumulativeDifficulty();
    } finally {
      sl.unlockWrite(stamp);
    }
  }

 /* public void SetCacheBackTo(long BadBlockId) {
    // Starting from lowest point and erase all up to last block.
    if (blockCache.containsKey(BadBlockId)) {
      BlockImpl badBlock;
      long id;
      badBlock = (BlockImpl) blockCache.get(BadBlockId);
      reverseCache.remove(badBlock.getPreviousBlockId());
      blockCacheSize -= badBlock.getByteLength();
      blockCache.remove(BadBlockId);
      while (reverseCache.containsKey(BadBlockId)) {
        id = reverseCache.get(BadBlockId);
        reverseCache.remove(BadBlockId);
        blockCacheSize -= ((BlockImpl) blockCache.get(id)).getByteLength();
        blockCache.remove(id);
        BadBlockId = id;
      }
      setLastVars();
    }
  }*/

  public boolean RemoveBlock(BlockImpl block) {
    long stamp = sl.tryOptimisticRead();
    boolean chkVal = blockCache.containsKey(block.getId());
    long lastId = LastBlockId;
    
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        chkVal = blockCache.containsKey(block.getId());
        lastId = LastBlockId;
      } finally {
         sl.unlockRead(stamp);
      }
    }
    if (chkVal) { // make sure there is something to remove
      stamp = sl.writeLock();
      try {
        reverseCache.remove(block.getPreviousBlockId());
        blockCache.remove(block.getId());
        blockCacheSize -= block.getByteLength();
      } finally {
        sl.unlockWrite(stamp);
      }
      if (block.getId() == lastId) {
        setLastVars();
      }
      return true;
    }
    return false;
  }

  public int getPoCVersion(long blockId) {
    BlockImpl blockImpl = GetBlock(blockId);
    return (blockImpl == null || blockImpl.getHeight() < Constants.POC2_START_BLOCK) ? 1 : 2;
  }
  
  public long getLastBlockId() {
    Long lId = getLastCacheId();
    if (lId != null) {
      return lId;
    }
    return Burst.getBlockchain().getLastBlock().getId();
  }
  private Long getLastCacheId() {
    long stamp = sl.tryOptimisticRead();
    Long lId = LastBlockId;
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        lId = LastBlockId;
      } finally {
        sl.unlockRead(stamp);
      }
    }
    return lId;
  }
  
  
  public BlockImpl getLastBlock() {
    Long iLd = getLastCacheId();
    if (iLd != null) {
      long stamp = sl.tryOptimisticRead();
      BlockImpl retBlock = (BlockImpl) blockCache.get(iLd);
      if (!sl.validate(stamp)) {
        stamp = sl.readLock();
        try {
          retBlock = (BlockImpl) blockCache.get(iLd);
        } finally {
          sl.unlockRead(stamp);
        }
      }
      return retBlock;
    }
    return Burst.getBlockchain().getLastBlock();
  }

  public int size() {
    long stamp = sl.tryOptimisticRead();
    int size = blockCache.size();
    if (!sl.validate(stamp)) {
      stamp = sl.readLock();
      try {
        size = blockCache.size();
      } finally {
        sl.unlockRead(stamp);
      }
    }
    return size;
  }

  public void printDebug() {
    if (reverseCache.size() > 0) {
      logger.debug("BlockCache First block key:" + blockCache.keySet().toArray()[0]);
      logger.debug("revCache First block key:" + reverseCache.keySet().toArray()[0]);
      logger.debug("revCache First block Val:" + reverseCache.get(reverseCache.keySet().toArray()[0]));
      logger.debug("BlockCache size:" + blockCache.size());
      logger.debug("revCache size:" + reverseCache.size());
    } else {
      logger.debug("BlockCache size:" + blockCache.size());
      logger.debug("revCache size:" + reverseCache.size());
    }
  }

  private void setLastVars() {
    long stamp = sl.writeLock();
    try {
      if (blockCache.size() > 0) {
        LastBlockId =
            blockCache.get(blockCache.keySet().toArray()[blockCache.keySet().size() - 1]).getId();
        LastHeight = blockCache.get(LastBlockId).getHeight();
        HigestCumulativeDifficulty = blockCache.get(LastBlockId).getCumulativeDifficulty();
      } else {
        LastBlockId = Burst.getBlockchain().getLastBlock().getId();
        LastHeight = Burst.getBlockchain().getHeight();
        HigestCumulativeDifficulty = Burst.getBlockchain().getLastBlock().getCumulativeDifficulty();
      }
    } finally {
      sl.unlockWrite(stamp);
    }
  }
}
