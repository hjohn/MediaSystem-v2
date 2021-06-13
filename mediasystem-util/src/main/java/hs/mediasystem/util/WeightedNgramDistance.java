package hs.mediasystem.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WeightedNgramDistance {

  /**
   * Splits each string into all possible subsequences of 1..len characters, prunes
   * the one with spaces in them, and then compares these.  Longer subsequences are
   * given a higher weight.
   *
   * @param s1 string 1
   * @param s2 string 2
   * @return a weight
   */
  public static double calculate(String s1, String s2) {
    List<Set<String>> list1 = ngramList(clean(s1));
    List<Set<String>> list2 = ngramList(clean(s2));
    int len = Math.max(list1.size(), list2.size());
    double commonCount = 0;
    double totalCount = 0;

    for(int i = 0; i < len; i++) {
      double x = i + 1;
      double weight = 1 / x;

      if(i >= list1.size()) {
        totalCount += list2.get(i).size() * weight;
      }
      else if(i >= list2.size()) {
        totalCount += list1.get(i).size() * weight;
      }
      else {
        Set<String> all = list1.get(i);
        Set<String> set2 = list2.get(i);
        Set<String> common = new HashSet<>(all);

        common.retainAll(set2);
        all.addAll(set2);

        commonCount += common.size() * weight;
        totalCount += all.size() * weight;
      }
    }

    return commonCount / totalCount;
  }

  private static List<Set<String>> ngramList(String s) {
    List<Set<String>> ngramList = new ArrayList<>();

    for(int len = 1; len <= s.length(); len++) {
      Set<String> ngrams = new HashSet<>();

      // 'len' length sequences (without spaces)
      for(int j = 0; j <= s.length() - len; j++) {
        String ngram = s.substring(j, j + len);

        ngrams.add(ngram);
      }


      ngramList.add(ngrams);
    }

    return ngramList;
  }

  private static String clean(String s) {
    s = s.toLowerCase();
    s = s.replaceAll("\\.", "");
    s = s.replaceAll("[^a-z0-9&]", " ");
    s = s.replaceAll(" +", " ");

    return s;
  }
}
