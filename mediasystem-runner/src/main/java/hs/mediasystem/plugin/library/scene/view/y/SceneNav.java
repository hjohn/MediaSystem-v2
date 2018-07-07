package hs.mediasystem.plugin.library.scene.view.y;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import javax.inject.Inject;

public class SceneNav {
  @Inject private List<Layout<?, ?>> layouts = List.of(new LibraryLayout(), new MovieCollectionLayout());

  private List<Layout<?, ?>> layoutStack = new ArrayList<>();
  private List<View> viewStack = new ArrayList<>();

  public static void main(String[] args) {
    new SceneNav().goSomewhere(null);
  }

  public void goSomewhere(Location location2) {
    Location<?> loc = new MovieCollectionLocation(new LibraryLocation());
    List<Location<?>> locations = new ArrayList<>();

    while(loc != null) {
      locations.add(loc);
      loc = (Location<?>)loc.getParent();
    }

    Collections.reverse(locations);

    BaseView baseView = new BaseView(new StackPane());

    for(int i = 0; i < locations.size(); i++) {
      Location<?> location = locations.get(i);
      Layout<?, ?> layout = layouts.stream().filter(l -> l.getLocationClass().isInstance(location)).findFirst().orElse(null);

      if(layoutStack.size() <= i || layout != layoutStack.get(i)) {
        if(layoutStack.size() > i) {
          layoutStack.subList(i, layoutStack.size()).clear();
          viewStack.subList(i, viewStack.size()).clear();
        }

        layoutStack.add(layout);
        viewStack.add(((Layout<Object, View>)layout).create(i == 0 ? baseView : viewStack.get(i - 1), location));

        // Add to parent here
        // How?
        // - pass in node?
        // - standard way of adding?
        // - parent says where?
        // - parent specifies extension points?

        // --> Return View instead of Node, pass to child -- View has method to clear any children, and methods to add to the appropriate spot
      }
    }


  }

  public class BaseView extends AbstractView {

    public BaseView(Node node) {
      super(node);
    }

    public void place(Node node) {
    }
  }
}
