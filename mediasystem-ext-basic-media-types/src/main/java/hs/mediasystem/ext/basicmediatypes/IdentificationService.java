package hs.mediasystem.ext.basicmediatypes;

public interface IdentificationService<T extends MediaStream<?>> {
  DataSource getDataSource();
  Identification identify(T stream);
}
