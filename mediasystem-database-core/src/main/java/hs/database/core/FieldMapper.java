package hs.database.core;

import java.util.HashMap;
import java.util.Map;

public class FieldMapper {
  private final Map<String, Integer> indexByFieldName = new HashMap<>();
  private final Map<String, Integer> indexByTableAndFieldName = new HashMap<>();

  public void add(String tableName, String fieldName, int index) {
    if(indexByFieldName.containsKey(fieldName)) {
      indexByFieldName.put(fieldName, -1);
    }
    else {
      indexByFieldName.put(fieldName, index);
    }

    indexByTableAndFieldName.put(tableName + "/" + fieldName, index);
  }

  public int get(String name) {
    if(name.indexOf('/') >= 0) {
      return indexByTableAndFieldName.get(name);
    }

    int index = indexByFieldName.get(name);

    if(index == -1) {
      throw new IllegalArgumentException("fieldName is ambigious: " + name);
    }

    return index;
  }

  public int getColumnCount() {
    return indexByTableAndFieldName.size();
  }
}
