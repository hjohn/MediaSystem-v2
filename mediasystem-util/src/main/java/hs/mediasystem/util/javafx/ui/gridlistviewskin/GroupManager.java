package hs.mediasystem.util.javafx.ui.gridlistviewskin;

import java.util.Arrays;

public class GroupManager {
  private final int[] shifts;
  private final int[] modelPositions;
  private final int[] viewPositions;
  private final int width;
  private final int size;

  public GroupManager(int[] modelPositions, int width) {
    this.size = modelPositions.length;
    this.width = width;
    this.modelPositions = modelPositions;
    this.shifts = new int[size];
    this.viewPositions = new int[size];

    int shift = 0;

    for(int i = 0; i < size; i++) {
      int pos = modelPositions[i];

      shift += (width - (pos + shift) % width) % width;

      shifts[i] = shift;
      viewPositions[i] = pos + shift;
    }
  }

  public int getWidth() {
    return width;
  }

  public int countViewItems(int modelIndex, int rowCount) {
    int viewItemCount = rowCount * width;
    int shift = 0;

    for(int groupIndex : modelPositions) {
      if(groupIndex > modelIndex && groupIndex < modelIndex + viewItemCount) {
        int i = (width - (groupIndex - modelIndex + shift) % width) % width;

        shift += i;
        viewItemCount -= i;
      }
    }

    return viewItemCount;
  }

  /**
   * Returns the index in terms of the model of the group with the given
   * number (starting from 0).
   *
   * @param groupNumber a group number
   * @return a model index
   */
  public int modelIndexOfGroup(int groupNumber) {
    return modelPositions[groupNumber];
  }

  /**
   * Returns the index in terms of the view of the group with the given
   * number (starting from 0).
   *
   * @param groupNumber a group number
   * @return a view index
   */
  public int viewIndexOfGroup(int groupNumber) {
    return viewPositions[groupNumber];
  }

  /**
   * Returns the group number which the given model index is part of.
   *
   * @param modelIndex a model index
   * @return the group number which the given model index is part of
   */
  public int groupNumber(int modelIndex) {
    int index = Arrays.binarySearch(modelPositions, modelIndex);

    if(index < 0) {
      index = -index - 2;
    }

    return index;
  }

  /**
   * Shifts the index in the model to a view index which can contain gaps
   * when items are to appear grouped.
   *
   * @param modelIndex a model index
   * @return a visible index
   */
  public int toViewIndex(int modelIndex) {
    int index = groupNumber(modelIndex);

    return modelIndex + (index < 0 ? 0 : shifts[index]);
  }

  private int findToModelShift(int viewIndex) {
    int index = getGroupNumberByViewIndex(viewIndex);

    return index < 0 ? 0 : -shifts[index];
  }

  /**
   * Converts a view index to a model index.  If the view index refers to a gap, then there
   * is no corresponding model index and -1 is returned.
   *
   * @param viewIndex a view index
   * @return a model index, or -1 if there was no corresponding model index
   */
  public int toModelIndex(int viewIndex) {
    if(!isValidViewIndex(viewIndex)) {
      return -1;
    }

    return viewIndex + findToModelShift(viewIndex);
  }

  public int toModelIndexSmart(int viewIndex) {
    while(!isValidViewIndex(viewIndex)) {
      viewIndex = viewIndexOfLastItemInViewRow(viewIndex / width);
    }

    return viewIndex + findToModelShift(viewIndex);
  }

  public int modelIndexOfViewRow(int viewRow) {
    int viewIndex = viewRow * width;

    return viewIndex + findToModelShift(viewIndex);
  }

  /**
   * Returns the number of the row in the view in which the given model index
   * is part of.
   *
   * @param modelIndex a model index
   * @return a view row number
   */
  public int getViewRowNumber(int modelIndex) {
    return toViewIndex(modelIndex) / width;
  }

  private int getGroupNumberByViewIndex(int viewIndex) {
    int index = Arrays.binarySearch(viewPositions, viewIndex);

    if(index < 0) {
      index = -index - 2;
    }

    return index;
  }

  public boolean isValidViewIndex(int viewIndex) {
    return viewIndexOfLastItemInViewRow(viewIndex / width) >= viewIndex;
  }

  public int viewIndexOfLastItemInViewRow(int viewRow) {
    int viewIndex = viewRow * width + width - 1;
    int index = getGroupNumberByViewIndex(viewIndex) + 1;

    if(index > 0 && index < size) {
      int nextGroupIndex = modelPositions[index];
      int viewNextGroupIndex = viewPositions[index];
      int modelShift = findToModelShift(viewPositions[index - 1]);

      if(viewIndex < viewNextGroupIndex && viewIndex >= nextGroupIndex - modelShift) {
        return nextGroupIndex - modelShift - 1;
      }
    }

    return viewIndex;
  }
}
