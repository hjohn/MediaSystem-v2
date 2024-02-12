package hs.database.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class DefaultReflector implements Reflector {
  private final Class<? extends Record> baseType;
  private final List<String> names;

  DefaultReflector(Class<? extends Record> baseType, List<String> names) {
    this.baseType = Objects.requireNonNull(baseType, "baseType");
    this.names = names;

    if(names.stream().noneMatch(String::isEmpty)) {
      new ConstructorsBuilder(baseType).build(baseType, true);  // verifies that names count matches (when there are no holes)
    }
  }

  @Override
  public Class<?> baseType() {
    return baseType;
  }

  @Override
  public List<String> names() {
    return names;
  }

  @Override
  public Reflector excluding(String... names) {
    Set<String> set = new LinkedHashSet<>(List.of(names));
    Reflector reflector = new DefaultReflector(baseType, this.names.stream().map(n -> set.remove(n) ? "" : n).toList());

    if(!set.isEmpty()) {
      throw new IllegalArgumentException("unable to exclude non-existing fields: " + set);
    }

    return reflector;
  }

  @Override
  public Reflector only(String... names) {
    Set<String> set = new LinkedHashSet<>(List.of(names));
    Reflector reflector = new DefaultReflector(baseType, this.names.stream().map(n -> set.remove(n) ? n : "").toList());

    if(!set.isEmpty()) {
      throw new IllegalArgumentException("unable to keep non-existing fields: " + set);
    }

    return reflector;
  }

  @Override
  public Entries entries(Record r) {
    int length = r.getClass().getRecordComponents().length;

    if (length != names.size()) {
      throw new IllegalArgumentException("Record does not match the template " + names + "; expected " + names.size() + " record components, but got: " + length);
    }

    return new Entries(this, r);
  }

  @Override
  public Values values(Record r) {
    int length = r.getClass().getRecordComponents().length;

    if (length != names.size()) {
      throw new IllegalArgumentException("Record does not match the template " + names + "; expected " + names.size() + " record components, but got: " + length);
    }

    return new Values(this, r);
  }

  interface ThrowingBiFunction<T, U, R> {
    R apply(T t, U u) throws SQLException;
  }

  private static final Map<Class<?>, ThrowingBiFunction<RestrictedResultSet, Integer, ?>> MAP = Map.of(
    String.class, RestrictedResultSet::getString,
    Integer.class, RestrictedResultSet::getInt,
    Long.class, RestrictedResultSet::getLong,
    Double.class, RestrictedResultSet::getDouble,
    int.class, RestrictedResultSet::getInt,
    long.class, RestrictedResultSet::getLong,
    double.class, RestrictedResultSet::getDouble,
    byte[].class, RestrictedResultSet::getBytes
  );

  @Override
  public <T extends Record> SqlMapper<T> asMapperFor(Class<T> cls) {
    List<Constructor<?>> constructors = new ConstructorsBuilder(baseType).build(cls, true);

    return rs -> new RecordBuilder(rs, constructors.iterator()).build(cls);
  }

  private class RecordBuilder {
    private final RestrictedResultSet rs;
    private final Iterator<Constructor<?>> iterator;

    private int columnIndex = 1;

    public RecordBuilder(RestrictedResultSet rs, Iterator<Constructor<?>> iterator) {
      this.rs = rs;
      this.iterator = iterator;
    }

    private <T> T build(Class<T> cls) throws SQLException {
      RecordComponent[] recordComponents = cls.getRecordComponents();
      Object[] args = new Object[recordComponents.length];

      for(int i = 0; i < recordComponents.length; i++) {
        RecordComponent rc = recordComponents[i];

        if(rc.getType().isRecord()) {  // nested record?
          args[i] = build(rc.getType());
        }
        else {
          ThrowingBiFunction<RestrictedResultSet, Integer, ?> valueExtractor = MAP.get(rc.getType());

          if(valueExtractor == null) {
            args[i] = rs.getObject(columnIndex++, rc.getType());
          }
          else {
            args[i] = valueExtractor.apply(rs, columnIndex++);
          }
        }
      }

      try {
        @SuppressWarnings("unchecked")
        T newInstance = (T)iterator.next().newInstance(args);

        return newInstance;
      }
      catch(Exception e) {
        throw new IllegalStateException("construction failed for record of type: " + cls, e);
      }
    }
  }

  @Override
  public String toString() {
    return names.toString();
  }

  private class ConstructorsBuilder {
    private final Class<? extends Record> baseClass;
    private final RecordComponent[] recordComponents;

    private int componentIndex;

    ConstructorsBuilder(Class<? extends Record> baseClass) {
      this.baseClass = baseClass;
      this.recordComponents = baseClass.getRecordComponents();
    }

    private List<Constructor<?>> build(Class<? extends Record> cls, boolean topLevel) {
      try {
        List<Constructor<?>> constructors = new ArrayList<>();
        Class<?>[] paramTypes = Arrays.stream(cls.getRecordComponents())
          .map(RecordComponent::getType)
          .toArray(Class<?>[]::new);

        for(int i = 0; i < paramTypes.length; i++) {
          Class<?> c = paramTypes[i];

          if(c.isRecord()) {
            @SuppressWarnings("unchecked")
            Class<? extends Record> recordClass = (Class<? extends Record>)c;

            constructors.addAll(build(recordClass, false));
          }
          else {
            // Skip holes:
            while(componentIndex < recordComponents.length && componentIndex < names.size() && names.get(componentIndex).isEmpty()) {
              componentIndex++;
            }

            if(componentIndex >= recordComponents.length) {
              throw new IllegalStateException(cls + " argument index " + i + " of type [" + c + "] does not match missing argument at index " + componentIndex + " in " + baseClass);
            }
            if(componentIndex >= names.size()) {
              throw new IllegalStateException(baseClass + " argument index " + componentIndex + " of type [" + c + "] has no matching name in: " + names);
            }
            if(!c.equals(recordComponents[componentIndex].getType())) {
              throw new IllegalStateException(cls + " argument index " + i + " of type [" + c + "] does not match argument index " + componentIndex + " of type [" + recordComponents[componentIndex].getType() + "] in " + baseClass);
            }

            componentIndex++;
          }
        }

        if(topLevel && componentIndex != recordComponents.length) {
          throw new IllegalStateException(cls + " is missing argument at index " + paramTypes.length + " to match argument index " + componentIndex + " of type [" + recordComponents[componentIndex].getType() + "] in " + baseClass);
        }
        if(topLevel && componentIndex != names.size()) {
          throw new IllegalStateException(baseClass + " name \"" + names.get(componentIndex) + "\" at index " + componentIndex + " has no matching argument");
        }

        constructors.add(cls.getDeclaredConstructor(paramTypes));

        return constructors;
      }
      catch(NoSuchMethodException e) {
        throw new IllegalStateException("record is missing its canonical constructor: " + cls);
      }
    }
  }
}