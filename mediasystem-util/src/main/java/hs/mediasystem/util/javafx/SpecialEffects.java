package hs.mediasystem.util.javafx;

import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;

public class SpecialEffects {
  public static Effect createNeonEffect(final double size) { // font point size
    return new Blend() {{
      setMode(BlendMode.MULTIPLY);
      setBottomInput(new DropShadow() {{
        setColor(Color.rgb(254, 235, 66, 0.3));
        setOffsetX(size / 22);
        setOffsetY(size / 22);
        setSpread(0.2);
      }});
      setTopInput(new Blend() {{
        setMode(BlendMode.MULTIPLY);
        setBottomInput(new DropShadow() {{
          setColor(Color.web("#f13a00"));
          setRadius(size / 5.5);
          setSpread(0.2);
        }});
        setTopInput(new Blend() {{
          setMode(BlendMode.MULTIPLY);
          setBottomInput(new InnerShadow() {{
            setColor(Color.web("#feeb42"));
            setRadius(size / 12);
            setChoke(0.8);
          }});
          setTopInput(new InnerShadow() {{
            setColor(Color.web("#f13a00"));
            setRadius(size / 22);
            setChoke(0.4);
          }});
        }});
      }});
    }};
  }
}
