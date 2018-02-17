package brs.services.impl;

import brs.Alias;
import brs.Block;
import brs.Blockchain;
import brs.Constants;
import brs.Escrow;
import brs.Escrow.Decision;
import brs.db.BurstIterator;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedEntityTable;
import brs.db.store.EscrowStore;
import brs.services.AliasService;
import brs.services.EscrowService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EscrowServiceImpl implements EscrowService {

  private final VersionedEntityTable<Escrow> escrowTable;
  private final LongKeyFactory<Escrow> escrowDbKeyFactory;
  private final VersionedEntityTable<Decision> decisionTable;
  private final EscrowStore escrowStore;
  private final Blockchain blockchain;
  private final AliasService aliasService;

  public EscrowServiceImpl(EscrowStore escrowStore, Blockchain blockchain, AliasService aliasService) {
    this.escrowStore = escrowStore;
    this.escrowTable = escrowStore.getEscrowTable();
    this.escrowDbKeyFactory = escrowStore.getEscrowDbKeyFactory();
    this.decisionTable = escrowStore.getDecisionTable();
    this.blockchain = blockchain;
    this.aliasService = aliasService;
  }

  @Override
  public BurstIterator<Escrow> getAllEscrowTransactions() {
    return escrowTable.getAll(0, -1);
  }

  @Override
  public Escrow getEscrowTransaction(Long id) {
    return escrowTable.get(escrowDbKeyFactory.newKey(id));
  }

  @Override
  public Collection<Escrow> getEscrowTransactionsByParticipant(Long accountId) {
    return escrowStore.getEscrowTransactionsByParticipent(accountId);
  }

  @Override
  public boolean isEnabled() {
    if(blockchain.getLastBlock().getHeight() >= Constants.BURST_ESCROW_START_BLOCK) {
      return true;
    }

    Alias escrowEnabled = aliasService.getAlias("featureescrow");
    return escrowEnabled != null && escrowEnabled.getAliasURI().equals("enabled");
  }

  @Override
  public void removeEscrowTransaction(Long id) {
    Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(id));
    if(escrow == null) {
      return;
    }
    BurstIterator<Decision> decisionIt = escrow.getDecisions();

    List<Decision> decisions = new ArrayList<>();
    while(decisionIt.hasNext()) {
      Decision decision = decisionIt.next();
      decisions.add(decision);
    }

    decisions.forEach(decision -> decisionTable.delete(decision));
    escrowTable.delete(escrow);
  }

  @Override
  public void updateOnBlock(Block block, int blockchainHeight) {
    escrowStore.updateOnBlock(block, blockchainHeight);
  }

}
