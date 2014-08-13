package nxt.util;

public interface Observable<T,E extends Enum<E>> {

    boolean addListener(Listener<T> listener, E eventType);

    boolean removeListener(Listener<T> listener, E eventType);

}
