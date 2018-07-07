package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.runner.LessLoader;
import hs.mediasystem.util.javafx.Events;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

public class GenericTreeListPane<T> extends BorderPane {
  private static final KeyCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);

  public final ObservableList<T> mediaNodes = FXCollections.observableArrayList();
  public final ObjectProperty<T> focusedMediaNode = new SimpleObjectProperty<>();
  public final ObjectProperty<EventHandler<ItemSelectedEvent<T>>> onNodeSelected = new SimpleObjectProperty<>();

  public final ListView<T> listView = new ListView<>();

  private final InvalidationListener invalidateTreeListener = new InvalidationListener() {
    @Override
    public void invalidated(Observable observable) {
      if(treeValid) {
        treeValid = false;
        requestLayout();
      }
    }
  };

  private final ChangeListener<T> updateFocusedMediaNode = new ChangeListener<T>() {
    @Override
    public void changed(ObservableValue<? extends T> observable, T old, T current) {
      focusedMediaNode.set(current);
    }
  };

  private boolean treeValid = true;

  public GenericTreeListPane() {
    focusedMediaNode.addListener(new ChangeListener<T>() {
      @Override
      public void changed(ObservableValue<? extends T> observable, T old, T current) {
        setSelectedNode(current);
      }
    });

    getStylesheets().add(LessLoader.compile(getClass().getResource("styles.css")).toExternalForm());

    listView.getStyleClass().add("main-list");
    listView.setEditable(false);

    listView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        T focusedItem = listView.getFocusModel().getFocusedItem();

        if(focusedItem != null) {
          if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            itemSelected(event, focusedItem);
          }
        }
      }
    });

    listView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        T focusedItem = listView.getFocusModel().getFocusedItem();

        if(focusedItem != null) {
          if(ENTER.match(event)) {
            itemSelected(event, focusedItem);
            event.consume();
          }
        }
      }
    });

    mediaNodes.addListener(invalidateTreeListener);

    mediaNodes.addListener((Change<? extends T> c) -> {
      if(!listView.getItems().isEmpty() && listView.getFocusModel().getFocusedIndex() < 0) {
        System.out.println(">>>>>>>>>>>>>> SETTING FOCUS!");
        listView.getFocusModel().focus(0);
      }
    });
    listView.setItems(mediaNodes);
    listView.focusedProperty().addListener(o -> System.out.println("Focused Changed: " + listView.isFocused() + " ; " + listView.getFocusModel().getFocusedIndex()));
    listView.getFocusModel().focusedIndexProperty().addListener(o -> System.out.println("Focused Index Changed: " + listView.getFocusModel().getFocusedIndex()));
    listView.getFocusModel().focusedItemProperty().addListener(updateFocusedMediaNode);

    setCenter(listView);
  }

  @Override
  protected double computePrefWidth(double height) {
    if(!treeValid) {
      buildTree();
      treeValid = true;
    }

    return super.computePrefWidth(height);
  }

  private void buildTree() {
    listView.getFocusModel().focusedItemProperty().removeListener(updateFocusedMediaNode);  // prevent focus updates from changing tree root

//    listView.setCellFactory(new Callback<TreeView<MediaNode>, TreeCell<MediaNode>>() {
//      @Override
//      public TreeCell<MediaNode> call(TreeView<MediaNode> param) {
//        return new MediaItemTreeCell();
//      }
//    });

    listView.getFocusModel().focusedItemProperty().addListener(updateFocusedMediaNode);

    setSelectedNode(focusedMediaNode.get());

    // applyCss();
  }

  @Override
  public void requestFocus() {
    listView.requestFocus();

    Skin<?> lvs = listView.getSkin();

    if(listView.getScene() == null) {
      System.err.println("Request Focus called too early, not part of Scene");
      return;
    }
    if(lvs == null) {
      System.err.println("Request Focus called too early, no Skin");
      return;
    }
/*
    try {
      Field behaviorField = Class.forName("javafx.scene.control.skin.ListViewSkin").getDeclaredField("behavior");

      behaviorField.setAccessible(true);

      Object listViewBehavior = behaviorField.get(lvs);

      Field field = Class.forName("com.sun.javafx.scene.control.behavior.ListViewBehavior").getDeclaredField("tlFocus");

      field.setAccessible(true);

      Object tlflb = field.get(listViewBehavior);

      Class.forName("com.sun.javafx.scene.control.behavior.TwoLevelFocusListBehavior").getMethod("setExternalFocus", boolean.class).invoke(tlflb, false);
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }
*/
  }

  private void itemSelected(Event event, T focusedItem) {
    Events.dispatchEvent(onNodeSelected, new ItemSelectedEvent(event.getTarget(), focusedItem), event);
  }

  private void setSelectedNode(T selectedMediaNode) {
    T focusedTreeItem = listView.getFocusModel().getFocusedItem();

    if(selectedMediaNode == null || (focusedTreeItem != null && selectedMediaNode.equals(focusedTreeItem))) {
      return;
    }

    int index = listView.getItems().indexOf(selectedMediaNode);

    listView.getFocusModel().focus(index);

    Platform.runLater(() -> listView.scrollTo(index));
  }
}
