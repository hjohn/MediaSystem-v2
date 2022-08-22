package hs.mediasystem.db;

import hs.ddif.core.Injector;
import hs.ddif.jsr330.Injectors;

import java.lang.reflect.Constructor;

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

        Constructor<?> constructor = factoryContext.getTestClass().getDeclaredConstructor(outer.getClass());

        constructor.setAccessible(true);

        return constructor.newInstance(outer);
      }

      return injector.getInstance(factoryContext.getTestClass());
    }
    catch(Exception e) {
      throw new TestInstantiationException("Failed to instantiate test: " + factoryContext.getTestClass(), e);
    }
  }
}

