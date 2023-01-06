package hs.mediasystem.util.events.cache;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RangeCacheTest {

  RangeCache<Long> cache = new RangeCache<>(l -> l);

  @Nested
  class WhenEmpty {

    @Test
    void shouldFindNothing() {
      assertThat(cache.find(0)).isNull();
      assertThat(cache.find(10)).isNull();
      assertThat(cache.find(Long.MAX_VALUE)).isNull();
    }

    @Test
    void shouldRejectInsertionOfItemsInNonAscendingOrder() {
      assertThatThrownBy(() -> cache.insert(10, List.of(20L, 15L)))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("items must be in ascending order and contain no duplicate indices: [20, 15]");
    }

    @Test
    void shouldRejectInsertionOfItemsWithDuplicateIndices() {
      assertThatThrownBy(() -> cache.insert(10, List.of(20L, 20L)))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("items must be in ascending order and contain no duplicate indices: [20, 20]");
    }

    @Test
    void shouldRejectInsertionOfItemsWithIndicesBeforeStartOffset() {
      assertThatThrownBy(() -> cache.insert(10, List.of(9L, 20L)))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("items cannot contain items before start (10): 9");
    }

    @Test
    void shouldRejectInsertionOfNullList() {
      assertThatThrownBy(() -> cache.insert(10, null))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("items cannot be null or empty: null");
    }

    @Test
    void shouldRejectInsertionOfEmptyList() {
      assertThatThrownBy(() -> cache.insert(10, List.of()))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("items cannot be null or empty: []");
    }

    @Nested
    class AndARangeIsAdded {
      {
        cache.insert(10, List.of(11L, 12L, 15L, 20L));
      }

      @Test
      void shouldFindEntriesInRange() {
        assertThat(cache.find(10)).isEqualTo(11L);
        assertThat(cache.find(11)).isEqualTo(11L);
        assertThat(cache.find(12)).isEqualTo(12L);
        assertThat(cache.find(13)).isEqualTo(15L);
        assertThat(cache.find(19)).isEqualTo(20L);
        assertThat(cache.find(20)).isEqualTo(20L);
      }

      @Test
      void shouldNotFindEntriesOutsideRange() {
        assertThat(cache.find(9)).isNull();
        assertThat(cache.find(21)).isNull();
      }

      @Nested
      class AndANonTouchingNonOverlappingRangeIsAdded {
        {
          cache.insert(40, List.of(40L, 42L, 45L, 50L));
        }

        @Test
        void shouldFindEntriesInRanges() {
          assertThat(cache.find(10)).isEqualTo(11L);
          assertThat(cache.find(11)).isEqualTo(11L);
          assertThat(cache.find(12)).isEqualTo(12L);
          assertThat(cache.find(13)).isEqualTo(15L);
          assertThat(cache.find(19)).isEqualTo(20L);
          assertThat(cache.find(20)).isEqualTo(20L);
          assertThat(cache.find(40)).isEqualTo(40L);
          assertThat(cache.find(43)).isEqualTo(45L);
          assertThat(cache.find(50)).isEqualTo(50L);
        }

        @Test
        void shouldNotFindEntriesOutsideRange() {
          assertThat(cache.find(9)).isNull();
          assertThat(cache.find(21)).isNull();
          assertThat(cache.find(39)).isNull();
          assertThat(cache.find(51)).isNull();
        }

        @Nested
        class AndAnExactRangeIsInsertedInBetween {
          {
            cache.insert(21, List.of(25L, 39L));
          }

          @Test
          void shouldFindEntriesInRanges() {
            assertThat(cache.find(10)).isEqualTo(11L);
            assertThat(cache.find(11)).isEqualTo(11L);
            assertThat(cache.find(12)).isEqualTo(12L);
            assertThat(cache.find(13)).isEqualTo(15L);
            assertThat(cache.find(19)).isEqualTo(20L);
            assertThat(cache.find(20)).isEqualTo(20L);
            assertThat(cache.find(21)).isEqualTo(25L);
            assertThat(cache.find(30)).isEqualTo(39L);
            assertThat(cache.find(40)).isEqualTo(40L);
            assertThat(cache.find(43)).isEqualTo(45L);
            assertThat(cache.find(50)).isEqualTo(50L);
          }

          @Test
          void shouldNotFindEntriesOutsideRange() {
            assertThat(cache.find(9)).isNull();
            assertThat(cache.find(51)).isNull();
          }
        }

        @Nested
        class AndARangeIsInsertedInBetweenOverlappingPreviousRange {
          {
            cache.insert(15, List.of(15L, 20L, 25L, 39L));
          }

          @Test
          void shouldFindEntriesInRanges() {
            assertThat(cache.find(10)).isEqualTo(11L);
            assertThat(cache.find(11)).isEqualTo(11L);
            assertThat(cache.find(12)).isEqualTo(12L);
            assertThat(cache.find(13)).isEqualTo(15L);
            assertThat(cache.find(19)).isEqualTo(20L);
            assertThat(cache.find(20)).isEqualTo(20L);
            assertThat(cache.find(21)).isEqualTo(25L);
            assertThat(cache.find(30)).isEqualTo(39L);
            assertThat(cache.find(40)).isEqualTo(40L);
            assertThat(cache.find(43)).isEqualTo(45L);
            assertThat(cache.find(50)).isEqualTo(50L);
          }

          @Test
          void shouldNotFindEntriesOutsideRange() {
            assertThat(cache.find(9)).isNull();
            assertThat(cache.find(51)).isNull();
          }
        }

        @Nested
        class AndARangeIsInsertedInBetweenOverlappingNextRange {
          {
            cache.insert(21, List.of(25L, 39L, 40L, 45L));
          }

          @Test
          void shouldFindEntriesInRanges() {
            assertThat(cache.find(10)).isEqualTo(11L);
            assertThat(cache.find(11)).isEqualTo(11L);
            assertThat(cache.find(12)).isEqualTo(12L);
            assertThat(cache.find(13)).isEqualTo(15L);
            assertThat(cache.find(19)).isEqualTo(20L);
            assertThat(cache.find(20)).isEqualTo(20L);
            assertThat(cache.find(21)).isEqualTo(25L);
            assertThat(cache.find(30)).isEqualTo(39L);
            assertThat(cache.find(40)).isEqualTo(40L);
            assertThat(cache.find(43)).isEqualTo(45L);
            assertThat(cache.find(50)).isEqualTo(50L);
          }

          @Test
          void shouldNotFindEntriesOutsideRange() {
            assertThat(cache.find(9)).isNull();
            assertThat(cache.find(51)).isNull();
          }
        }

        @Nested
        class AndARangeIsInsertedInBetweenTouchingPreviousRangeOnly {
          {
            cache.insert(21, List.of(25L));
          }

          @Test
          void shouldFindEntriesInRanges() {
            assertThat(cache.find(10)).isEqualTo(11L);
            assertThat(cache.find(11)).isEqualTo(11L);
            assertThat(cache.find(12)).isEqualTo(12L);
            assertThat(cache.find(13)).isEqualTo(15L);
            assertThat(cache.find(19)).isEqualTo(20L);
            assertThat(cache.find(20)).isEqualTo(20L);
            assertThat(cache.find(21)).isEqualTo(25L);
            assertThat(cache.find(40)).isEqualTo(40L);
            assertThat(cache.find(43)).isEqualTo(45L);
            assertThat(cache.find(50)).isEqualTo(50L);
          }

          @Test
          void shouldNotFindEntriesOutsideRange() {
            assertThat(cache.find(9)).isNull();
            assertThat(cache.find(26)).isNull();
            assertThat(cache.find(39)).isNull();
            assertThat(cache.find(51)).isNull();
          }
        }

        @Nested
        class AndARangeIsInsertedInBetweenTouchingNextRangeOnly {
          {
            cache.insert(25, List.of(25L, 39L));
          }

          @Test
          void shouldFindEntriesInRanges() {
            assertThat(cache.find(10)).isEqualTo(11L);
            assertThat(cache.find(11)).isEqualTo(11L);
            assertThat(cache.find(12)).isEqualTo(12L);
            assertThat(cache.find(13)).isEqualTo(15L);
            assertThat(cache.find(19)).isEqualTo(20L);
            assertThat(cache.find(20)).isEqualTo(20L);
            assertThat(cache.find(25)).isEqualTo(25L);
            assertThat(cache.find(39)).isEqualTo(39L);
            assertThat(cache.find(40)).isEqualTo(40L);
            assertThat(cache.find(43)).isEqualTo(45L);
            assertThat(cache.find(50)).isEqualTo(50L);
          }

          @Test
          void shouldNotFindEntriesOutsideRange() {
            assertThat(cache.find(9)).isNull();
            assertThat(cache.find(21)).isNull();
            assertThat(cache.find(24)).isNull();
            assertThat(cache.find(51)).isNull();
          }
        }
      }
    }

    @Nested
    class AndAMaxNegativeRangeIsAdded {
      {
        cache.insert(Long.MIN_VALUE, List.of(11L, 12L, 15L, 20L));
      }

      @Test
      void shouldFindEntriesInRange() {
        assertThat(cache.find(Long.MIN_VALUE)).isEqualTo(11L);
        assertThat(cache.find(9)).isEqualTo(11L);
        assertThat(cache.find(10)).isEqualTo(11L);
        assertThat(cache.find(11)).isEqualTo(11L);
        assertThat(cache.find(12)).isEqualTo(12L);
        assertThat(cache.find(13)).isEqualTo(15L);
        assertThat(cache.find(19)).isEqualTo(20L);
        assertThat(cache.find(20)).isEqualTo(20L);
      }

      @Test
      void shouldNotFindEntriesOutsideRange() {
        assertThat(cache.find(21)).isNull();
      }
    }
  }
}
