package hs.database.core;

import java.sql.SQLException;

public interface SqlMapper<T> {
  T map(RestrictedResultSet rs) throws SQLException;
}
