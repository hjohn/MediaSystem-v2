package hs.mediasystem.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeightedNgramDistanceTest {
  private enum MatchType {PERFECT, EXCELLENT, GOOD, AVERAGE, POOR}

  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]
      {
        {MatchType.PERFECT, "Star Wars", "Star Wars"},
        {MatchType.AVERAGE, "Star Wars Rogue One", "Rogue One: A Star Wars Story"},
        {MatchType.AVERAGE, "Star Wars", "Star Warz"},  // EXCELLENT with Levenshtein
        {MatchType.AVERAGE, "Star Wars", "Staar Wars"},  // EXCELLENT with Levenshtein
        {MatchType.AVERAGE, "Star Wars", "Satr Wars"},  // GOOD with Levenshtein
        {MatchType.POOR, "Star Wars Episode IV: A new hope", "Star Wars - 04 - A new hope"},    // GOOD with Levenshtein
        {MatchType.POOR, "Star Wars", "Stargate"},  // AVERAGE with Levenshtein
        {MatchType.POOR, "Star Wars: Clone Wars", "Star Wars"},
        {MatchType.POOR, "Star Wars: Clone Wars", "Star Wars 4"},
        {MatchType.POOR, "Star Wars: Clone Wars", "Star Wars 4 A New Hope"},
        {MatchType.POOR, "Star Wars: Episode IV - A New Hope", "Star Wars 4 A New Hope"},
        {MatchType.POOR, "Star Wars", "Star Wars - 04 - A new hope"},
        {MatchType.POOR, "Star Wars", "Fantasic Four"},
        {MatchType.POOR, "Star Wars: Episode IV - A New Hope", "Star Wars"},
        {MatchType.POOR, "Star Wars: Episode IV - A New Hope", "Star Wars 4"},
        {MatchType.PERFECT, "V for Vendetta", "V for Vendetta"},
        {MatchType.POOR, "V for Vendetta", "Sympathy for Lady Vengeance"},
        {MatchType.PERFECT, "21", "21"},
        {MatchType.POOR, "21", "22"},
        {MatchType.POOR, "21", "B2"},
        {MatchType.PERFECT, "300", "300"},
        {MatchType.AVERAGE, "300", "301"},
        {MatchType.POOR, "300", "299"},
        {MatchType.AVERAGE, "300", "3000"},
      }
    );
  }

  @ParameterizedTest
  @MethodSource("data")
  public void shouldMatchWellEnough(MatchType matchType, String text, String matchText) {
    double compare = WeightedNgramDistance.calculate(text, matchText);
    String failureText = "\"" + text + "\" vs \"" + matchText + "\"; expected=" + matchType + ", but was: " + compare;

    switch(matchType) {
    case PERFECT:
      assertEquals(1.0, compare, 0.00001, failureText);
      break;
    case EXCELLENT:
      assertTrue(compare >= 0.85, failureText);
      break;
    case GOOD:
      assertTrue(compare >= 0.66, failureText);
      break;
    case AVERAGE:
      assertTrue(compare >= 0.4, failureText);
      break;
    case POOR:
      assertTrue(compare < 0.4, failureText);
      break;
    }
  }
}
