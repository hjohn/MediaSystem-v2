package hs.mediasystem.db.core;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;

import java.net.URI;
import java.util.List;

public record IdentificationEvent(URI location, List<? extends WorkDescriptor> descriptors, Match match) {}
