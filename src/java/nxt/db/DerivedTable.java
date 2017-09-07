package nxt.db;

/**
 * Created by jens on 10.08.2017.
 */
public interface DerivedTable
{
    void rollback(int height);

    void truncate();

    void trim(int height);

    void finish();
}
