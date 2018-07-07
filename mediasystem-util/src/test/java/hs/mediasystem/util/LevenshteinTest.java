package hs.mediasystem.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LevenshteinTest {
  private enum MatchType {PERFECT, EXCELLENT, GOOD, AVERAGE, POOR}

  private final MatchType matchType;
  private final String text;
  private final String matchText;

  public LevenshteinTest(MatchType matchType, String text, String matchText) {
    this.matchType = matchType;
    this.text = text;
    this.matchText = matchText;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]
      {
        {MatchType.PERFECT, "Star Wars", "Star Wars"},
        {MatchType.EXCELLENT, "Star Wars", "Star Warz"},
        {MatchType.EXCELLENT, "Star Wars", "Staar Wars"},
        {MatchType.GOOD, "Star Wars", "Satr Wars"},
        {MatchType.GOOD, "Star Wars Episode IV: A new hope", "Star Wars - 04 - A new hope"},
        {MatchType.AVERAGE, "Star Wars", "Stargate"},
        {MatchType.AVERAGE, "Star Wars: Clone Wars", "Star Wars"},
        {MatchType.AVERAGE, "Star Wars: Clone Wars", "Star Wars 4"},
        {MatchType.AVERAGE, "Star Wars: Clone Wars", "Star Wars 4 A New Hope"},
        {MatchType.AVERAGE, "Star Wars: Episode IV - A New Hope", "Star Wars 4 A New Hope"},
        {MatchType.POOR, "Star Wars", "Star Wars - 04 - A new hope"},
        {MatchType.POOR, "Star Wars", "Fantasic Four"},
        {MatchType.POOR, "Star Wars: Episode IV - A New Hope", "Star Wars"},
        {MatchType.POOR, "Star Wars: Episode IV - A New Hope", "Star Wars 4"}
      }
    );
  }

  @Test
  public void shouldMatchWellEnough() {
    double compare = Levenshtein.compare(text.toLowerCase(), matchText.toLowerCase());

    switch(matchType) {
    case PERFECT:
      assertEquals(1.0, compare, 0.00001);
      break;
    case EXCELLENT:
      assertTrue(compare >= 0.85);
      break;
    case GOOD:
      assertTrue(compare >= 0.66);
      break;
    case AVERAGE:
      assertTrue(compare >= 0.4);
      break;
    case POOR:
      assertTrue(compare < 0.4);
      break;
    }
  }
}
