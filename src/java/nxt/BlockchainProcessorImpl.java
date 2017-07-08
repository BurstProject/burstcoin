package nxt;

import nxt.at.AT_Block;
import nxt.at.AT_Controller;
import nxt.at.AT_Exception;
import nxt.crypto.Crypto;
import nxt.db.Db;
import nxt.db.DerivedDbTable;
import nxt.db.FilteringIterator;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

final class BlockchainProcessorImpl implements BlockchainProcessor {
	public static final int BLOCKCACHEMB = Nxt.getIntProperty("burst.blockCacheMB") == 0 ? 40 : Nxt.getIntProperty("blockCacheMB");
	public static boolean oclVerify = Nxt.getBooleanProperty("burst.oclVerify");
	public static final int oclThreshold = Nxt.getIntProperty("burst.oclThreshold") == 0 ? 50 : Nxt.getIntProperty("burst.oclThreshold");
	public static final int oclWaitThreshold = Nxt.getIntProperty("burst.oclWaitThreshold") == 0 ? 2000 : Nxt.getIntProperty("burst.oclWaitThreshold");
	private static final Semaphore gpuUsage = new Semaphore(2);

	private static final BlockchainProcessorImpl instance = new BlockchainProcessorImpl();

	static BlockchainProcessorImpl getInstance() {
		return instance;
	}

	private final BlockchainImpl blockchain = BlockchainImpl.getInstance();

	private final List<DerivedDbTable> derivedTables = new CopyOnWriteArrayList<>();
	private final boolean trimDerivedTables = Nxt.getBooleanProperty("nxt.trimDerivedTables");
	private volatile int lastTrimHeight;

	private final Listeners<Block, Event> blockListeners = new Listeners<>();
	private volatile Peer lastBlockchainFeeder;
	private volatile int lastBlockchainFeederHeight;
	private volatile boolean getMoreBlocks = true;

	// blockCache for faster sync
	public static final Map<Long, Block> blockCache = new HashMap<Long, Block>();
	public static final Map<Long, Long> reverseCache = new HashMap<Long, Long>();
	public static final List<Long> unverified = new LinkedList<Long>();
	private static int blockCacheSize = 0;

	private volatile boolean isScanning;
	private volatile boolean forceScan = Nxt.getBooleanProperty("nxt.forceScan");
	private volatile boolean validateAtScan = Nxt.getBooleanProperty("nxt.forceValidate");

	// Last downloaded block:
	private Long lastDownloaded = 0L;

	private final Runnable debugInfoThread = new Runnable() {
		@Override
		public void run() {
                        Logger.logMessage("Unverified blocks: " + String.valueOf(unverified.size()));
                        Logger.logMessage("Blocks in cache: " + String.valueOf(blockCache.size()));
                        Logger.logMessage("Bytes in cache: " + String.valueOf(blockCacheSize));
		}
	};

	private final Runnable pocVerificationThread = new Runnable() {
		@Override
		public void run() {
			for(;;) {
				if(oclVerify) {
					boolean gpuAcquired = false;
					try {
						List<BlockImpl> blocks = new LinkedList<>();
						synchronized (blockCache) {
							if (unverified.size() == 0) {
								return;
							}

							int verifiedCached = blockCache.size() - unverified.size();
							if(verifiedCached >= oclWaitThreshold && unverified.size() < OCLPoC.getMaxItems() / 2) {
								return;
							}

							if (unverified.size() < oclThreshold) {
								Long blockId = BlockchainProcessorImpl.unverified.get(0);
								BlockchainProcessorImpl.unverified.remove(blockId);

								blocks.add((BlockImpl) BlockchainProcessorImpl.blockCache.get(blockId));
							} else {
								if (!gpuUsage.tryAcquire()) {
								        Logger.logDebugMessage("already max locked");
									return;
								}
								gpuAcquired = true;

								while (unverified.size() > 0 && blocks.size() < OCLPoC.getMaxItems()) {
									Long blockId = BlockchainProcessorImpl.unverified.get(0);
									BlockchainProcessorImpl.unverified.remove(blockId);

									blocks.add((BlockImpl) BlockchainProcessorImpl.blockCache.get(blockId));
								}
							}
						}
						try {
							if(blocks.size() > 1) {
								OCLPoC.validatePoC(blocks);
							}
							else {
								blocks.get(0).preVerify();
							}
						}
						catch (OCLPoC.PreValidateFailException e) {
							e.printStackTrace();
							blacklistClean(e.getBlock(), e);
						}
						catch (BlockNotAcceptedException e) {
							e.printStackTrace();
							blacklistClean(blocks.get(0), e);
						}
					}
					finally {
						if(gpuAcquired) {
							gpuUsage.release();
						}
					}
				}
				else {
					BlockImpl block;
					synchronized (BlockchainProcessorImpl.blockCache) {
						if (BlockchainProcessorImpl.unverified.size() == 0)
							return;

						Long blockId = BlockchainProcessorImpl.unverified.get(0);
						BlockchainProcessorImpl.unverified.remove(blockId);

						block = (BlockImpl) BlockchainProcessorImpl.blockCache.get(blockId);
					}
					try {
						block.preVerify();
					} catch (BlockchainProcessor.BlockNotAcceptedException e) {
						blacklistClean(block, e);
					}
				}
			}
		}
	};

	private final Runnable blockImporterThread = new Runnable() {
		@Override
		public void run() {
			while(true) {
				synchronized (blockchain) {
					for (; ; ) {
						Long lastId = blockchain.getLastBlock().getId();

						Long currentBlockId;
						BlockImpl currentBlock;
						synchronized (BlockchainProcessorImpl.blockCache) {
							if (!BlockchainProcessorImpl.reverseCache.containsKey(lastId))
								break;

							currentBlockId = BlockchainProcessorImpl.reverseCache.get(lastId);
							currentBlock = (BlockImpl) BlockchainProcessorImpl.blockCache.get(currentBlockId);
						}
						try {
							if (!currentBlock.isVerified()) {
								currentBlock.preVerify();
							}
							pushBlock(currentBlock);
						} catch (BlockNotAcceptedException e) {
							Logger.logMessage("Block not accepted", e);
							blacklistClean(currentBlock, e);
							return;
						}
						// Clean up cache
						synchronized (BlockchainProcessorImpl.blockCache) {
							if(blockCache.containsKey(currentBlockId)) { // make sure it wasn't already removed(ex failed preValidate) to avoid double subtracting from blockCacheSize
								BlockchainProcessorImpl.reverseCache.remove(lastId);

								BlockchainProcessorImpl.blockCache.remove(currentBlockId);
								blockCacheSize -= currentBlock.getByteLength();
							}
						
						/*if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
						} else if (!BlockDb.hasBlock(block.getId())) {
							forkBlocks.add(block);
						}*/
						}
					}
				}
				synchronized(blockCache) {
					while(!reverseCache.containsKey(blockchain.getLastBlock().getId())) {
						try {
							blockCache.wait(2000);
						} catch (InterruptedException ignore) {}
					}
				}
			}
		}
	};

