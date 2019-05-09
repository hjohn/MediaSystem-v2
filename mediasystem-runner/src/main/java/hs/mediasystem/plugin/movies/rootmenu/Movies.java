//package hs.mediasystem.plugin.movies.rootmenu;
//
//import hs.mediasystem.plugin.library.scene.view.PresentationLoader;
//import hs.mediasystem.plugin.movies.videolibbaroption.MovieCollectionPresentation;
//import hs.mediasystem.plugin.rootmenu.RootMenuScenePlugin;
//import hs.mediasystem.util.FocusEvent;
//import hs.mediasystem.util.javafx.control.Buttons;
//import hs.mediasystem.util.javafx.control.Containers;
//
//import javafx.animation.Animation;
//import javafx.animation.KeyFrame;
//import javafx.animation.KeyValue;
//import javafx.animation.Timeline;
//import javafx.animation.Transition;
//import javafx.event.EventHandler;
//import javafx.geometry.Point2D;
//import javafx.scene.Node;
//import javafx.scene.control.Button;
//import javafx.scene.effect.GaussianBlur;
//import javafx.scene.effect.InnerShadow;
//import javafx.scene.effect.Light;
//import javafx.scene.effect.Lighting;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.HBox;
//import javafx.scene.paint.Color;
//import javafx.scene.transform.Scale;
//import javafx.util.Duration;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
//@Singleton
//public class Movies implements RootMenuScenePlugin.MenuPlugin {
//  @Inject private MovieCollectionPresentation.Factory movieCollectionPresentationFactory;
//
//  @Override
//  public Node getNode() {
//    Button button1 = Buttons.create("Collection", e -> PresentationLoader.navigate(e, () -> movieCollectionPresentationFactory.create()));
//    Button button2 = Buttons.create("Top 100", e -> PresentationLoader.navigate(e, () -> movieCollectionPresentationFactory.create()));
//    Button button3 = Buttons.create("New Releases", e -> PresentationLoader.navigate(e, () -> movieCollectionPresentationFactory.create()));
//
//    Button button = new Button(getText("title"), new ImageView(getImage("image")));
//
//    button.onActionProperty().set(e -> PresentationLoader.navigate(e, () -> movieCollectionPresentationFactory.create()));
//
//    HBox hbox = Containers.hbox();
//
//    hbox.getChildren().addAll(
//      createShadowNode(hbox, button1, Color.RED.deriveColor(0, 1, 1, 0.4)),
//      createShadowNode(hbox, button2, Color.RED.deriveColor(0, 1, 1, 0.4)),
//      createShadowNode(hbox, button3, Color.RED.deriveColor(0, 1, 1, 0.4)),
//      button1, button2, button3
//    );
//
//    hbox.addEventHandler(FocusEvent.FOCUS_GAINED, new EventHandler<FocusEvent>() {
//      @Override
//      public void handle(FocusEvent event) {
//        Node target = ((Node)event.getTarget());
//
//        new Timeline(new KeyFrame(Duration.millis(250),
//          new KeyValue(hbox.translateXProperty(), -target.getLayoutX())
//        )).play();
//      }
//    });
//
//    Transition t = new Transition() {
//      {
//        setCycleDuration(Duration.seconds(8));
//      }
//
//      @Override
//      protected void interpolate(double frac) {
//        Light.Distant light = new Light.Distant();
//
//        if(frac < 0.1) {
//          light.setAzimuth(frac * 360 * 10 + 135);
//        }
//        else {
//          light.setAzimuth(135);
//        }
//
//        Lighting lighting = new Lighting(light);
//
//        lighting.setBumpInput(new InnerShadow(3, Color.WHITE));
//        lighting.setSurfaceScale(3);
//
//        button1.setEffect(lighting);
//      }
//    };
//
//    t.setCycleCount(Animation.INDEFINITE);
//
//    button1.focusedProperty().addListener(o -> {
//      if(button1.isFocused()) {
//        t.play();
//
//        new Timeline(
//          new KeyFrame(Duration.seconds(0),
//            new KeyValue(button1.scaleXProperty(), 1),
//            new KeyValue(button1.scaleYProperty(), 1)
//          ),
//          new KeyFrame(Duration.seconds(1),
//            new KeyValue(button1.scaleXProperty(), 1.5),
//            new KeyValue(button1.scaleYProperty(), 1.5)
//          )
//        ).play();
//
//      }
//      else {
//        t.stop();
//        button1.setEffect(null);
//      }
//    });
//
//   // hbox.setEffect(new DropShadow(5, 50, 50, Color.RED));
//
//    return Containers.vbox(
//      new ImageView(getImage("image")),
//      hbox
//    );
//  }
//
//  private Node createShadowNode(Node hbox, Button button, Color color) {
//    Button buttonCopy = new Button(button.getText());
//
//    buttonCopy.setManaged(false);
//
//    hbox.translateXProperty().addListener(o -> {
//      double toZero = Math.abs(button.getLayoutX() + hbox.getTranslateX()) / 20 + 1;
//      System.out.println("Value of " + (button.getLayoutX() + hbox.getTranslateX()));
//      Point2D p2d = hbox.localToScene(new Point2D(0, 0));
//      double sceneWidth = hbox.getScene().getWidth();
//      double scale = 1.5;
//
//      buttonCopy.setTextFill(Color.WHITE.deriveColor(0, 1, 1, 1));
//      //buttonCopy.relocate(button.getLayoutX() + (button.getLayoutX() + hbox.getTranslateX()) * 1 * scale, button.getLayoutY());
//      buttonCopy.relocate(button.getLayoutX() + (button.getLayoutX() + hbox.getTranslateX()) * (scale - 1), button.getLayoutY());
////      buttonCopy.relocate((button.getLayoutX() + (p2d.getX() - sceneWidth / 2)) * 2, button.getLayoutY());//button1.getLayoutX() + hbox.getTranslateX() * 2, button1.getLayoutY() + hbox.getTranslateY());
//      buttonCopy.resize(button.getWidth(), button.getHeight());
//      buttonCopy.setEffect(new GaussianBlur(3));
//      buttonCopy.getTransforms().clear();
//      buttonCopy.getTransforms().add(new Scale(scale, scale, button.getWidth() / 2, button.getHeight() / 2));
//    });
//
//    buttonCopy.setManaged(false);
//    buttonCopy.setFocusTraversable(false);
//    buttonCopy.setDisable(true);
//    buttonCopy.setMouseTransparent(true);
//
//    return buttonCopy;
//  }
//}
