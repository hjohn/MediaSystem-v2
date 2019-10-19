package hs.mediasystem.util.expose;

public abstract class AbstractExposedControl<P> implements ExposedControl<P> {
  protected String name;
  protected Class<? super P> cls;

  @Override
  public String getName() {
    return name;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<P> getDeclaringClass() {
    return (Class<P>)cls;
  }
}

