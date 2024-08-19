package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.domain.Classification;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.Attributes;

import java.net.URI;

class MinimalWorkSupport {

  /**
   * DataSource which uses as key a {@link URI}.<p>
   *
   * This internal data source is used for items that are part of a serie but have
   * not been matched up to a known episode or special.
   */
  private static final String DEFAULT_DATA_SOURCE_NAME = "@INTERNAL";

  static Production createMinimalDescriptor(Discovery discovery) {
    return new Production(
      new WorkId(DataSource.instance(DEFAULT_DATA_SOURCE_NAME), discovery.mediaType(), discovery.location().toString()),  // TODO slash not allowed in location.. there is a trick to determine parent there
      createMinimalDetails(discovery.attributes()),
      null,
      null,
      null,
      Classification.EMPTY,
      0.0
    );
  }

  private static Details createMinimalDetails(Attributes attributes) {
    String title = attributes.get(Attribute.TITLE);
    String subtitle = attributes.get(Attribute.SUBTITLE);

    if(title == null || title.isBlank()) {
      title = subtitle;
      subtitle = null;
    }

    if(title == null || title.isBlank()) {
      title = "(Untitled)";
    }

    return new Details(
      title,
      subtitle,
      attributes.get(Attribute.DESCRIPTION),
      null,
      null,  // no images derived from stream image captures are provided, front-end should do appropriate fall backs
      null,
      null
    );
  }
}
