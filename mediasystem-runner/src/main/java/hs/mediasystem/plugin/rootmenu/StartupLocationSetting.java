package hs.mediasystem.plugin.rootmenu;

import hs.mediasystem.presentation.Presentation;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named("primary")
@Singleton
public class StartupLocationSetting implements Supplier<Presentation> {
  @Inject private MenuPresentation.Factory factory;

  @Override
  public Presentation get() {
    return factory.create();
  }
}
