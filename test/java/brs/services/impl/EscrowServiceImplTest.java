package brs.services.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Escrow;
import brs.db.BurstIterator;
import brs.db.VersionedEntityTable;
import brs.db.store.EscrowStore;
import org.junit.Before;
import org.junit.Test;

public class EscrowServiceImplTest {

  private EscrowServiceImpl t;

  private EscrowStore mockEscrowStore;
  private VersionedEntityTable<Escrow> mockEscrowTable;

  @Before
  public void setUp() {
    mockEscrowStore = mock(EscrowStore.class);
    mockEscrowTable = mock(VersionedEntityTable.class);

    when(mockEscrowStore.getEscrowTable()).thenReturn(mockEscrowTable);

    t = new EscrowServiceImpl(mockEscrowStore);
  }


  @Test
  public void getAllEscrowTransactions() {
    final BurstIterator<Escrow> mockEscrowIterator = mock(BurstIterator.class);

    when(mockEscrowTable.getAll(eq(0), eq(-1))).thenReturn(mockEscrowIterator);

    assertEquals(mockEscrowIterator, t.getAllEscrowTransactions());
  }
}
