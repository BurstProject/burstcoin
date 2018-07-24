package brs.common;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import brs.db.BurstIterator;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.mockito.stubbing.Answer;

public abstract class AbstractUnitTest {

  protected <T> BurstIterator<T> mockBurstIterator(List<T> items) {
    final BurstIterator mockIterator = mock(BurstIterator.class);
    final Iterator<T> it = items.iterator();

    when(mockIterator.hasNext()).thenAnswer((Answer<Boolean>) invocationOnMock -> it.hasNext());
    when(mockIterator.next()).thenAnswer((Answer<T>) invocationOnMock -> it.next());

    return mockIterator;
  }

  protected <T> BurstIterator<T> mockBurstIterator(T... items) {
    return mockBurstIterator(Arrays.asList(items));
  }

  protected String stringWithLength(int length) {
    String result = "";

    for(int i = 0; i < length; i++) {
      result += "a";
    }

    return result;
  }

}
