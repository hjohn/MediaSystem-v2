package hs.database.core;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReflectorTest {

  @Test
  void shouldCreateReflectors() {
    assertThat(Reflector.of(Base.class, "a", "x", "y", "z", "angle")).isNotNull();
    assertThat(Reflector.of(Angle.class, "a")).isNotNull();
  }

  @Test
  void shouldRejectCreatingReflectorsWithMismatchingFieldNameCount() {
    assertThatThrownBy(() -> Reflector.of(Base.class, "a"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(Base.class + " argument index 1 of type [int] has no matching name in: [a]");

    assertThatThrownBy(() -> Reflector.of(Base.class, "a", "x", "y", "z", "angle", "extra"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(Base.class + " name \"extra\" at index 5 has no matching argument");
  }

  @Nested
  class WhenReflectorCreated {
    private final Reflector reflector = Reflector.of(Base.class, "a", "x", "y", "z", "angle");

    @Test
    void shouldCreateDirectMapper() {
      assertThat(reflector.asMapperFor(Base.class)).isNotNull();
    }

    @Test
    void shouldCreateCompoundMapper() {
      assertThat(reflector.asMapperFor(Compound.class)).isNotNull();
      assertThat(reflector.asMapperFor(Compound2.class)).isNotNull();
    }

    @Test
    void shouldExcludeFields() {
      assertThat(reflector.excluding("x", "y", "z").names()).isEqualTo(List.of("a", "", "", "", "angle"));
    }

    @Test
    void shouldRejectExcludingNonExistingFields() {
      assertThatThrownBy(() -> reflector.excluding("x", "y", "z", "b"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("unable to exclude non-existing fields: [b]");
    }

    @Test
    void shouldKeepFields() {
      assertThat(reflector.only("x", "y").names()).isEqualTo(List.of("", "x", "y", "", ""));
    }

    @Test
    void shouldRejectKeepingNonExistingFields() {
      assertThatThrownBy(() -> reflector.only("b"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("unable to keep non-existing fields: [b]");
    }

    @Test
    void shouldRejectCreatingMapperWithMismatchingRecordDefinition() {
      assertThatThrownBy(() -> reflector.asMapperFor(Angle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(Angle.class + " argument index 0 of type [double] does not match argument index 0 of type [class java.lang.String] in " + Base.class);

      assertThatThrownBy(() -> reflector.asMapperFor(Base2D.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(Base2D.class + " argument index 3 of type [double] does not match argument index 3 of type [int] in " + Base.class);

      assertThatThrownBy(() -> reflector.asMapperFor(DescribedBase2D.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(DescribedBase2D.class + " argument index 1 of type [class java.lang.String] does not match argument index 1 of type [int] in " + Base.class);

      assertThatThrownBy(() -> reflector.asMapperFor(AngleBase2D.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(Angle.class + " argument index 0 of type [double] does not match argument index 3 of type [int] in " + Base.class);

      assertThatThrownBy(() -> reflector.asMapperFor(NestedBase.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(NestedBase.class + " argument index 1 of type [class java.lang.String] does not match missing argument at index 5 in " + Base.class);

      assertThatThrownBy(() -> reflector.asMapperFor(BaseMinusAngle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(BaseMinusAngle.class + " is missing argument at index 4 to match argument index 4 of type [double] in " + Base.class);
    }

    @Nested
    class AndAResultSetNeedsMapping {
      private final RestrictedResultSet rs;

      AndAResultSetNeedsMapping(@Mock RestrictedResultSet rs) throws SQLException {
        this.rs = rs;

        when(rs.getString(1)).thenReturn("Alice");
        when(rs.getInt(2)).thenReturn(5);
        when(rs.getInt(3)).thenReturn(6);
        when(rs.getInt(4)).thenReturn(7);
        when(rs.getDouble(5)).thenReturn(45.0);
      }

      @Test
      void shouldCreateBaseRecord() throws SQLException {
        assertThat(reflector.asMapperFor(Base.class).map(rs)).isEqualTo(new Base("Alice", 5, 6, 7, 45.0));
      }

      @Test
      void shouldCreateCompoundRecord() throws SQLException {
        assertThat(reflector.asMapperFor(Compound.class).map(rs)).isEqualTo(new Compound("Alice", new Coordinate(new Coordinate2D(5, 6), 7), new Angle(45.0)));
      }

      @Test
      void shouldCreateCompound2Record() throws SQLException {
        assertThat(reflector.asMapperFor(Compound2.class).map(rs)).isEqualTo(new Compound2(new Text("Alice"), new Coordinate2D(5, 6), 7, 45.0));
      }
    }

    @Nested
    class AndReflectorHasHole {
      private final Reflector holedReflector = reflector.excluding("z");

      @Test
      void shouldCreateMapperWithMatchingHole() {
        assertThat(holedReflector.asMapperFor(Base2D.class)).isNotNull();
      }

      @Nested
      class AndAResultSetNeedsMapping {
        private final RestrictedResultSet rs;

        AndAResultSetNeedsMapping(@Mock RestrictedResultSet rs) throws SQLException {
          this.rs = rs;

          when(rs.getString(1)).thenReturn("Alice");
          when(rs.getInt(2)).thenReturn(5);
          when(rs.getInt(3)).thenReturn(6);
          when(rs.getDouble(4)).thenReturn(45.0);
        }

        @Test
        void shouldCreateBase2DRecord() throws SQLException {
          assertThat(holedReflector.asMapperFor(Base2D.class).map(rs)).isEqualTo(new Base2D("Alice", 5, 6, 45.0));
        }
      }
    }
  }

  record Base(String a, int x, int y, int z, double angle) {}

  record Compound(String a, Coordinate c, Angle angle) {}
  record Compound2(Text a, Coordinate2D coord2d, int z, double angle) {}
  record Coordinate(Coordinate2D coord2d, int z) {}
  record Coordinate2D(int x, int y) {}
  record Angle(double a) {}

  record Base2D(String a, int x, int y, double angle) {}
  record DescribedBase2D(Text a, String description, int x, int y, double angle) {}
  record NestedBase(Base b, String extraStuff) {}
  record AngleBase2D(String a, int x, int y, Angle angle) {}

  record Text(String text) {}

  record BaseMinusAngle(String a, int x, int y, int z) {}
}
