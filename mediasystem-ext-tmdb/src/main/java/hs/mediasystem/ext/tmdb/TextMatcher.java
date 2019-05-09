package hs.mediasystem.ext.tmdb;

import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.util.WeightedNgramDistance;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class TextMatcher {
  private static final double MATCH_NAME_SCORE = 0.6;
  private static final double MATCH_YEAR_SCORE = 0.4;
  private static final double MATCH_ADJACENT_YEAR_SCORE = 0.15;

  public static Match createMatch(LocalDate releaseDate, String titleToMatch, Integer year, String nodeTitle, String id) {
    Integer movieYear = extractYear(releaseDate);

    MatchType nameMatchType = MatchType.NAME;
    double score = WeightedNgramDistance.calculate(nodeTitle.toLowerCase(), titleToMatch.toLowerCase());

    if(year != null && movieYear != null) {
      if(year.equals(movieYear)) {
        nameMatchType = MatchType.NAME_AND_RELEASE_DATE;
        score *= MATCH_NAME_SCORE;
        score += MATCH_YEAR_SCORE;
      }
      else if(Math.abs(year - movieYear) == 1) {
        nameMatchType = MatchType.NAME_AND_RELEASE_DATE;
        score *= MATCH_NAME_SCORE;
        score += MATCH_ADJACENT_YEAR_SCORE;
      }
    }

    return new Match(releaseDate, nameMatchType, id, nodeTitle, score * 100);
  }

  private static Integer extractYear(LocalDate date) {
    if(date == null) {
      return null;
    }

    return date.getYear();
  }

  public static List<String> createVariations(String fullTitle) {
    if(fullTitle == null || fullTitle.isEmpty()) {
      return Collections.emptyList();
    }

    return createPronounVariations(fullTitle);
  }

  private static List<String> createPronounVariations(String title) {
    int comma = title.lastIndexOf(", ");  // lastIndexOf for titles like "Good, the Bad and the Ugly, The"

    if(comma > 0) {
      String strippedTitle = title.substring(0, comma);
      String pronoun = title.substring(comma + 2);

      if(Character.isUpperCase(pronoun.charAt(0)) && pronoun.length() < 4) {  // Allows for "The", "A", "Le", "Les", etc..
        return List.of(title, pronoun + " " + strippedTitle, strippedTitle);
      }
    }

    return List.of(title);
  }

  public static class Match {
    private final LocalDate releaseDate;
    private final MatchType type;
    private final String id;
    private final String name;
    private final double score;

    Match(LocalDate releaseDate, MatchType matchType, String id, String name, double score) {
      this.releaseDate = releaseDate;
      this.type = matchType;
      this.id = id;
      this.name = name;
      this.score = score;
    }

    public String getId() {
      return id;
    }

    public MatchType getType() {
      return type;
    }

    public double getScore() {
      return score;
    }

    public double getNormalizedScore() {
      return type == MatchType.NAME ? score * 0.6 : score;
    }

    @Override
    public String toString() {
      return String.format("Match[%6.2f%% \"%s\" [%s]]", score, name, releaseDate);
    }
  }
}
