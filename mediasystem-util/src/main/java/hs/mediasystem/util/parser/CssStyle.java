package hs.mediasystem.util.parser;

import java.util.regex.Pattern;

public enum CssStyle implements Type {
  IDENTIFIER("[_A-Za-z](?:[_A-Za-z0-9-]*[_A-Za-z0-9])?"),  // Identifiers consist of letters, digits, underscores and hyphens, but cannot start with a digit or hypen and cannot end with a hyphen
  NUMBER("-?[0-9]+(?:\\\\.[0-9]+)?"),
  STRING("\"(?:\\\\.|[^\\\\\"])*\""),
  OPERATOR("\\(|\\)|\\{|\\}|[^A-Za-z0-9 ]+");

  private final Pattern pattern;

  CssStyle(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }

  @Override
  public Pattern getPattern() {
    return pattern;
  }
}