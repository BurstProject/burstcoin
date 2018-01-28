package brs.services.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Alias;
import brs.Alias.Offer;
import brs.common.AbstractUnitTest;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedEntityTable;
import brs.db.store.AliasStore;
import org.junit.Before;
import org.junit.Test;

public class AliasServiceImplTest extends AbstractUnitTest {

  private AliasServiceImpl t;

  private AliasStore aliasStoreMock;
  private VersionedEntityTable<Alias> aliasTableMock;
  private BurstKey.LongKeyFactory<Alias> aliasDbKeyFactoryMock;
  private VersionedEntityTable<Offer> offerTableMock;
  private BurstKey.LongKeyFactory<Offer> offerDbKeyFactoryMock;

  @Before
  public void setUp() {
    aliasStoreMock = mock(AliasStore.class);
    aliasTableMock = mock(VersionedEntityTable.class);
    aliasDbKeyFactoryMock = mock(LongKeyFactory.class);
    offerTableMock = mock(VersionedEntityTable.class);
    offerDbKeyFactoryMock = mock(LongKeyFactory.class);

    when(aliasStoreMock.getAliasTable()).thenReturn(aliasTableMock);
    when(aliasStoreMock.getAliasDbKeyFactory()).thenReturn(aliasDbKeyFactoryMock);
    when(aliasStoreMock.getOfferTable()).thenReturn(offerTableMock);
    when(aliasStoreMock.getOfferDbKeyFactory()).thenReturn(offerDbKeyFactoryMock);

    t = new AliasServiceImpl(aliasStoreMock);
  }

  @Test
  public void getAlias() {
    final String aliasName = "aliasName";
    final Alias mockAlias = mock(Alias.class);

    when(aliasStoreMock.getAlias(eq(aliasName))).thenReturn(mockAlias);

    assertEquals(mockAlias, t.getAlias(aliasName));
  }

  @Test
  public void getAlias_byId() {
    final long id = 123l;
    final BurstKey mockKey = mock(BurstKey.class);
    final Alias mockAlias = mock(Alias.class);

    when(aliasDbKeyFactoryMock.newKey(eq(id))).thenReturn(mockKey);
    when(aliasTableMock.get(eq(mockKey))).thenReturn(mockAlias);

    assertEquals(mockAlias, t.getAlias(id));
  }

  @Test
  public void getOffer() {
    final Long aliasId = 123l;
    final Alias mockAlias = mock(Alias.class);
    when(mockAlias.getId()).thenReturn(aliasId);
    final BurstKey mockOfferKey = mock(BurstKey.class);
    final Offer mockOffer = mock(Offer.class);

    when(offerDbKeyFactoryMock.newKey(eq(aliasId))).thenReturn(mockOfferKey);
    when(offerTableMock.get(eq(mockOfferKey))).thenReturn(mockOffer);

    assertEquals(mockOffer, t.getOffer(mockAlias));
  }

  @Test
  public void getAliasCount() {
    when(aliasTableMock.getCount()).thenReturn(5);
    assertEquals(5L, t.getAliasCount());
  }

  @Test
  public void getAliasesByOwner() {
    final long accountId = 123L;
    final int from = 0;
    final int to = 1;

    final BurstIterator<Alias> mockAliasIterator = mockBurstIterator();

    when(aliasStoreMock.getAliasesByOwner(eq(accountId), eq(from), eq(to))).thenReturn(mockAliasIterator);

    assertEquals(mockAliasIterator, t.getAliasesByOwner(accountId, from, to));
  }
}
