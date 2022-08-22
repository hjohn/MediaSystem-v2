package hs.mediasystem.ui.api.domain;

import java.time.LocalDate;
import java.util.Optional;

public record Serie(Optional<LocalDate> lastAirDate, Optional<Integer> totalSeasons, Optional<Integer> totalEpisodes) {
}
