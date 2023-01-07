package hs.mediasystem.util.javafx.ui.gridlistviewskin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupManagerTest {
  private final GroupManager manager3 = new GroupManager(new int[] {0, 7, 9, 13, 14}, 3);
  private final GroupManager manager4 = new GroupManager(new int[] {0, 7, 9, 13, 14}, 4);

  @Test
  public void shouldReturnCorrectGroupIndex() {
    GroupManager gm = new GroupManager(new int[] {0}, 3);

    for(int i = 0; i < 100; i++) {
      assertEquals(0, gm.groupNumber(i), "testing " + i);
    }
  }

  @Test
  public void shouldHandleEmptyJumpPointList() {
    GroupManager gm = new GroupManager(new int[] {}, 4);

    for(int i = 0; i < 10; i++) {
      assertEquals(i, gm.toViewIndex(i));
      assertEquals(i, gm.toModelIndex(i));
    }
  }

  @Test
  public void shouldConvertToVisibleIndicesCorrectly() {
    int[] expectedValues = new int[] {
       0,  1,  2,  3,
       4,  5,  6,
       8,  9,
      12, 13, 14, 15,
      16,
      20, 21, 22, 23
    };

    for(int i = 0; i < expectedValues.length; i++) {
      assertEquals(expectedValues[i], manager4.toViewIndex(i), "input: " + i);
      assertEquals(i, manager4.toModelIndex(manager4.toViewIndex(i)));
    }

    expectedValues = new int[] {
       0,  1,  2,
       3,  4,  5,
       6,
       9, 10,
      12, 13, 14,
      15,
      18,
      21, 22, 23
    };

    for(int i = 0; i < expectedValues.length; i++) {
      assertEquals(expectedValues[i], manager3.toViewIndex(i), "input: " + i);
      assertEquals(i, manager3.toModelIndex(manager3.toViewIndex(i)));
    }
  }

  @Test
  public void toModelIndexShouldConvertToModelIndicesCorrectly() {
    int[] expectedValues = new int[] {
       0,  1,  2,  3,
       4,  5,  6, -1,
       7,  8, -1, -1,
       9, 10, 11, 12,
      13, -1, -1, -1,
      14, 15, 16, 17
    };

    for(int i = 0; i < expectedValues.length; i++) {
//      System.out.println("Converting " + i);
      assertEquals(expectedValues[i], manager4.toModelIndex(i), "input: " + i);
    }
  }

  @Test
  public void toModelIndexSmartShouldConvertToModelIndicesCorrectly() {
    int[] expectedValues = new int[] {
       0,  1,  2,  3,
       4,  5,  6,  6,
       7,  8,  8,  8,
       9, 10, 11, 12,
      13, 13, 13, 13,
      14, 15, 16, 17
    };

    for(int i = 0; i < expectedValues.length; i++) {
      assertEquals(expectedValues[i], manager4.toModelIndexSmart(i), "input: " + i);
    }
  }

  @Test
  public void isValidViewIndexShouldReturnCorrectResult() {
    boolean[] expectedValues = new boolean[] {
       true, true, true, true,
       true, true, true, false,
       true, true, false, false,
       true, true, true, true,
       true, false, false, false,
       true, true, true, true
    };

    for(int i = 0; i < expectedValues.length; i++) {
      assertEquals(expectedValues[i], manager4.isValidViewIndex(i), "input: " + i);
    }

    expectedValues = new boolean[] {
      true, true, true,
      true, true, true,
      true, false, false,
      true, true, false,
      true, true, true,
      true, false, false,
      true, false, false,
      true, true, true
   };

   for(int i = 0; i < expectedValues.length; i++) {
     assertEquals(expectedValues[i], manager3.isValidViewIndex(i), "input: " + i);
   }
  }

  @Test
  public void shouldCountViewItemsCorrectly() {
    int[] rows1 = new int[] {4, 4, 4, 4, 3,
                             2, 1, 2, 1, 4,
                             3, 2, 1, 1, 4,
                             4, 4, 4, 4, 4};

    for(int i = 0; i < rows1.length; i++) {
      assertEquals(rows1[i], manager4.countViewItems(i, 1), "input 1 row: " + i);
    }

    int[] rows3 = new int[] {9, 8, 7, 10, 9,
                             8, 7, 7, 6, 9,
                             8, 7, 6, 9, 12,
                             12, 12, 12};

    for(int i = 0; i < rows3.length; i++) {
      assertEquals(rows3[i], manager4.countViewItems(i, 3), "input 3 rows: " + i);
    }
  }

  @Test
  public void getViewRowNumberShouldReturnCorrectRow() {
    int[] rows = new int[] {0, 0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 3, 3, 4, 5, 5, 5, 5, 6, 6, 6, 6};

    for(int i = 0; i < rows.length; i++) {
      assertEquals(rows[i], manager4.getViewRowNumber(i));
    }
  }

  @Test
  public void modelIndexOfViewRowShouldReturnCorrectIndex() {
    int[] rows = new int[] {0, 4, 7, 9, 13, 14, 18, 22, 26};

    for(int i = 0; i < rows.length; i++) {
      assertEquals(rows[i], manager4.modelIndexOfViewRow(i));
    }
  }

  @Test
  public void viewIndexOfLastItemInViewRowShouldReturnCorrectIndex() {
    int[] rows = new int[] {3, 6, 9, 15, 16, 23, 27, 31};

    for(int i = 0; i < rows.length; i++) {
      assertEquals(rows[i], manager4.viewIndexOfLastItemInViewRow(i), "input: " + i);
    }
  }
}