    private void blacklistClean(BlockImpl block, Exception e) {
        block.getPeer().blacklist(e);
        long removeId = block.getId();
        synchronized (BlockchainProcessorImpl.blockCache) {
			if(blockCache.containsKey(block.getId())) {
				blockCacheSize -= block.getByteLength();
				reverseCache.remove(block.getPreviousBlockId());
			}
            blockCache.remove(block.getId());
            while(reverseCache.containsKey(removeId)) {
                long id = reverseCache.get(removeId);
                reverseCache.remove(removeId);
                blockCacheSize -= ((BlockImpl)blockCache.get(id)).getByteLength();
                blockCache.remove(id);
                removeId = id;
            }
			Block lastBlock = blockchain.getLastBlock();
			lastDownloaded = lastBlock.getHeight() >= block.getHeight() ? lastBlock.getId() : block.getPreviousBlockId();
			blockCache.notify();
        }
    }

	private final Runnable getMoreBlocksThread = new Runnable() {
		private final JSONStreamAware getCumulativeDifficultyRequest;

		{
			JSONObject request = new JSONObject();
			request.put("requestType", "getCumulativeDifficulty");
			getCumulativeDifficultyRequest = JSON.prepareRequest(request);
		}

		private boolean peerHasMore;

		@Override
		public void run() {
			try {
				try {
					if (!getMoreBlocks) {
						return;
					}

					if(blockCacheSize > BLOCKCACHEMB * 1024 * 1024)
						return;

					peerHasMore = true;
					Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
					if (peer == null) {
						return;
					}
					JSONObject response = peer.send(getCumulativeDifficultyRequest);
					if (response == null) {
						return;
					}
					BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
					String peerCumulativeDifficulty = (String) response.get("cumulativeDifficulty");
					if (peerCumulativeDifficulty == null) {
						return;
					}
					BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
					if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
						return;
					}
					if (response.get("blockchainHeight") != null) {
						lastBlockchainFeeder = peer;
						lastBlockchainFeederHeight = ((Long) response.get("blockchainHeight")).intValue();
					}
					if (betterCumulativeDifficulty.equals(curCumulativeDifficulty)) {
						return;
					}

					long commonBlockId = Genesis.GENESIS_BLOCK_ID;

					if (blockchain.getLastBlock().getId() != Genesis.GENESIS_BLOCK_ID) {
						commonBlockId = getCommonMilestoneBlockId(peer);
					}
					if (commonBlockId == 0 || !peerHasMore) {
						return;
					}

					commonBlockId = getCommonBlockId(peer, commonBlockId);
					if (commonBlockId == 0 || !peerHasMore) {
						return;
					}

					final Block commonBlock = BlockDb.findBlock(commonBlockId);
					if (commonBlock == null || blockchain.getHeight() - commonBlock.getHeight() >= 720) {
						return;
					}

					long currentBlockId = (lastDownloaded == 0 ? commonBlockId : lastDownloaded);
					if(commonBlock.getHeight() < blockchain.getLastBlock().getHeight()) { // fork point
						currentBlockId = commonBlockId;
					}
					else {
						synchronized (blockCache) {
							long checkBlockId = currentBlockId;
							while (checkBlockId != blockchain.getLastBlock().getId()) {
								if (blockCache.get(checkBlockId) == null) {
									currentBlockId = blockchain.getLastBlock().getId();
									break;
								}
								checkBlockId = blockCache.get(checkBlockId).getPreviousBlockId();
							}
						}
					}
					
					List<BlockImpl> forkBlocks = new ArrayList<>();

					boolean processedAll = true;
					int requestCount = 0;
					outer:
						while (forkBlocks.size() < 1440 && requestCount++ < 10 && ((blockCacheSize < BLOCKCACHEMB * 1024 * 1024) || forkBlocks.size() > 0)) { // fork decision could be wrong if cut off so ignore cache size for forks
							//Logger.logMessage("Downloading " + String.valueOf(currentBlockId));
							JSONArray nextBlocks = getNextBlocks(peer, currentBlockId);
							if (nextBlocks == null || nextBlocks.size() == 0) {
								break;
							}
							// Insert Blocks to blockCache
							synchronized(BlockchainProcessorImpl.blockCache) {
								for(Object o : nextBlocks) {
									BlockImpl block;
                                                                        JSONObject blockData = (JSONObject) o;
									
									try {
                                                                                block = BlockImpl.parseBlock(blockData);
										if(block.getPreviousBlockId() != currentBlockId) { // ensure peer isn't cluttering cache with unrequested stuff
											Logger.logMessage("Peer sent unrequested block. Blacklisting...");
											peer.blacklist();
											return;
										}
										currentBlockId = block.getId();

										if(blockCache.containsKey(block.getId()) || BlockDb.hasBlock(block.getId())) {
											lastDownloaded = currentBlockId;
											continue;
										}

										if(reverseCache.containsKey(block.getPreviousBlockId())) {
											long existingId = reverseCache.get(block.getPreviousBlockId());
											if(existingId != block.getId()) {
												Logger.logMessage("Aborting getMoreBlocks. Conflicting fork already in queue.");
												return;
											}
										}

										block.setPeer(peer);
										int blockSize = blockData.toString().length();
										block.setByteLength(blockSize);
		
										Long prevId = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
										// Try to find previous Block:
										if(BlockchainProcessorImpl.blockCache.containsKey(prevId)) {
											block.setHeight(BlockchainProcessorImpl.blockCache.get(prevId).getHeight() + 1);
										} else {
											// First in cache? Get from blockchain:
											BlockImpl prevBlock = (BlockImpl)Nxt.getBlockchain().getBlock(prevId);
											if(prevBlock == null) {
												// We may be on a fork: Add all other Blocks to forkQueue:
												if(forkBlocks.size() > 0) {
													block.setHeight(forkBlocks.get(forkBlocks.size() - 1).getHeight() + 1);
													forkBlocks.add(block);
												} else {
													// Logger.logMessage("Previous Block with ID " + String.valueOf(prevId) + " not found. Blacklisting ...");
													// peer.blacklist();

													// We've already verified that we asked for a block with that prevBlock,
													// so not the peer's fault if we changed what we wanted before we receive the data.
													return;
												}
											}
											Long altBlockId = null;
											if(forkBlocks.size() == 0) {
												try {
													altBlockId = BlockDb.findBlockIdAtHeight(prevBlock.getHeight() + 1);
												} catch (Exception e) {}
												if(altBlockId != null) {
													if(altBlockId.longValue() != block.getId()) {
														// fork
														forkBlocks.add(block);
													}
													else {
														lastDownloaded = currentBlockId;
														continue; // don't clutter cache with stuff we already have
													}
												}
												block.setHeight(prevBlock.getHeight() + 1);
											}
										}
										//Logger.logMessage("Block Height " + String.valueOf(block.getHeight()) + " ID " + String.valueOf(block.getId()));

										if(forkBlocks.size() == 0) { // keep old cache separate from fork. blindly adding to reverseCache could cause it to end up in an uncleanable state
											// Add to Blockcache
											BlockchainProcessorImpl.blockCache.put(block.getId(), block);

											// Add to reverse cache
											BlockchainProcessorImpl.reverseCache.put(prevId, currentBlockId);

											// Mark for threaded poc verification
											BlockchainProcessorImpl.unverified.add(block.getId());

                                            blockCacheSize += blockSize;

											lastDownloaded = currentBlockId;
										}

                                                                        } catch (RuntimeException | NxtException.ValidationException e) {
                                                                                Logger.logMessage("Failed to parse block: " + e.toString(), e);
                                                                                peer.blacklist(e);
                                                                                return;
									} catch (Exception e) {	
									}
								}
								blockCache.notify();
							}
						}
					if(forkBlocks.size() > 0 && blockchain.getHeight() - commonBlock.getHeight() < 720) {
						processFork(forkBlocks.get(0).getPeer(), forkBlocks, commonBlock);
					}
				} catch (NxtException.StopException e) {
					Logger.logMessage("Blockchain download stopped: " + e.getMessage());
				} catch (Exception e) {
					Logger.logMessage("Error in blockchain download thread", e);
				}
			} catch (Throwable t) {
				Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
				t.printStackTrace();
				System.exit(1);
			}

		}

