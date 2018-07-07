package hs.mediasystem.ext.tmdb;

public class Genres {
  public static String toString(int id) {
    switch(id) {
      case 12: return "Adventure";
      case 14: return "Fantasy";
      case 16: return "Animation";
      case 18: return "Drama";
      case 27: return "Horror";
      case 28: return "Action";
      case 35: return "Comedy";
      case 36: return "History";
      case 37: return "Western";
      case 53: return "Thriller";
      case 80: return "Crime";
      case 99: return "Documentary";
      case 878: return "Science Fiction";
      case 9648: return "Mystery";
      case 10402: return "Music";
      case 10749: return "Romance";
      case 10751: return "Family";
      case 10752: return "War";
      case 10759: return "Action & Adventure";
      case 10762: return "Kids";
      case 10763: return "News";
      case 10764: return "Reality";
      case 10765: return "Sci-Fi & Fantasy";
      case 10766: return "Soap";
      case 10767: return "Talk";
      case 10768: return "War & Politics";
      case 10770: return "TV Movie";
      default: return "Unknown Genre " + id;
    }
  }
}
