package hs.mediasystem.plugin.home;

import java.util.ArrayList;
import java.util.List;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.fxmisc.richtext.StyleClassedTextArea;

public class MarkdownTextArea extends StyleClassedTextArea {

  public MarkdownTextArea(String markdownText) {
    Parser parser = Parser.builder().build();
    Node document = parser.parse(markdownText);

    MarkdownCssVisitor visitor = new MarkdownCssVisitor();
    document.accept(visitor);

    this.setWrapText(true);
    this.setFocusTraversable(false);
    this.replaceText(visitor.plainText.toString());
    this.setEditable(false);

    for(Style style : visitor.styles) {
      this.setStyleClass(style.start, style.end, style.name);
    }
  }

  private static class Style {
    private String name;
    private int start;
    private int end;

    public Style(String name, int start, int end) {
      this.name = name;
      this.start = start;
      this.end = end;
    }
  }

  private static class MarkdownCssVisitor extends AbstractVisitor {
    private StringBuilder plainText = new StringBuilder();
    private List<Style> styles = new ArrayList<>();

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

      plainText.append("\n\n");
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
