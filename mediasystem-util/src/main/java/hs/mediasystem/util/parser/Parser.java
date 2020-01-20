package hs.mediasystem.util.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Parser {
  private final Pattern pattern;
  private final List<Type> types;

  public Parser(Class<? extends Type> knownTypes) {
    types = Arrays.asList(knownTypes.getEnumConstants());

    List<String> parts = types.stream().map(Type::getPattern).map(Pattern::pattern).collect(Collectors.toList());
    String p = "";

    for(int i = 0; i < parts.size(); i++) {
      String part = parts.get(i);

      if(!p.isEmpty()) {
        p += "|";
      }

      p += "(?<g" + i + ">" + part + ")";
    }

    pattern = Pattern.compile(p);
  }

  public List<Token> parse(String expr) {
    Matcher matcher = pattern.matcher(expr);
    int skippedStart = 0;
    List<Token> tokens = new ArrayList<>();

    while(matcher.find()) {
      Type type = null;

      for(int i = 0; i < types.size(); i++) {
        if(matcher.group(i + 1) != null) {
          type = types.get(i);
          break;
        }
      }

      tokens.add(new Token(type, matcher.group(0), expr.substring(skippedStart, matcher.start()), matcher.start()));
      skippedStart = matcher.end();
    }

    return tokens;
  }
}
