package hs.mediasystem.api.datasource.domain;

import java.time.LocalDate;
import java.util.List;

public class ProductionCollection extends AbstractCollection<Production> {
  private transient LocalDate firstReleaseDate;
  private transient LocalDate lastReleaseDate;
  private transient LocalDate nextReleaseDate;

  public ProductionCollection(CollectionDetails collectionDetails, List<Production> items) {
    super(collectionDetails, items);
  }

  /**
   * Returns the first release date (can be in the future).
   *
   * @return a date or <code>null</code> if unavailable
   */
  public LocalDate getFirstReleaseDate() {
    ensureReleaseDatesCalculated();

    return firstReleaseDate;
  }

  /**
   * Returns the last release date.  If the first release date
   * is in the future, this will return <code>null</code>.
   *
   * @return a date in the past or <code>null</code>
   */
  public LocalDate getLastReleaseDate() {
    ensureReleaseDatesCalculated();

    return lastReleaseDate;
  }

  /**
   * Returns the next release date.
   *
   * @return a date in the future or <code>null</code>
   */
  public LocalDate getNextReleaseDate() {
    ensureReleaseDatesCalculated();

    return nextReleaseDate;
  }

  private void ensureReleaseDatesCalculated() {
    LocalDate now = LocalDate.now();

    if(firstReleaseDate != null && (nextReleaseDate == null || nextReleaseDate.isAfter(now))) {  // exit if calculated and next release date is still in the future
      return;
    }

    LocalDate first = null;   // first release
    LocalDate next = null;    // upcoming future release
    LocalDate last = null;    // last release

    for(Production production : getItems()) {
      LocalDate date = production.getDate().orElse(null);

      if(date != null) {
        first = first == null || date.isBefore(first) ? date : first;
        next = (next == null || date.isAfter(next)) && now.isBefore(date) ? date : next;
        last = (last == null || last.isBefore(date)) && date.isBefore(now) ? date : last;
      }
    }

    firstReleaseDate = first;
    lastReleaseDate = last;
    nextReleaseDate = next;
  }
}
