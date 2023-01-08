package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.domain.work.Match;

import java.net.URI;
import java.util.List;

public record IdentificationEvent(URI location, List<? extends WorkDescriptor> descriptors, Match match) {}
