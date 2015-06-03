import java.io.*;
import java.util.*;
import java.util.List;

import static java.util.Arrays.*;
import static java.util.Collections.addAll;

/**
 *
 * A test class
 *
 */
public final class AvoidStarImportCheck {

  /**
   * Constructor
   */
  private MyClass() {
    super();
  }

  /**
   * java.io declaration
   * @param input Input
   * @return null or not
   */
  public static boolean testMethodInput(InputStream input) {
    return input == null;
  }

  /**
   * java.util declaration
   */
  public static void testMethodArrays() {
    sort(new int[] {});
    List<String> list = new ArrayList<String>();
    addAll(list, "");
  }
}
