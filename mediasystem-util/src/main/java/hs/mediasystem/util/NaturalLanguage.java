package hs.mediasystem.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

public class NaturalLanguage {
  private static final Collator COLLATOR;

  static {
    COLLATOR = Collator.getInstance(Locale.US);
    COLLATOR.setStrength(Collator.PRIMARY);
  }

  public static final Comparator<String> ALPHABETICAL = (a, b) -> {
    int result = COLLATOR.compare(stripArticle(a), stripArticle(b));

    if(result == 0) {
      result = COLLATOR.compare(getArticle(a), getArticle(b));
    }

    return result;
  };

  private static final Set<String> ARTICLES = Set.of("THE", "A", "AN"); //, "LE", "LA", "LES", "DE", "HET", "DIE", "DAS", "DER");  need to know title language... die = dying

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
