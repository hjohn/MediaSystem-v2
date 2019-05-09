package hs.mediasystem.presentation;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class ViewPort extends TransitionPane {
  private final ParentPresentation parentPresentation;
  private final Consumer<Node> nodeAdjuster;

  public ViewPort(Theme theme, ParentPresentation parentPresentation, Consumer<Node> nodeAdjuster) {
    super(new TransitionPane.FadeIn(), null);

    this.parentPresentation = parentPresentation;
    this.nodeAdjuster = nodeAdjuster;

    ChangeListener<? super Presentation> listener = (obs, old, current) -> updateChildNode(theme, current);

    parentPresentation.childPresentation.addListener(listener);

    updateChildNode(theme, parentPresentation.childPresentation.get());
  }

  protected Node updateChildNode(Theme theme, Presentation current) {
    if(current == null) {
      return null;
    }

    /*
     * In order to prevent binding short-lived Nodes to a long-lived
     * Presentation, a copy is made of the presentation and bound
     * bidirectionally to its copy.  The copy is then passed to the
     * NodeFactory.
     *
     * When the Node is discarded, the presentation copy is unbound
     * again, and the Node and the presentation copy will be free
     * to be gc'd.
     */

    //Presentation presentationCopy = theme.createPresentation(current.getClass());

    //bindBidirectional(presentationCopy, current);

//    Node node = theme.findPlacer(parentPresentation, presentationCopy).place(parentPresentation, presentationCopy);
    Node node = theme.findPlacer(parentPresentation, current).place(parentPresentation, current);

    //node.getProperties().put("presentation1", presentationCopy);
    node.getProperties().put("presentation2", current);

    add((Pane)node);
//    getChildren().setAll(node);

    if(nodeAdjuster != null) {
      nodeAdjuster.accept(node);
    }

    return node;
  }

  @SuppressWarnings("unchecked")
  private static <C extends Presentation> void bindBidirectional(C presentation, C presentationCopy) {
    if(!presentation.getClass().equals(presentationCopy.getClass())) {
      throw new IllegalArgumentException("presentation must be of same class: " + presentation.getClass() + " != " + presentationCopy.getClass());
    }

    try {
      for(Field field : presentation.getClass().getFields()) {
        Class<?> type = field.getType();

        if(Property.class.isAssignableFrom(type)) {
          ((Property<Object>)field.get(presentation)).bindBidirectional((Property<Object>)field.get(presentationCopy));
        }
        else if(ObservableList.class.isAssignableFrom(type)) {
          Bindings.bindContentBidirectional((ObservableList<Object>)field.get(presentation), (ObservableList<Object>)field.get(presentationCopy));
        }
        else {
          throw new IllegalStateException("Encountered unknown type in " + presentation.getClass() + " while binding: " + field);
        }
      }
    }
    catch(IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <C extends Presentation> void unbindBidirectional(C presentation, C presentationCopy) {
    if(!presentation.getClass().equals(presentationCopy.getClass())) {
      throw new IllegalArgumentException("presentation must be of same class: " + presentation.getClass() + " != " + presentationCopy.getClass());
    }

    try {
      for(Field field : presentation.getClass().getFields()) {
        Class<?> type = field.getType();

        if(Property.class.isAssignableFrom(type)) {
          ((Property<Object>)field.get(presentation)).unbindBidirectional((Property<Object>)field.get(presentationCopy));
        }
        else if(ObservableList.class.isAssignableFrom(type)) {
          Bindings.unbindContentBidirectional(field.get(presentation), field.get(presentationCopy));
        }
        else {
          throw new IllegalStateException("Encountered unknown type in " + presentation.getClass() + " while unbinding: " + field);
        }
      }
    }
    catch(IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
