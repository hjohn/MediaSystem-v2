package hs.mediasystem.db.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.int4.dirk.api.Injector;
import org.int4.dirk.jsr330.Injectors;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

/**
 * A JUnit 5 extension that sets up an auto-discovering {@link Injector}.
 */
public class InjectorExtension implements TestInstanceFactory {

  @Override
  public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) throws TestInstantiationException {
    Injector injector = Injectors.autoDiscovering();

    try {
      if(factoryContext.getOuterInstance().isPresent()) {
        Object outer = factoryContext.getOuterInstance().get();

        Constructor<?>[] constructors = factoryContext.getTestClass().getDeclaredConstructors();

        if(constructors.length != 1 || constructors[0].getParameterCount() != 1) {
          throw new IllegalArgumentException("Must have exactly one declared constructor without parameters: " + Arrays.toString(constructors));
        }

        constructors[0].setAccessible(true);

        return constructors[0].newInstance(outer);
      }

      injector.register(factoryContext.getTestClass());

      return injector.getInstance(factoryContext.getTestClass());
    }
    catch(Exception e) {
      throw new TestInstantiationException("Failed to instantiate test: " + factoryContext.getTestClass(), e);
    }
  }
}

