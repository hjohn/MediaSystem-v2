package hs.mediasystem.util;

import java.util.Comparator;
import java.util.Set;

public class NaturalLanguage {
  public static final Comparator<String> ALPHABETICAL = (a, b) -> {
    int result = stripArticle(a).compareTo(stripArticle(b));

    if(result == 0) {
      result = getArticle(a).compareTo(getArticle(b));
    }

    return result;
  };

  private static final Set<String> ARTICLES = Set.of("THE", "A", "AN"); //, "LE", "LA", "LES", "DE", "DIE", "DAS", "DER");  need to know title language... die = dying

  public static boolean isArticle(String text) {
    return ARTICLES.contains(text.toUpperCase());
  }

  public static String stripArticle(String text) {
    int space = text.indexOf(' ');

    if(space == -1) {
      return text;
    }

    return isArticle(text.substring(0, space)) ? text.substring(space + 1) : text;
  }

  public static String getArticle(String text) {
    int space = text.indexOf(' ');

    if(space == -1) {
      return "";
    }

    String potentialArticle = text.substring(0, space);

    return isArticle(potentialArticle) ? potentialArticle : "";
  }
}
