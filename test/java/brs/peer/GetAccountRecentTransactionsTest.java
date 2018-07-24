package brs.peer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Blockchain;
import brs.Transaction;
import brs.TransactionType.DigitalGoods;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.JSONParam;
import brs.common.TestConstants;
import brs.db.BurstIterator;
import brs.services.AccountService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountRecentTransactionsTest extends AbstractUnitTest {

  private GetAccountRecentTransactions t;

  private AccountService mockAccountService;
  private Blockchain mockBlockchain;

  @Before
  public void setUp() {
    mockAccountService = mock(AccountService.class);
    mockBlockchain = mock(Blockchain.class);

    t = new GetAccountRecentTransactions(mockAccountService, mockBlockchain);
  }

  @Test
  public void processRequest() {
    final String accountId = TestConstants.TEST_ACCOUNT_NUMERIC_ID;

    final JSONObject request = QuickMocker.jsonObject(new JSONParam("account", accountId));

    final Peer peerMock = mock(Peer.class);

    final Account mockAccount = mock(Account.class);

    final Transaction mockTransaction = mock(Transaction.class);
    when(mockTransaction.getType()).thenReturn(DigitalGoods.DELISTING);
    final BurstIterator<Transaction> transactionsIterator = mockBurstIterator(mockTransaction);

    when(mockAccountService.getAccount(eq(TestConstants.TEST_ACCOUNT_NUMERIC_ID_PARSED))).thenReturn(mockAccount);
    when(mockBlockchain.getTransactions(eq(mockAccount), eq(0), eq((byte) -1), eq((byte) 0), eq(0), eq(0), eq(9))).thenReturn(transactionsIterator);

    final JSONObject result = (JSONObject) t.processRequest(request, peerMock);
    assertNotNull(result);

    final JSONArray transactionsResult = (JSONArray) result.get("transactions");
    assertNotNull(transactionsResult);
    assertEquals(1, transactionsResult.size());
  }

}
