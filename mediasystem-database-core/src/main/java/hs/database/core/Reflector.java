package hs.database.core;

import java.util.List;
import java.util.Objects;

public interface Reflector {

  /**
   * Creates a reflector for the given record type, mapping each of its
   * components (in order) to the given (database field) names.
   *
   * @param baseType a record type, cannot be {@code null}
   * @param names aliases used for each of the record's components, cannot be {@code null}
   * @return a reflector, never {@code null}
   * @throws IllegalStateException when the number of names given does not match the number of components in the given type (and its sub types)
   */
  static Reflector of(Class<? extends Record> baseType, String... names) {
    List<String> list = List.of(Objects.requireNonNull(names, "names"));

    return new DefaultReflector(baseType, list);
  }

  /**
   * Returns the base type for this reflector. This remains the same for
   * derived versions of this reflector.
   *
   * @return the base type, never {@code null}
   */
  Class<?> baseType();

  /**
   * Returns the list of field names for this reflector. The list matches
   * the positions of the components in the reflectors base record type.
   * Holes are represented as empty strings.
   *
   * @return the list of field names for this reflector, never {@code null}
   */
  List<String> names();

  /**
   * Creates a new reflector based on this reflector, but excluding the given
   * names.
   *
   * @param names an array of names to exclude, cannot be {@code null}
   * @return a new reflector minus the given names, never {@code null}
   * @throws IllegalArgumentException when one or more names are missing
   */
  Reflector excluding(String... names);

  /**
   * Creates a new reflector based on this reflector, but keeping only the
   * given names.
   *
   * @param names an array of names to keep, cannot be {@code null}
   * @return a new reflector with only the given names, never {@code null}
   * @throws IllegalArgumentException when one or more names are missing
   */
  Reflector only(String... names);

  /**
   * Given a record, provides a template parameter that inserts its fields
   * comma separated in the form of "field = value" suitable for UPDATE statements.
   *
   * @param r a record, cannot be {@code null}
   * @return an entries template parameter, never {@code null}
   */
  Entries entries(Record r);

  /**
   * Given a record, provides a template parameter that inserts its values
   * comma separated, suitable for the INSERT statement's VALUES clause.
   *
   * @param r a record, cannot be {@code null}
   * @return a values template parameter, never {@code null}
   */
  Values values(Record r);

  /**
   * Creates a mapper for the given type based on this reflector. The given
   * type can have nested types, as long as their component types match the field
   * types and order and skips any excluded fields in the base type of the reflector.
   * <p>
   * Note that only the types are checked to be matching. The names of the component
   * field names are not verified to match.
   * <p>
   * Given the following records and reflector:
   * {@snippet :
   *     record Address(String streetName, String city) {}
   *     record Employee(String name, int age, Address address) {}
   *     record Person(String name, int age) {}
   *     record PersonEmployee(Person person, Address address) {}
   *
   *     Reflector reflector = Reflector.of(Employee.class, "name", "age", "streetName", "city");
   *     Reflector reflectorWithoutAddress = reflector.excluding("streetName", "city");
   * }
   * Then you can create the following valid mappers;
   * {@snippet :
   *     reflector.asMapperFor(Employee.class);
   *     reflector.asMapperFor(PersonEmployee.class);
   *     reflectorWithoutAddress.asMapperFor(Person.class);
   * }
   * The following mappers are not allowed:
   * {@snippet :
   *     reflector.asMapperFor(Person.class);  // lacks fields to map streetName and city
   *     reflector.asMapperFor(Address.class);  // lacks fields to map name and age
   *     reflectorWithoutAddress.asMapperFor(Employee.class);  // needs fields to maps streetName and city
   * }
   *
   * @param <T> the mapped type
   * @param cls the type to map, cannot be {@code null}
   * @return a mapper, never {@code null}
   * @throws IllegalStateException when the given type could not be mapped onto the
   *   base type of this reflector
   */
  <T extends Record> SqlMapper<T> asMapperFor(Class<T> cls);

  // TODO add mapper without parameter?  Unsupported if there are any holes

  final class Entries {  // better as class, as records are used to provide values as well, to avoid mixing them up in StringTemplates
    private final Reflector reflector;
    private final Record data;

    Entries(Reflector reflector, Record data) {
      this.reflector = reflector;
      this.data = data;
    }

    public Reflector reflector() {
      return reflector;
    }

    public Record data() {
      return data;
    }

    @Override
    public String toString() {
      return "Entries[reflector=" + reflector + ", data=" + data + "]";
    }
  }

  final class Values {
    private final Reflector reflector;
    private final Record data;

    Values(Reflector reflector, Record data) {
      this.reflector = reflector;
      this.data = data;
    }

    public Reflector reflector() {
      return reflector;
    }

    public Record data() {
      return data;
    }

    @Override
    public String toString() {
      return "Values[reflector=" + reflector + ", data=" + data + "]";
    }
  }
}