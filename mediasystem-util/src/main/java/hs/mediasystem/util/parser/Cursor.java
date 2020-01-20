package hs.mediasystem.util.parser;

import java.util.Arrays;
import java.util.List;

public class Cursor {
  private final Token endToken;

  private final List<Token> tokens;

  private int position;

  public Cursor(List<Token> tokens) {
    this.tokens = tokens;

    Token lastToken = tokens.get(tokens.size() - 1);

    endToken = new Token(null, null, "", lastToken.getEndOffset());
  }

  public Token current() {
    return tokens.size() > position ? tokens.get(position) : endToken;
  }

  public Cursor advance() {
    position++;

    return this;
  }

  public Cursor advance(int amount) {
    position += amount;

    return this;
  }

  public Token next() {
    return tokens.size() > position + 1 ? tokens.get(position + 1) : endToken;
  }

  public String getAs(Type type) {
    if(current().getType() != type) {
      throw new IllegalArgumentException("Expected " + type + " at " + current().getOffset() + " but found: " + current());
    }

    return current().getText();
  }

  public void expect(Type type, String... validValues) {
    for(String validValue : validValues) {
      if(current().getType() == type && current().getText().equals(validValue)) {
        return;
      }
    }

    throw new IllegalArgumentException("Expected " + Arrays.toString(validValues) + " at " + current().getOffset() + " but found: " + current());
  }

  public void expectTypeOrEnd(Type type, String... validValues) {
    if(current().isEnd()) {
      return;
    }

    expect(type, validValues);
  }
}