		private long getCommonMilestoneBlockId(Peer peer) {

			String lastMilestoneBlockId = null;

			while (true) {
				JSONObject milestoneBlockIdsRequest = new JSONObject();
				milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
				if (lastMilestoneBlockId == null) {
					milestoneBlockIdsRequest.put("lastBlockId", blockchain.getLastBlock().getStringId());
				} else {
					milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
				}

				JSONObject response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest));
				if (response == null) {
					return 0;
				}
				JSONArray milestoneBlockIds = (JSONArray) response.get("milestoneBlockIds");
				if (milestoneBlockIds == null) {
					return 0;
				}
				if (milestoneBlockIds.isEmpty()) {
					return Genesis.GENESIS_BLOCK_ID;
				}
				// prevent overloading with blockIds
				if (milestoneBlockIds.size() > 20) {
					Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many milestoneBlockIds, blacklisting");
					peer.blacklist();
					return 0;
				}
				if (Boolean.TRUE.equals(response.get("last"))) {
					peerHasMore = false;
				}
				for (Object milestoneBlockId : milestoneBlockIds) {
					long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
					if (BlockDb.hasBlock(blockId)) {
						if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
							peerHasMore = false;
						}
						return blockId;
					}
					lastMilestoneBlockId = (String) milestoneBlockId;
				}
			}

		}

		private long getCommonBlockId(Peer peer, long commonBlockId) {

			while (true) {
				JSONObject request = new JSONObject();
				request.put("requestType", "getNextBlockIds");
				request.put("blockId", Convert.toUnsignedLong(commonBlockId));
				JSONObject response = peer.send(JSON.prepareRequest(request));
				if (response == null) {
					return 0;
				}
				JSONArray nextBlockIds = (JSONArray) response.get("nextBlockIds");
				if (nextBlockIds == null || nextBlockIds.size() == 0) {
					return 0;
				}
				// prevent overloading with blockIds
				if (nextBlockIds.size() > 1440) {
					Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlockIds, blacklisting");
					peer.blacklist();
					return 0;
				}

				for (Object nextBlockId : nextBlockIds) {
					long blockId = Convert.parseUnsignedLong((String) nextBlockId);
					if (! BlockDb.hasBlock(blockId)) {
						return commonBlockId;
					}
					commonBlockId = blockId;
				}
			}

		}

		private JSONArray getNextBlocks(Peer peer, long curBlockId) {

			JSONObject request = new JSONObject();
			request.put("requestType", "getNextBlocks");
			request.put("blockId", Convert.toUnsignedLong(curBlockId));
			JSONObject response = peer.send(JSON.prepareRequest(request));
			if (response == null) {
				return null;
			}

			JSONArray nextBlocks = (JSONArray) response.get("nextBlocks");
			if (nextBlocks == null) {
				return null;
			}
			// prevent overloading with blocks
			if (nextBlocks.size() > 1440) {
				Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlocks, blacklisting");
				peer.blacklist();
				return null;
			}

			return nextBlocks;

		}

		private void processFork(Peer peer, final List<BlockImpl> forkBlocks, final Block commonBlock) {

			synchronized (blockchain) {
				BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();

				List<BlockImpl> myPoppedOffBlocks = popOffTo(commonBlock);

				int pushedForkBlocks = 0;
				if (blockchain.getLastBlock().getId() == commonBlock.getId()) {
					for (BlockImpl block : forkBlocks) {
						if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
							try {
								pushBlock(block);
								pushedForkBlocks += 1;
							} catch (BlockNotAcceptedException e) {
								peer.blacklist(e);
								break;
							}
						}
					}
				}

				if (pushedForkBlocks > 0 && blockchain.getLastBlock().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {
					Logger.logDebugMessage("Pop off caused by peer " + peer.getPeerAddress() + ", blacklisting");
					peer.blacklist();
					List<BlockImpl> peerPoppedOffBlocks = popOffTo(commonBlock);
					pushedForkBlocks = 0;
					for (BlockImpl block : peerPoppedOffBlocks) {
						TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
					}
				}

				if (pushedForkBlocks == 0) {
					for (int i = myPoppedOffBlocks.size() - 1; i >= 0; i--) {
						BlockImpl block = myPoppedOffBlocks.remove(i);
						try {
							pushBlock(block);
						} catch (BlockNotAcceptedException e) {
							Logger.logErrorMessage("Popped off block no longer acceptable: " + block.getJSONObject().toJSONString(), e);
							break;
						}
					}
				} else {
					for (BlockImpl block : myPoppedOffBlocks) {
						TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
					}
				}
			} // synchronized

			synchronized (blockCache) { // cache may no longer correspond with current chain, so dump it
				blockCache.clear();
				reverseCache.clear();
				unverified.clear();
				lastDownloaded = blockchain.getLastBlock().getId();
				blockCacheSize = 0;
				blockCache.notify();
			}
		}

	};

	private BlockchainProcessorImpl() {

		blockListeners.addListener(new Listener<Block>() {
			@Override
			public void notify(Block block) {
				if (block.getHeight() % 5000 == 0) {
					Logger.logMessage("processed block " + block.getHeight());
				}
			}
		}, Event.BLOCK_SCANNED);

		blockListeners.addListener(new Listener<Block>() {
			@Override
			public void notify(Block block) {
				if (block.getHeight() % 5000 == 0) {
					Logger.logMessage("processed block " + block.getHeight());
					Db.analyzeTables();
				}
			}
		}, Event.BLOCK_PUSHED);

		if (trimDerivedTables) {
			blockListeners.addListener(new Listener<Block>() {
				@Override
				public void notify(Block block) {
					if (block.getHeight() % 1440 == 0) {
						lastTrimHeight = Math.max(block.getHeight() - Constants.MAX_ROLLBACK, 0);
						if (lastTrimHeight > 0) {
							for (DerivedDbTable table : derivedTables) {
								table.trim(lastTrimHeight);
							}
						}
					}
				}
			}, Event.AFTER_BLOCK_APPLY);
		}

		blockListeners.addListener(new Listener<Block>() {
			@Override
			public void notify(Block block) {
				Db.analyzeTables();
			}
		}, Event.RESCAN_END);

		ThreadPool.runBeforeStart(new Runnable() {
			@Override
			public void run() {
				addGenesisBlock();
				if (forceScan) {
					scan(0);
				}
			}
		}, false);

		ThreadPool.scheduleThread("GetMoreBlocks", getMoreBlocksThread, 2);
		ThreadPool.scheduleThread("ImportBlocks", blockImporterThread, 10);
		ThreadPool.scheduleThreadCores("VerifyPoc", pocVerificationThread, 9);
		//ThreadPool.scheduleThread("Info", debugInfoThread, 5);
	}

	@Override
	public boolean addListener(Listener<Block> listener, BlockchainProcessor.Event eventType) {
		return blockListeners.addListener(listener, eventType);
	}

	@Override
	public boolean removeListener(Listener<Block> listener, Event eventType) {
		return blockListeners.removeListener(listener, eventType);
	}

	@Override
	public void registerDerivedTable(DerivedDbTable table) {
		derivedTables.add(table);
	}

	@Override
	public Peer getLastBlockchainFeeder() {
		return lastBlockchainFeeder;
	}

	@Override
	public int getLastBlockchainFeederHeight() {
		return lastBlockchainFeederHeight;
	}

	@Override
	public boolean isScanning() {
		return isScanning;
	}

	@Override
	public int getMinRollbackHeight() {
		return trimDerivedTables ? (lastTrimHeight > 0 ? lastTrimHeight : Math.max(blockchain.getHeight() - Constants.MAX_ROLLBACK, 0)) : 0;
	}

	@Override
	public void processPeerBlock(JSONObject request) throws NxtException {
		BlockImpl block = BlockImpl.parseBlock(request);
		synchronized (blockchain) {
			synchronized (blockCache) {
				Block prevBlock = blockchain.getLastBlock();
				if(blockCache.containsKey(block.getPreviousBlockId()) && !(reverseCache.containsKey(block.getPreviousBlockId()))) {
					prevBlock = blockCache.get(block.getPreviousBlockId());
				}
				if (block.getPreviousBlockId() == prevBlock.getId()) {
					if(reverseCache.containsKey(prevBlock.getId())) {
						Long existingId = reverseCache.get(prevBlock.getId());
						if(existingId != block.getId()) {
							Logger.logMessage("Ignoring peer broadcast block with ID " + Convert.toUnsignedLong(block.getId())
								+ ". Conflicting block " + Convert.toUnsignedLong(existingId) + " already exists in queue");
						}
						else {
							Logger.logDebugMessage("Ignoring peer broadcast block with ID " + Convert.toUnsignedLong(block.getId()) + ". Already exists in queue");
						}
					}
					else {
						block.setHeight(prevBlock.getHeight() + 1);
						blockCache.put(block.getId(), block);
						reverseCache.put(block.getPreviousBlockId(), block.getId());
						int blockSize = block.toString().length();
						block.setByteLength(blockSize);
						blockCacheSize += block.getByteLength();
						// do not add to unverified as it will be processed immediately
						lastDownloaded = block.getId();
						blockCache.notify();
					}
				}
				else {
					Logger.logDebugMessage("Ignoring peer broadcast block with ID " + Convert.toUnsignedLong(block.getId())
							+ ". Previous block " + Convert.toUnsignedLong(block.getPreviousBlockId())
							+ " does not match actual previous block ID " + Convert.toUnsignedLong(prevBlock.getId()));
				}
			}
		}
	}

	@Override
	public List<BlockImpl> popOffTo(int height) {
		return popOffTo(blockchain.getBlockAtHeight(height));
	}

	@Override
	public void fullReset() {
		synchronized (blockchain) {
			//BlockDb.deleteBlock(Genesis.GENESIS_BLOCK_ID); // fails with stack overflow in H2
			BlockDb.deleteAll();
			addGenesisBlock();
			scan(0);
		}
	}

	@Override
	public void forceScanAtStart() {
		forceScan = true;
	}

	@Override
	public void validateAtNextScan() {
		validateAtScan = true;
	}

	void setGetMoreBlocks(boolean getMoreBlocks) {
		this.getMoreBlocks = getMoreBlocks;
	}

	private void addBlock(BlockImpl block) {
		try (Connection con = Db.getConnection()) {
			BlockDb.saveBlock(con, block);
			blockchain.setLastBlock(block);
		} catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private void addGenesisBlock() {
		if (BlockDb.hasBlock(Genesis.GENESIS_BLOCK_ID)) {
			Logger.logMessage("Genesis block already in database");
			BlockImpl lastBlock = BlockDb.findLastBlock();
			blockchain.setLastBlock(lastBlock);
			Logger.logMessage("Last block height: " + lastBlock.getHeight());
			return;
		}
		Logger.logMessage("Genesis block not in database, starting from scratch");
		try {
			List<TransactionImpl> transactions = new ArrayList<>();
			MessageDigest digest = Crypto.sha256();
			for (Transaction transaction : transactions) {
				digest.update(transaction.getBytes());
			}
			ByteBuffer bf = ByteBuffer.allocate( 0 );
			bf.order( ByteOrder.LITTLE_ENDIAN ); 
			byte[] byteATs = bf.array();
			BlockImpl genesisBlock = new BlockImpl(-1, 0, 0, 0, 0, transactions.size() * 128, digest.digest(),
					Genesis.CREATOR_PUBLIC_KEY, new byte[32], Genesis.GENESIS_BLOCK_SIGNATURE, null, transactions, 0, byteATs);
			genesisBlock.setPrevious(null);
			addBlock(genesisBlock);
		} catch (NxtException.ValidationException e) {
			Logger.logMessage(e.getMessage());
			throw new RuntimeException(e.toString(), e);
		}
	}

	private void pushBlock(final BlockImpl block) throws BlockNotAcceptedException {

		int curTime = Nxt.getEpochTime();

		synchronized (blockchain) {
			TransactionProcessorImpl transactionProcessor = TransactionProcessorImpl.getInstance();
			BlockImpl previousLastBlock = null;
			try {
				Db.beginTransaction();
				previousLastBlock = blockchain.getLastBlock();

				if (previousLastBlock.getId() != block.getPreviousBlockId()) {
					throw new BlockOutOfOrderException("Previous block id doesn't match");
				}

				if (block.getVersion() != getBlockVersion(previousLastBlock.getHeight())) {
					throw new BlockNotAcceptedException("Invalid version " + block.getVersion());
				}

				if (block.getVersion() != 1 && !Arrays.equals(Crypto.sha256().digest(previousLastBlock.getBytes()), block.getPreviousBlockHash())) {
					throw new BlockNotAcceptedException("Previous block hash doesn't match");
				}
				if (block.getTimestamp() > curTime + 15 || block.getTimestamp() <= previousLastBlock.getTimestamp()) {
					throw new BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
							+ " current time is " + curTime + ", previous block timestamp is " + previousLastBlock.getTimestamp());
				}
				if (block.getId() == 0L || BlockDb.hasBlock(block.getId())) {
					throw new BlockNotAcceptedException("Duplicate block or invalid id");
				}
				if (!block.verifyGenerationSignature()) {
					throw new BlockNotAcceptedException("Generation signature verification failed");
				}
				if (!block.verifyBlockSignature()) {
					throw new BlockNotAcceptedException("Block signature verification failed");
				}

				Map<TransactionType, Set<String>> duplicates = new HashMap<>();
				long calculatedTotalAmount = 0;
				long calculatedTotalFee = 0;
				MessageDigest digest = Crypto.sha256();
				
				for (TransactionImpl transaction : block.getTransactions()) {

					if (transaction.getTimestamp() > curTime + 15) {
						throw new BlockOutOfOrderException("Invalid transaction timestamp: " + transaction.getTimestamp()
								+ ", current time is " + curTime);
					}
					if (transaction.getTimestamp() > block.getTimestamp() + 15
							|| transaction.getExpiration() < block.getTimestamp()) {
						throw new TransactionNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
								+ " for transaction " + transaction.getStringId() + ", current time is " + curTime
								+ ", block timestamp is " + block.getTimestamp(), transaction);
					}
					if (TransactionDb.hasTransaction(transaction.getId())) {
						throw new TransactionNotAcceptedException("Transaction " + transaction.getStringId()
								+ " is already in the blockchain", transaction);
					}
					if (transaction.getReferencedTransactionFullHash() != null) {
						if ((previousLastBlock.getHeight() < Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
								&& !TransactionDb.hasTransaction(Convert.fullHashToId(transaction.getReferencedTransactionFullHash())))
								|| (previousLastBlock.getHeight() >= Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
								&& !hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0))) {
							throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
									+ transaction.getReferencedTransactionFullHash()
									+ " for transaction " + transaction.getStringId(), transaction);
						}
					}
					if (transaction.getVersion() != transactionProcessor.getTransactionVersion(previousLastBlock.getHeight())) {
						throw new TransactionNotAcceptedException("Invalid transaction version " + transaction.getVersion()
								+ " at height " + previousLastBlock.getHeight(), transaction);
					}
					/*if (!transaction.verifySignature()) { // moved to preVerify
						throw new TransactionNotAcceptedException("Signature verification failed for transaction "
								+ transaction.getStringId() + " at height " + previousLastBlock.getHeight(), transaction);
					}*/
                    if(!transaction.verifyPublicKey()) {
                        throw new TransactionNotAcceptedException("Wrong public key in transaction " + transaction.getStringId() + " at height " + previousLastBlock.getHeight(), transaction);
                    }
					if (Nxt.getBlockchain().getHeight() >= Constants.AUTOMATED_TRANSACTION_BLOCK) {
	                    if (!EconomicClustering.verifyFork(transaction)) {
	                        Logger.logDebugMessage("Block " + block.getStringId() + " height " + (previousLastBlock.getHeight() + 1)
	                                + " contains transaction that was generated on a fork: "
	                                + transaction.getStringId() + " ecBlockHeight " + transaction.getECBlockHeight() + " ecBlockId "
	                                + Convert.toUnsignedLong(transaction.getECBlockId()));
	                        throw new TransactionNotAcceptedException("Transaction belongs to a different fork", transaction);
	                    }
                    }
					if (transaction.getId() == 0L) {
						throw new TransactionNotAcceptedException("Invalid transaction id", transaction);
					}
					if (transaction.isDuplicate(duplicates)) {
						throw new TransactionNotAcceptedException("Transaction is a duplicate: "
								+ transaction.getStringId(), transaction);
					}
					try {
						transaction.validate();
					} catch (NxtException.ValidationException e) {
						throw new TransactionNotAcceptedException(e.getMessage(), transaction);
					}

					calculatedTotalAmount += transaction.getAmountNQT();

					calculatedTotalFee += transaction.getFeeNQT();

					digest.update(transaction.getBytes());

				}

				if (calculatedTotalAmount > block.getTotalAmountNQT() || calculatedTotalFee > block.getTotalFeeNQT()) {
					throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals");
				}
				if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
					throw new BlockNotAcceptedException("Payload hash doesn't match");
				}

				long remainingAmount = Convert.safeSubtract(block.getTotalAmountNQT(), calculatedTotalAmount);
				long remainingFee = Convert.safeSubtract(block.getTotalFeeNQT(), calculatedTotalFee);

				block.setPrevious(previousLastBlock);
				blockListeners.notify(block, Event.BEFORE_BLOCK_ACCEPT);
				transactionProcessor.requeueAllUnconfirmedTransactions();
				Account.flushAccountTable();
				addBlock(block);
				accept(block, remainingAmount, remainingFee);

				for (DerivedDbTable table : derivedTables) {
					table.finish();
				}

				Db.commitTransaction();
			} catch (Exception e) {
				Db.rollbackTransaction();
				blockchain.setLastBlock(previousLastBlock);
				throw e;
			} finally {
				Db.endTransaction();
			}
		} // synchronized

		blockListeners.notify(block, Event.BLOCK_PUSHED);

		if (block.getTimestamp() >= Nxt.getEpochTime() - 15) {
			Peers.sendToSomePeers(block);
		}

	}

	private void accept(BlockImpl block, Long remainingAmount, Long remainingFee) throws TransactionNotAcceptedException, BlockNotAcceptedException {
		Subscription.clearRemovals();
		TransactionProcessorImpl transactionProcessor = TransactionProcessorImpl.getInstance();
		for (TransactionImpl transaction : block.getTransactions()) {
			if (! transaction.applyUnconfirmed()) {
				throw new TransactionNotAcceptedException("Double spending transaction: " + transaction.getStringId(), transaction);
			}
		}
		long calculatedRemainingAmount = 0;
		long calculatedRemainingFee = 0;
		//ATs
		AT_Block atBlock;
		AT.clearPendingFees();
		AT.clearPendingTransactions();
		try {
			atBlock = AT_Controller.validateATs( block.getBlockATs() , Nxt.getBlockchain().getHeight());
		} catch (NoSuchAlgorithmException e) {
			//should never reach that point
			throw new BlockNotAcceptedException( "md5 does not exist" );
		} catch (AT_Exception e) {
			throw new BlockNotAcceptedException("ats are not matching at block height " + Nxt.getBlockchain().getHeight() );
		}
		calculatedRemainingAmount += atBlock.getTotalAmount();
		calculatedRemainingFee += atBlock.getTotalFees();
		//ATs
		if(Subscription.isEnabled()) {
			calculatedRemainingFee += Subscription.applyUnconfirmed(block.getTimestamp());
		}
		if(remainingAmount != null && remainingAmount.longValue() != calculatedRemainingAmount) {
			throw new BlockNotAcceptedException("Calculated remaining amount doesn't add up");
		}
		if(remainingFee != null && remainingFee.longValue() != calculatedRemainingFee) {
			throw new BlockNotAcceptedException("Calculated remaining fee doesn't add up");
		}
		blockListeners.notify(block, Event.BEFORE_BLOCK_APPLY);
		block.apply();
		Subscription.applyConfirmed(block);
		if(Escrow.isEnabled()) {
			Escrow.updateOnBlock(block);
		}
		blockListeners.notify(block, Event.AFTER_BLOCK_APPLY);
		if (block.getTransactions().size() > 0) {
			transactionProcessor.notifyListeners(block.getTransactions(), TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
		}
	}

	private List<BlockImpl> popOffTo(Block commonBlock) {
		synchronized (blockchain) {
			if (commonBlock.getHeight() < getMinRollbackHeight()) {
				throw new IllegalArgumentException("Rollback to height " + commonBlock.getHeight() + " not suppported, "
						+ "current height " + Nxt.getBlockchain().getHeight());
			}
			if (! blockchain.hasBlock(commonBlock.getId())) {
				Logger.logDebugMessage("Block " + commonBlock.getStringId() + " not found in blockchain, nothing to pop off");
				return Collections.emptyList();
			}
			List<BlockImpl> poppedOffBlocks = new ArrayList<>();
			try {
				Db.beginTransaction();
				BlockImpl block = blockchain.getLastBlock();
				Logger.logDebugMessage("Rollback from " + block.getHeight() + " to " + commonBlock.getHeight());
				while (block.getId() != commonBlock.getId() && block.getId() != Genesis.GENESIS_BLOCK_ID) {
					poppedOffBlocks.add(block);
					block = popLastBlock();
				}
				for (DerivedDbTable table : derivedTables) {
					table.rollback(commonBlock.getHeight());
				}
				Db.commitTransaction();
			} catch (RuntimeException e) {
				Db.rollbackTransaction();
				Logger.logDebugMessage("Error popping off to " + commonBlock.getHeight(), e);
				throw e;
			} finally {
				Db.endTransaction();
			}
			return poppedOffBlocks;
		} // synchronized
	}

	private BlockImpl popLastBlock() {
		BlockImpl block = blockchain.getLastBlock();
		if (block.getId() == Genesis.GENESIS_BLOCK_ID) {
			throw new RuntimeException("Cannot pop off genesis block");
		}
		BlockImpl previousBlock = BlockDb.findBlock(block.getPreviousBlockId());
		blockchain.setLastBlock(block, previousBlock);
		for (TransactionImpl transaction : block.getTransactions()) {
			transaction.unsetBlock();
		}
		BlockDb.deleteBlocksFrom(block.getId());
		blockListeners.notify(block, Event.BLOCK_POPPED);
		return previousBlock;
	}

	int getBlockVersion(int previousBlockHeight) {
		return previousBlockHeight < Constants.TRANSPARENT_FORGING_BLOCK ? 1
				: previousBlockHeight < Constants.NQT_BLOCK ? 2
						: 3;
	}

	void generateBlock(String secretPhrase, byte[] publicKey, Long nonce) throws BlockNotAcceptedException {

		TransactionProcessorImpl transactionProcessor = TransactionProcessorImpl.getInstance();
		List<TransactionImpl> orderedUnconfirmedTransactions = new ArrayList<>();
		try (FilteringIterator<TransactionImpl> transactions = new FilteringIterator<>(transactionProcessor.getAllUnconfirmedTransactions(),
				new FilteringIterator.Filter<TransactionImpl>() {
			@Override
			public boolean ok(TransactionImpl transaction) {
				return hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0);
			}
		})) {
			for (TransactionImpl transaction : transactions) {
				orderedUnconfirmedTransactions.add(transaction);
			}
		}

		BlockImpl previousBlock = blockchain.getLastBlock();

		SortedSet<TransactionImpl> blockTransactions = new TreeSet<>();

		Map<TransactionType, Set<String>> duplicates = new HashMap<>();

		long totalAmountNQT = 0;
		long totalFeeNQT = 0;
		int payloadLength = 0;

		int blockTimestamp = Nxt.getEpochTime();

		while (payloadLength <= Constants.MAX_PAYLOAD_LENGTH && blockTransactions.size() <= Constants.MAX_NUMBER_OF_TRANSACTIONS) {

			int prevNumberOfNewTransactions = blockTransactions.size();

			for (TransactionImpl transaction : orderedUnconfirmedTransactions) {

				if(blockTransactions.size() >= Constants.MAX_NUMBER_OF_TRANSACTIONS) {
					break;
				}

				int transactionLength = transaction.getSize();
				if (blockTransactions.contains(transaction) || payloadLength + transactionLength > Constants.MAX_PAYLOAD_LENGTH) {
					continue;
				}

				if (transaction.getVersion() != transactionProcessor.getTransactionVersion(previousBlock.getHeight())) {
					continue;
				}

				if (transaction.getTimestamp() > blockTimestamp + 15 || transaction.getExpiration() < blockTimestamp) {
					continue;
				}
				
				if (Nxt.getBlockchain().getHeight() >= Constants.AUTOMATED_TRANSACTION_BLOCK) {
                	if (!EconomicClustering.verifyFork(transaction)) {
                        Logger.logDebugMessage("Including transaction that was generated on a fork: " + transaction.getStringId()
                                + " ecBlockHeight " + transaction.getECBlockHeight() + " ecBlockId " + Convert.toUnsignedLong(transaction.getECBlockId()));
                        continue;
                    }
                }

				if (transaction.isDuplicate(duplicates)) {
					continue;
				}

				try {
					transaction.validate();
				} catch (NxtException.NotCurrentlyValidException e) {
					continue;
				} catch (NxtException.ValidationException e) {
					transactionProcessor.removeUnconfirmedTransaction(transaction);
					continue;
				}

				 blockTransactions.add(transaction);
				 payloadLength += transactionLength;
				 totalAmountNQT += transaction.getAmountNQT();
				 totalFeeNQT += transaction.getFeeNQT();

			}

			if (blockTransactions.size() == prevNumberOfNewTransactions) {
				break;
			}
		}

		if(Subscription.isEnabled()) {
			synchronized(blockchain) {
				Subscription.clearRemovals();
				try {
					Db.beginTransaction();
					transactionProcessor.requeueAllUnconfirmedTransactions();
					//transactionProcessor.processTransactions(newTransactions, false);
					for(TransactionImpl transaction : blockTransactions) {
						transaction.applyUnconfirmed();
					}
					totalFeeNQT += Subscription.calculateFees(blockTimestamp);
				}
				finally {
					Db.rollbackTransaction();
					Db.endTransaction();
				}
			}
		}

		//final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
		
		//ATs for block
		AT.clearPendingFees();
		AT.clearPendingTransactions();
		AT_Block atBlock = AT_Controller.getCurrentBlockATs( Constants.MAX_PAYLOAD_LENGTH - payloadLength , previousBlock.getHeight() + 1 );
		byte[] byteATs = atBlock.getBytesForBlock();

		//digesting AT Bytes
		if (byteATs!=null)
		{
			payloadLength += byteATs.length;
			totalFeeNQT += atBlock.getTotalFees();
			totalAmountNQT += atBlock.getTotalAmount();

		}

		//ATs for block

		MessageDigest digest = Crypto.sha256();
		
		for (Transaction transaction : blockTransactions) {
			digest.update(transaction.getBytes());
		}

		byte[] payloadHash = digest.digest();

		byte[] generationSignature = Nxt.getGenerator().calculateGenerationSignature(previousBlock.getGenerationSignature(), previousBlock.getGeneratorId());

		BlockImpl block;
		byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());

		try {

			block = new BlockImpl(getBlockVersion(previousBlock.getHeight()), blockTimestamp, previousBlock.getId(), totalAmountNQT, totalFeeNQT, payloadLength,
					payloadHash, publicKey, generationSignature, null, previousBlockHash, new ArrayList<>(blockTransactions), nonce, byteATs);

		} catch (NxtException.ValidationException e) {
			// shouldn't happen because all transactions are already validated
			Logger.logMessage("Error generating block", e);
			return;
		}

		block.sign(secretPhrase);

		block.setPrevious(previousBlock);

		try {
			pushBlock(block);
			blockListeners.notify(block, Event.BLOCK_GENERATED);
			Logger.logDebugMessage("Account " + Convert.toUnsignedLong(block.getGeneratorId()) + " generated block " + block.getStringId()
					+ " at height " + block.getHeight());

			synchronized (blockCache) {
				blockCache.clear();
				reverseCache.clear();
				unverified.clear();
				lastDownloaded = blockchain.getLastBlock().getId();
				blockCacheSize = 0;
			}
		} catch (TransactionNotAcceptedException e) {
			Logger.logDebugMessage("Generate block failed: " + e.getMessage());
			Transaction transaction = e.getTransaction();
			Logger.logDebugMessage("Removing invalid transaction: " + transaction.getStringId());
			transactionProcessor.removeUnconfirmedTransaction((TransactionImpl) transaction);
			throw e;
		} catch (BlockNotAcceptedException e) {
			Logger.logDebugMessage("Generate block failed: " + e.getMessage());
			throw e;
		}
	}

	private boolean hasAllReferencedTransactions(Transaction transaction, int timestamp, int count) {
		if (transaction.getReferencedTransactionFullHash() == null) {
			return timestamp - transaction.getTimestamp() < 60 * 1440 * 60 && count < 10;
		}
		transaction = TransactionDb.findTransactionByFullHash(transaction.getReferencedTransactionFullHash());
		if(!Subscription.isEnabled()) {
			if(transaction != null && transaction.getSignature() == null) {
				transaction = null;
			}
		}
		return transaction != null && hasAllReferencedTransactions(transaction, timestamp, count + 1);
	}

	@Override
	public void scan(int height) {
		synchronized (blockchain) {
			TransactionProcessorImpl transactionProcessor = TransactionProcessorImpl.getInstance();
			int blockchainHeight = Nxt.getBlockchain().getHeight();
			if (height > blockchainHeight + 1) {
				throw new IllegalArgumentException("Rollback height " + (height - 1) + " exceeds current blockchain height of " + blockchainHeight);
			}
			if (height > 0 && height < getMinRollbackHeight()) {
				Logger.logMessage("Rollback of more than " + Constants.MAX_ROLLBACK + " blocks not supported, will do a full scan");
				height = 0;
			}
			if (height < 0) {
				height = 0;
			}
			isScanning = true;
			Logger.logMessage("Scanning blockchain starting from height " + height + "...");
			if (validateAtScan) {
				Logger.logDebugMessage("Also verifying signatures and validating transactions...");
			}
			try (Connection con = Db.beginTransaction();
					PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height >= ? ORDER BY db_id ASC")) {
				transactionProcessor.requeueAllUnconfirmedTransactions();
				Account.flushAccountTable();
				for (DerivedDbTable table : derivedTables) {
					if (height == 0) {
						table.truncate();
					} else {
						table.rollback(height - 1);
					}
				}
				pstmt.setInt(1, height);
				try (ResultSet rs = pstmt.executeQuery()) {
					BlockImpl currentBlock = BlockDb.findBlockAtHeight(height);
					blockListeners.notify(currentBlock, Event.RESCAN_BEGIN);
					long currentBlockId = currentBlock.getId();
					if (height == 0) {
						blockchain.setLastBlock(currentBlock); // special case to avoid no last block
						//Account.addOrGetAccount(Genesis.CREATOR_ID).apply(Genesis.CREATOR_PUBLIC_KEY, 0);
					} else {
						blockchain.setLastBlock(BlockDb.findBlockAtHeight(height - 1));
					}
					while (rs.next()) {
						try {
							currentBlock = BlockDb.loadBlock(con, rs);
							if (currentBlock.getId() != currentBlockId) {
								if(currentBlockId == Genesis.GENESIS_BLOCK_ID) {
									Logger.logDebugMessage("Wrong genesis block id set. Should be: " + Convert.toUnsignedLong(currentBlock.getId()));
								}
								throw new NxtException.NotValidException("Database blocks in the wrong order!");
							}
							if (validateAtScan && currentBlockId != Genesis.GENESIS_BLOCK_ID) {
								if (!currentBlock.verifyBlockSignature()) {
									throw new NxtException.NotValidException("Invalid block signature");
								}
								if (!currentBlock.verifyGenerationSignature()) {
									throw new NxtException.NotValidException("Invalid block generation signature");
								}
								if (currentBlock.getVersion() != getBlockVersion(blockchain.getHeight())) {
									throw new NxtException.NotValidException("Invalid block version");
								}
								byte[] blockBytes = currentBlock.getBytes();
								JSONObject blockJSON = (JSONObject) JSONValue.parse(currentBlock.getJSONObject().toJSONString());
								if (!Arrays.equals(blockBytes, BlockImpl.parseBlock(blockJSON).getBytes())) {
									throw new NxtException.NotValidException("Block JSON cannot be parsed back to the same block");
								}
								for (TransactionImpl transaction : currentBlock.getTransactions()) {
									/*if (!transaction.verifySignature()) { // moved to preVerify
										throw new NxtException.NotValidException("Invalid transaction signature");
									}*/
                                    if(!transaction.verifyPublicKey()) {
                                        throw new NxtException.NotValidException("Wrong transaction public key");
                                    }
									if (transaction.getVersion() != transactionProcessor.getTransactionVersion(blockchain.getHeight())) {
										throw new NxtException.NotValidException("Invalid transaction version");
									}
									/*
                                    if (!EconomicClustering.verifyFork(transaction)) {
                                        Logger.logDebugMessage("Found transaction that was generated on a fork: " + transaction.getStringId()
                                                + " in block " + currentBlock.getStringId() + " at height " + currentBlock.getHeight()
                                                + " ecBlockHeight " + transaction.getECBlockHeight() + " ecBlockId " + Convert.toUnsignedLong(transaction.getECBlockId()));
                                        //throw new NxtException.NotValidException("Invalid transaction fork");
                                    }
									 */
									transaction.validate();
									byte[] transactionBytes = transaction.getBytes();
									if (currentBlock.getHeight() > Constants.NQT_BLOCK
											&& !Arrays.equals(transactionBytes, transactionProcessor.parseTransaction(transactionBytes).getBytes())) {
										throw new NxtException.NotValidException("Transaction bytes cannot be parsed back to the same transaction");
									}
									JSONObject transactionJSON = (JSONObject) JSONValue.parse(transaction.getJSONObject().toJSONString());
									if (!Arrays.equals(transactionBytes, transactionProcessor.parseTransaction(transactionJSON).getBytes())) {
										throw new NxtException.NotValidException("Transaction JSON cannot be parsed back to the same transaction");
									}
								}
							}
							blockListeners.notify(currentBlock, Event.BEFORE_BLOCK_ACCEPT);
							blockchain.setLastBlock(currentBlock);
							accept(currentBlock, null, null);
							currentBlockId = currentBlock.getNextBlockId();
							Db.commitTransaction();
						} catch (NxtException | RuntimeException e) {
							Db.rollbackTransaction();
							Logger.logDebugMessage(e.toString(), e);
							Logger.logDebugMessage("Applying block " + Convert.toUnsignedLong(currentBlockId) + " at height "
									+ (currentBlock == null ? 0 : currentBlock.getHeight()) + " failed, deleting from database");
							if (currentBlock != null) {
								transactionProcessor.processLater(currentBlock.getTransactions());
							}
							while (rs.next()) {
								try {
									currentBlock = BlockDb.loadBlock(con, rs);
									transactionProcessor.processLater(currentBlock.getTransactions());
								} catch (NxtException.ValidationException ignore) {
								}
							}
							BlockDb.deleteBlocksFrom(currentBlockId);
							blockchain.setLastBlock(BlockDb.findLastBlock());
						}
						blockListeners.notify(currentBlock, Event.BLOCK_SCANNED);
					}
					Db.endTransaction();
					blockListeners.notify(currentBlock, Event.RESCAN_END);
				}
			} catch (SQLException e) {
				throw new RuntimeException(e.toString(), e);
			}
			validateAtScan = false;
			Logger.logMessage("...done at height " + Nxt.getBlockchain().getHeight());
			isScanning = false;
		} // synchronized
	}

}
