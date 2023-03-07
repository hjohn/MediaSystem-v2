package hs.mediasystem.presentation;

public interface Theme {
  <P extends ParentPresentation, C extends Presentation> Placer<P, C> findPlacer(P parentPresentation, C childPresentation);

  /**
   * Attempts to make the given descendant {@link Presentation} a direct or indirect child of the given ancestor
   * {@link ParentPresentation}. If necessary, additional {@code ParentPresentation}s are constructed to bridge
   * the gap between the given ancestor and descendant {@code Presentation}s. If the descendant can be a
   * descendant of the given ancestor, returns {@code true} otherwise {@code false}.
   *
   * @param ancestor a potential ancestor for the descendant {@code Presentation}, cannot be {@code null}
   * @param descendant a potential descendant for the ancestor {@code ParentPresentation}, cannot be {@code null}
   * @return {@code true} if the given descendant {@code Presentation} was made a descendant of the given
   *   ancestor {@code ParentPresentation}, otherwise {@code false}
   */
  boolean nestPresentation(ParentPresentation ancestor, Presentation descendant);
}
