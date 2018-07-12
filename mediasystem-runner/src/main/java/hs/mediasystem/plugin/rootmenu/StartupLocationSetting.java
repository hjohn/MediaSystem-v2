package hs.mediasystem.plugin.rootmenu;

import java.util.function.Supplier;

import javax.inject.Named;
import javax.inject.Singleton;

@Named("primary")
@Singleton
public class StartupLocationSetting implements Supplier<Object> {

  @Override
  public Object get() {
    return new MenuPresentation();  // TODO needs to be configurable
  }
}
