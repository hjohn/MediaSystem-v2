package hs.mediasystem.presentation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
@Qualifier
public @interface PlacerQualifier {
  Class<? extends NodeFactory<? extends ParentPresentation>> parent();
  Class<? extends NodeFactory<? extends Presentation>> child();
}
