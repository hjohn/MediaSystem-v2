package hs.mediasystem.db.services.domain;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;

public record MatchedWork(Match match, Work work) {
}
