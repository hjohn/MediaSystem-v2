package hs.mediasystem.db.services.domain;

import hs.mediasystem.api.datasource.domain.stream.Work;
import hs.mediasystem.domain.work.Match;

public record MatchedWork(Match match, Work work) {
}
