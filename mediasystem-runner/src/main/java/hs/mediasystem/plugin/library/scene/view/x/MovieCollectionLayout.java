package hs.mediasystem.plugin.library.scene.view.x;

import hs.mediasystem.plugin.library.scene.EntityPresentation;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import javax.inject.Singleton;

@Singleton
public class MovieCollectionLayout extends BaseLayout {

  public MovieCollectionLayout() {
    super(MovieCollectionLocation.class);
  }


  public Fragment create() {
    Node node = new StackPane();

    EntityPresentation entityPresentation = new EntityPresentation();

    return new Fragment(node, entityPresentation);
  }

}
