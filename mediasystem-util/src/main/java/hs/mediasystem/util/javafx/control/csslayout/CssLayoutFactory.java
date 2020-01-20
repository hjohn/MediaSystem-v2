package hs.mediasystem.util.javafx.control.csslayout;

import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.parser.CssStyle;
import hs.mediasystem.util.parser.Cursor;
import hs.mediasystem.util.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Creates new nodes for CSS based layout.<p>
 *
 * Accepted format: <code>([container-type]:)[style-name] ({ [key=value] (...) )(, ...) }</code><p>
 *
 * Where:<ul>
 *   <li>{@code container-type} is optionally one of {@code HBox, VBox, Stack}</li>
 *   <li>{@code style-name} is a Style class of a child control to place at the position it appears</li>
 *   <li>{@code key} and {@code value} are key value pairs with extra options.
 *   </ul>
 *
 * Support options are:<ul>
 *
 *   <li>{@code align} for aligning a child in a {@link StackPane}, see {@link Pos} for allowed values</li>
 *   <li>{@code hgrow} and {@code vgrow}, see {@link Priority} for allowed values</li>
 *   </ul>
 *
 * Examples:<ul>
 * <li><code>title, VBox:subtitle-block { hgrow=ALWAYS}, VBox:side-box</code></li>
 * </ul>
 */
public class CssLayoutFactory {
  private static final Parser PARSER = new Parser(CssStyle.class);
  private static final String POTENTIALS = "css-layout-factory.potential-children";

  public static List<Node> createNewChildren(String v, Pane parent) {
    List<Node> newChildren = new ArrayList<>();
    List<Node> children = new ArrayList<>(parent.getChildren());
    List<ChildDefinition> childDefinitions = createChildDefinitions(new Cursor(PARSER.parse(v)));

    @SuppressWarnings("unchecked")
    List<Node> potentialChildren = (List<Node>)parent.getProperties().getOrDefault(POTENTIALS, List.of());

    children.addAll(potentialChildren);

    for(ChildDefinition childDef : childDefinitions) {
      Node node;

      if(childDef.getType() == null) {
        node = children.stream().filter(c -> c.getStyleClass().stream().anyMatch(sc -> sc.equals(childDef.getName()))).findFirst()
          .orElseGet(() -> Labels.create("", "Child '" + childDef.getName() + "' not found"));
      }
      else {
        if(childDef.getType().equals("HBox")) {
          node = new StylableHBox();
        }
        else if(childDef.getType().equals("VBox")) {
          node = new StylableVBox();
        }
        else if(childDef.getType().equals("Stack")) {
          node = new StylableStackPane();
        }
        else {
          node = Labels.create("", "Unknown container type: " + childDef.getType());
        }

        node.getProperties().put(POTENTIALS, children);
      }

      String hgrow = childDef.getOptions().get("hgrow");

      if(hgrow != null) {
        HBox.setHgrow(node, Priority.valueOf(hgrow));
      }

      String vgrow = childDef.getOptions().get("vgrow");

      if(vgrow != null) {
        VBox.setVgrow(node, Priority.valueOf(hgrow));
      }

      String align = childDef.getOptions().get("align");

      if(align != null) {
        StackPane.setAlignment(node, Pos.valueOf(align));
      }

      node.getStyleClass().add(childDef.getName());
      newChildren.add(node);
    }

    return newChildren;
  }

  private static List<ChildDefinition> createChildDefinitions(Cursor cursor) {
    List<ChildDefinition> definitions = new ArrayList<>();

    while(!cursor.current().isEnd()) {
      String type = null;
      String name = null;

      if(cursor.current().getType() == CssStyle.IDENTIFIER) {
        if(cursor.next().matches(CssStyle.OPERATOR, ":")) {
          type = cursor.getAs(CssStyle.IDENTIFIER);
          name = cursor.advance(2).getAs(CssStyle.IDENTIFIER);
        }
        else if(cursor.next().getType() == CssStyle.OPERATOR || cursor.next().isEnd()) {
          name = cursor.getAs(CssStyle.IDENTIFIER);
        }

        Map<String, String> parameters;

        cursor.advance().expectTypeOrEnd(CssStyle.OPERATOR, ",", "{");

        if(cursor.current().getType() == CssStyle.OPERATOR && cursor.current().getText().equals("{")) {
          cursor.advance();
          parameters = toParameters(cursor);
          cursor.expect(CssStyle.OPERATOR, "}");
          cursor.advance();
        }
        else {
          parameters = Map.of();
        }

        definitions.add(new ChildDefinition(type, name, parameters));

        if(cursor.current().getType() == CssStyle.OPERATOR && cursor.current().getText().equals(",")) {
          cursor.advance();
        }
      }
      else {
        throw new IllegalArgumentException("Unexpected " + cursor.current().getText());
      }
    }

    return definitions;
  }

  private static Map<String, String> toParameters(Cursor cursor) {
    Map<String, String> parameters = new HashMap<>();

    while(!cursor.current().isEnd() && !cursor.current().matches(CssStyle.OPERATOR, "}")) {
      String name = cursor.getAs(CssStyle.IDENTIFIER);
      String value = null;

      cursor.advance();

      if(cursor.current().matches(CssStyle.OPERATOR, "=")) {
        value = cursor.advance().getAs(CssStyle.IDENTIFIER);
      }

      parameters.put(name, value);

      cursor.advance();
    }

    return parameters;
  }

  private static class ChildDefinition {
    private final String type;
    private final String name;
    private final Map<String, String> options;

    public ChildDefinition(String type, String name, Map<String, String> options) {
      this.type = type;
      this.name = name;
      this.options = options;
    }

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public Map<String, String> getOptions() {
      return options;
    }
  }
}
