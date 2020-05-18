package hs.mediasystem.runner.root;

import hs.mediasystem.runner.ConfigurationProvider;
import hs.mediasystem.runner.root.ParentalControlsProvider.ParentalControls;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public class ParentalControlsProvider extends ConfigurationProvider<ParentalControls> {
  private static final ParentalControls DEFAULT = new ParentalControls(null, null, List.of());

  public ParentalControlsProvider() {
    super(ParentalControls.class, "parental-controls", DEFAULT);
  }

  public static class ParentalControls {
    public final String passcode;
    public final int timeout;
    public final List<String> hidden;

    public ParentalControls(String passcode, Integer timeout, List<String> hidden) {
      this.passcode = passcode;
      this.timeout = timeout == null ? 900 : timeout;
      this.hidden = List.copyOf(hidden);
    }
  }
}
