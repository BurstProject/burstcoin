package brs.feesuggestions;

import static brs.Constants.FEE_QUANT;

import brs.Block;
import brs.BlockchainProcessor;
import brs.BlockchainProcessor.Event;
import brs.db.store.BlockchainStore;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

public class FeeSuggestionCalculator {

  private final CircularFifoBuffer latestBlocks;

  private final BlockchainStore blockchainStore;

  private FeeSuggestion feeSuggestion;

  public FeeSuggestionCalculator(BlockchainProcessor blockchainProcessor, BlockchainStore blockchainStore, int historyLength) {
    this.latestBlocks = new CircularFifoBuffer(historyLength);

    this.blockchainStore = blockchainStore;

    blockchainProcessor.addListener(block -> newBlockApplied(block), Event.AFTER_BLOCK_APPLY);
  }

  public FeeSuggestion giveFeeSuggestion() {
    if (latestBlocks.isEmpty()) {
      fillInitialHistory();
      recalculateSuggestion();
    }

    return feeSuggestion;
  }

  private void newBlockApplied(Block block) {
    if (latestBlocks.isEmpty()) {
      fillInitialHistory();
    }

    this.latestBlocks.add(block);
    recalculateSuggestion();
  }

  private void fillInitialHistory() {
    blockchainStore.getLatestBlocks(latestBlocks.maxSize()).forEachRemaining(latestBlocks::add);
  }

  private void recalculateSuggestion() {
    int lowestAmountTransactionsNearHistory = latestBlocks.stream().mapToInt(b -> ((Block) b).getTransactions().size()).min().orElse(1);
    int medianAmountTransactionsNearHistory = (int) latestBlocks.stream().mapToInt(b -> ((Block) b).getTransactions().size()).sorted().skip((latestBlocks.size()-1)/2).limit(2-latestBlocks.size()%2).average().getAsDouble();
    int highestAmountTransactionsNearHistory = latestBlocks.stream().mapToInt(b -> ((Block) b).getTransactions().size()).max().orElse(1);

    long cheapFee = (1 + lowestAmountTransactionsNearHistory) * FEE_QUANT;
    long standardFee = (1 + medianAmountTransactionsNearHistory) * FEE_QUANT;
    long priorityFee = (1 + highestAmountTransactionsNearHistory) * FEE_QUANT;

    feeSuggestion = new FeeSuggestion(cheapFee, standardFee, priorityFee);
  }
}
