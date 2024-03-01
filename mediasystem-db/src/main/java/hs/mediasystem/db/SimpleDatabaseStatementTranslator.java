package hs.mediasystem.db;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SimpleDatabaseStatementTranslator implements DatabaseStatementTranslator {
  private final Map<String, String> translations;

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}");

  public SimpleDatabaseStatementTranslator(Map<String, String> translations) {
    this.translations = translations;
  }

  @Override
  public String translate(String statement) {
    StringBuffer sb = new StringBuffer();
    Matcher matcher = VARIABLE_PATTERN.matcher(statement);

    while(matcher.find()) {
      matcher.appendReplacement(sb, translations.get(matcher.group(1)));
    }

    matcher.appendTail(sb);

    return sb.toString();
  }
}
