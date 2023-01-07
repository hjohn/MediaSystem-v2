package hs.mediasystem.util.javafx.ui.gridlistviewskin;

public class Group {
  private final String title;
  private final int position;

  public Group(String title, int position) {
    this.title = title;
    this.position = position;
  }

  public String getTitle() {
    return title;
  }

  public int getPosition() {
    return position;
  }
}
