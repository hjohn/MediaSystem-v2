package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.domain.work.Match;

import java.net.URI;
import java.util.List;

public record IdentificationEvent(URI location, List<? extends Release> releases, Match match) {}
