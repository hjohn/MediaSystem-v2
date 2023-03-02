package hs.mediasystem.util.expose;

public abstract class AbstractExposedControl implements ExposedControl {
  protected String name;
  protected Class<?> cls;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Class<?> getDeclaringClass() {
    return cls;
  }
}

