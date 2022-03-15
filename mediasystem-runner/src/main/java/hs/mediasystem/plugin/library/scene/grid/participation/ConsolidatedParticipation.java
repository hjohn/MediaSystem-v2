package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.ui.api.domain.Role.Type;
import hs.mediasystem.ui.api.domain.Work;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ConsolidatedParticipation {
  private static final List<Type> ROLE_TYPE_ORDER = Arrays.asList(Type.CAST, Type.GUEST_STAR, Type.CREW, null);

  private final Work work;
  private final List<Participation> lines = new ArrayList<>();

  private double popularity;
  private Type roleType;

  public ConsolidatedParticipation(Work work) {
    this.work = work;
    this.popularity = work.getDetails().getPopularity().orElse(0.0);
  }

  public Work getWork() {
    return work;
  }

  public double getPopularity() {
    return popularity;
  }

  public Type getRoleType() {
    return roleType;
  }

  public void addParticipation(Participation participation) {
    lines.add(participation);

    this.popularity = Math.max(this.popularity, participation.getPopularity());
    this.roleType = ROLE_TYPE_ORDER.indexOf(roleType) > ROLE_TYPE_ORDER.indexOf(participation.getRole().getType()) ? participation.getRole().getType() : this.roleType;
  }

  public String getParticipationText() {
    return lines.stream()
      .sorted(Comparator.comparing(p -> ROLE_TYPE_ORDER.indexOf(p.getRole().getType())))
      .map(ConsolidatedParticipation::createParticipationLine)
      .filter(s -> !s.isBlank())
      .collect(Collectors.joining("; "));
  }

  private static String createParticipationLine(Participation participation) {
    Role role = participation.getRole();

    String text = role.getCharacter() != null && !role.getCharacter().isBlank() ?
      "as " + role.getCharacter() :
      role.getJob() != null && !role.getJob().isBlank() ? role.getJob() : "";

    if(!text.isBlank() && participation.getEpisodeCount() > 1) {
      text += " [x" + participation.getEpisodeCount() + "]";
    }

    return text;
  }
}