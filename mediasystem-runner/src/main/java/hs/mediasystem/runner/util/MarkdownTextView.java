package hs.mediasystem.runner.util;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Caret.CaretVisibility;
import org.fxmisc.richtext.StyleClassedTextArea;

public class MarkdownTextView extends VirtualizedScrollPane<StyleClassedTextArea> {
  private static final String STYLES_URL = LessLoader.compile(MarkdownTextView.class, "markdown-styles.less");

  public final StringProperty markdownText = new SimpleStringProperty();

  private final StyleClassedTextArea textArea;

  public MarkdownTextView() {
    super(new StyleClassedTextArea(false));

    this.textArea = getContent();
    this.textArea.setWrapText(true);
    this.textArea.setFocusTraversable(false);
    this.textArea.setEditable(false);
    this.textArea.setShowCaret(CaretVisibility.OFF);

    getStylesheets().add(STYLES_URL);

    setFocusTraversable(true);

    addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if(e.getCode().isNavigationKey()) {
        if(KeyCode.UP == e.getCode()) {
          scrollBy(0, -50);
          e.consume();
        }
        else if(KeyCode.DOWN == e.getCode()) {
          scrollBy(0, 50);
          e.consume();
        }
      }
    });

    markdownText.addListener((obs, old, current) -> {
      Parser parser = Parser.builder().build();
      Node document = parser.parse(current);

      MarkdownCssVisitor visitor = new MarkdownCssVisitor();

      document.accept(visitor);

      this.textArea.clear();  // hopefully clears all styles as well
      this.textArea.replaceText(visitor.plainText.toString());
      this.textArea.showParagraphAtTop(0);

      for(Style style : visitor.styles) {
        this.textArea.setStyleClass(style.start, style.end, style.name);
      }
    });
  }

  private static class Style {
    final String name;
    final int start;
    final int end;

    Style(String name, int start, int end) {
      this.name = name;
      this.start = start;
      this.end = end;
    }
  }

  private static class MarkdownCssVisitor extends AbstractVisitor {
    private final StringBuilder plainText = new StringBuilder();
    private final List<Style> styles = new ArrayList<>();
    private final List<String> bulletMarkerStack = new ArrayList<>();

    @Override
    public void visit(Heading heading) {
      int start = plainText.length();

      visitChildren(heading);

      styles.add(new Style("heading" + heading.getLevel(), start, plainText.length()));
      plainText.append("\n");
    }

    @Override
    public void visit(Paragraph paragraph) {
      visitChildren(paragraph);

      if(paragraph.getParent() == null || paragraph.getParent() instanceof Document) {
        plainText.append("\n\n");
      }
    }

    @Override
    public void visit(Emphasis emphasis) {
      int start = plainText.length();

      visitChildren(emphasis);

      styles.add(new Style("emphasis", start, plainText.length()));
    }

    @Override
    public void visit(StrongEmphasis strongEmphasis) {
      int start = plainText.length();

      visitChildren(strongEmphasis);

      styles.add(new Style("strong", start, plainText.length()));
    }

    @Override
    public void visit(Code code) {
      int start = plainText.length();

      plainText.append(code.getLiteral());

      styles.add(new Style("code", start, plainText.length()));
    }

    @Override
    public void visit(FencedCodeBlock fencedCodeBlock) {
      int start = plainText.length();

      plainText.append(fencedCodeBlock.getLiteral());

      styles.add(new Style("code", start, plainText.length()));
    }

    @Override
    public void visit(IndentedCodeBlock indentedCodeBlock) {
      int start = plainText.length();

      plainText.append(indentedCodeBlock.getLiteral());

      styles.add(new Style("code", start, plainText.length()));
    }

    @Override
    public void visit(BulletList bulletList) {
      bulletMarkerStack.add(" • ");  // same for "-", "+" and "*" for now

      visitChildren(bulletList);

      bulletMarkerStack.remove(bulletMarkerStack.size() - 1);

      plainText.append("\n");
    }

    @Override
    public void visit(ListItem listItem) {
      plainText.append(bulletMarkerStack.get(bulletMarkerStack.size() - 1));

      visitChildren(listItem);

      plainText.append("\n");
    }

    @Override
    public void visit(Text text) {
      plainText.append(text.getLiteral());
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
      plainText.append(" ");
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
      plainText.append("\n");
    }
  }
}
