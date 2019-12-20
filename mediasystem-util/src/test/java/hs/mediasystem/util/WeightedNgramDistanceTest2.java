package hs.mediasystem.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class WeightedNgramDistanceTest2 {
  private static final double PREC = 0.01;

  @Test
  public void shouldMatchBetter() {
    assertTrue(match("Star Wars", "Star War", "Star Gate"));
    assertTrue(match("Star Gates", "Star Gate", "Star Wars"));
    assertTrue(match("Star Wars Rogue One", "Rogue One A Star Wars Story", "The Rogue One A Star Wars Toy Story"));
//    assertTrue(match("Star Wras", "Star Wars", "Star Wrappers"));
    assertTrue(match("Star Wars", "Star Wars", "Wars Star"));

    assertEquals(0.92, WeightedNgramDistance.calculate("Alien 3", "Alien³"), PREC);
    assertEquals(0.85, WeightedNgramDistance.calculate("Alien 3", "Alien 4"), PREC);
    assertEquals(1.0, WeightedNgramDistance.calculate("Alien 3", "Alien 3"), PREC);

    assertEquals(0.60, WeightedNgramDistance.calculate("Alien", "Aliens"), PREC);
    assertEquals(1.0, WeightedNgramDistance.calculate("Aliens", "Aliens"), PREC);
    assertEquals(0.45, WeightedNgramDistance.calculate("Alienz", "Aliens"), PREC);

    assertEquals(1.0, WeightedNgramDistance.calculate("Star Wars", "Star Wars"), PREC);
    assertEquals(0.9, WeightedNgramDistance.calculate("Star Wars", "Wars Star"), PREC);
  }

  @Test
  public void shouldMatch() {
    double n = WeightedNgramDistance.calculate(
      "Requiem A Remembrance of Star Trek The Next Generation Part One The Needs of the Many",
      "Requiem: A Rememberance of Star Trek: The Next Generation - Part 1: The Needs of the Many");

    System.out.println(n);

    n = WeightedNgramDistance.calculate(
      "Requiem: A Rememberance of Star Trek: The Next Generation - Part One: The Needs of the Many",
      "Requiem: A Rememberance of Star Trek: The Next Generation - Part 1: The Needs of the Many");

    System.out.println(n);

    n = WeightedNgramDistance.calculate(
      "Requiem: A Remembrance of Star Trek: The Next Generation - Part 1: The Needs of the Many",
      "Requiem: A Rememberance of Star Trek: The Next Generation - Part 1: The Needs of the Many");

    System.out.println(n);

    n = WeightedNgramDistance.calculate(
      "Requiem: A Remembrance of Star Trek: The Next Generation - Part 1: The Needs of the Many And He was a jolly good fellow in the americias",
      "Requiem: A Rememberance of Star Trek: The Next Generation - Part 1: The Needs of the Many And He was a jolly good fellow in the americias");

    System.out.println(n);

  }

  private static boolean match(String p, String s1, String s2) {
    return WeightedNgramDistance.calculate(p, s1) > WeightedNgramDistance.calculate(p, s2);
  }
}
