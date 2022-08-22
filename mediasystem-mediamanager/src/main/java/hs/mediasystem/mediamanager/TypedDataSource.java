package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;

public record TypedDataSource(DataSource dataSource, MediaType mediaType) {}