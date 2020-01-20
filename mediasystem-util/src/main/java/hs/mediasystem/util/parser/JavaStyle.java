package hs.mediasystem.util.parser;

import java.util.regex.Pattern;

public enum JavaStyle implements Type {
  IDENTIFIER("[$_A-Za-z][$_A-Za-z0-9]*"),  // Identifiers consist of letters, digits, underscores and dollar signs, but cannot start with a digit
  NUMBER("-?[0-9]+(?:\\\\.[0-9]+)?"),
  STRING("\"(?:\\\\.|[^\\\\\"])*\""),
  OPERATOR("\\(|\\)|\\{|\\}|[^A-Za-z0-9 ]+");

  private final Pattern pattern;

  JavaStyle(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }

  @Override
  public Pattern getPattern() {
    return pattern;
  }
}