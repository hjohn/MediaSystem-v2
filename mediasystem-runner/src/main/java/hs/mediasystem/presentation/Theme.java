package hs.mediasystem.presentation;

import java.util.List;
import java.util.Optional;

import javafx.event.Event;
import javafx.scene.Node;

public interface Theme {

  /**
   * Returns a {@link Node} representing the child presentation which, if necessary,
   * has been adjusted to best fit in with the UI associated with the parent
   * presentation, if provided.
   *
   * @param <P> the type of the parent {@link Presentation}
   * @param <C> the type of the child {@link Presentation}
   * @param parentPresentation a parent {@link Presentation}, can be {@code null}
   * @param childPresentation a child {@link Presentation}, cannot be {@code null}
   * @return a {@link Node} for the adjusted child presentation, never {@code null}
   */
  <P extends ParentPresentation, C extends Presentation> Node place(P parentPresentation, C childPresentation);

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

  /**
   * Given a {@link Presentation} returns all possible targets (in order of priority)
   * to which can be navigated.
   *
   * @param presentation a {@link Presentation}, cannot be {@code null}
   * @return a list of {@link NavigationTarget}s, never {@code null} or contains {@code null}, but can be empty
   */
  List<NavigationTarget> targetsFor(Presentation presentation);

  /**
   * Given a {@link Presentation} returns the single most important target
   * to which can be navigated.
   *
   * @param presentation a {@link Presentation}, cannot be {@code null}
   * @return an optional {@link NavigationTarget}, never {@code null}, but can be empty
   */
  Optional<NavigationTarget> targetFor(Presentation presentation);

  /**
   * A target to which can be navigated, including description.
   */
  interface NavigationTarget {

    /**
     * Returns a label for the target.
     *
     * @return a label for the target, never {@code null}
     */
    String getLabel();

    /**
     * Navigates to the target in response to an event.
     *
     * @param event an {@link Event}, cannot be {@code null}
     */
    void go(Event event);
  }
}
