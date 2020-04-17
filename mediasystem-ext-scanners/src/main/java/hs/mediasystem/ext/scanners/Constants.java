package hs.mediasystem.ext.scanners;

import java.util.regex.Pattern;

public class Constants {
  public static final Pattern VIDEOS = Pattern.compile("(?i).+\\.(avi|flv|mkv|mov|mp4|mpg|mpeg|ogm)");
}
