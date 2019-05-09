package hs.mediasystem.util;

import hs.mediasystem.util.SizeFormatter.AutoDoubleFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SizeFormatterTest {

  @Test
  public void test() {
    AutoDoubleFormat format = new AutoDoubleFormat(3);

    assertEquals("41.3 μ", format.format(0.0000413));
    assertEquals("4.13 m", format.format(0.00413));
    assertEquals("1.00", format.format(1.0));
    assertEquals("3.14", format.format(3.1415));
    assertEquals("1.49", format.format(1.4892));
    assertEquals("14.9 k", format.format(14892.0));
    assertEquals("-41.3 μ", format.format(-0.0000413));
    assertEquals("-4.13 m", format.format(-0.00413));
    assertEquals("-1.00", format.format(-1.0));
    assertEquals("-3.14", format.format(-3.1415));
    assertEquals("-1.49", format.format(-1.4892));
    assertEquals("-14.9 k", format.format(-14892.0));

    assertEquals("1.49 P", format.format(1489246643874893.0));
    assertEquals("1.49 E", format.format(1489246643874993893.0));
    assertEquals("1489 Y", format.format(1489246643878798473944993893.0));
    assertEquals("-1.49 P", format.format(-1489246643874893.0));
    assertEquals("-1.49 E", format.format(-1489246643874993893.0));
    assertEquals("-14.9 Y", format.format(-14892466438787984739449393.0));
    assertEquals("-149 Y", format.format(-148924664387879847394499393.0));
    assertEquals("-1489 Y", format.format(-1489246643878798473944993893.0));
    assertEquals("-15489 Y", format.format(-15489246643878798473944993893.0));

    assertEquals("2.28 p", format.format(0.00000000000227748));
    assertEquals("923 a", format.format(0.0000000000000009227748));
    assertEquals("19.2 z", format.format(0.000000000000000000019227748));
    assertEquals("0.0219 y", format.format(0.0000000000000000000000000219227748));
    assertEquals("0.00219 y", format.format(0.00000000000000000000000000219227748));
    assertEquals("0.000219 y", format.format(0.000000000000000000000000000219227748));
    assertEquals("0.0000219 y", format.format(0.0000000000000000000000000000219227748));
  }

  @Test
  public void test4() {
    AutoDoubleFormat format = new AutoDoubleFormat(4);

    assertEquals("41.30 μ", format.format(0.0000413));
    assertEquals("4.130 m", format.format(0.00413));
    assertEquals("1.000", format.format(1.0));
    assertEquals("3.142", format.format(3.1415));
    assertEquals("1.489", format.format(1.4892));
    assertEquals("14.89 k", format.format(14892.0));
    assertEquals("-41.30 μ", format.format(-0.0000413));
    assertEquals("-4.130 m", format.format(-0.00413));
    assertEquals("-1.000", format.format(-1.0));
    assertEquals("-3.142", format.format(-3.1415));
    assertEquals("-1.489", format.format(-1.4892));
    assertEquals("-14.89 k", format.format(-14892.0));

    assertEquals("1.489 P", format.format(1489246643874893.0));
    assertEquals("1.489 E", format.format(1489246643874993893.0));
    assertEquals("1489 Y", format.format(1489246643878798473944993893.0));
    assertEquals("-1.489 P", format.format(-1489246643874893.0));
    assertEquals("-1.489 E", format.format(-1489246643874993893.0));
    assertEquals("-14.89 Y", format.format(-14892466438787984739449393.0));
    assertEquals("-148.9 Y", format.format(-148924664387879847394499393.0));
    assertEquals("-1489 Y", format.format(-1489246643878798473944993893.0));
    assertEquals("-15489 Y", format.format(-15489246643878798473944993893.0));

    assertEquals("2.277 p", format.format(0.00000000000227748));
    assertEquals("922.8 a", format.format(0.0000000000000009227748));
    assertEquals("19.23 z", format.format(0.000000000000000000019227748));
    assertEquals("0.02192 y", format.format(0.0000000000000000000000000219227748));
    assertEquals("0.002192 y", format.format(0.00000000000000000000000000219227748));
    assertEquals("0.0002192 y", format.format(0.000000000000000000000000000219227748));
    assertEquals("0.00002192 y", format.format(0.0000000000000000000000000000219227748));
  }
}
