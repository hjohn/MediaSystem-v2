package hs.mediasystem.framework.expose;

public abstract class AbstractExposedControl<P> implements ExposedControl<P> {
  protected Type type;
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

  @Override
  public Type getType() {
    return type;
  }
}

