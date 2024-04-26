package checks;

import java.io.*; // Noncompliant {{Explicitly import the specific classes needed.}}
//     ^^^^^^^^^
import java.util.*; // Noncompliant {{Explicitly import the specific classes needed.}}
import org.apache.commons.io.*; // Noncompliant {{Explicitly import the specific classes needed.}}
import java.sql.Date; // Not used in code but at least one non static import
import static java.util.Arrays.*; // Compliant, exception with static imports
import static java.util.Collections.addAll;

/**
 *
 * A test class
 *
 */
public final class WildcardImportsShouldNotBeUsedCheck {

  /**
   * Constructor
   */
  private WildcardImportsShouldNotBeUsedCheck() {
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
    List<Class<?>> list = new ArrayList<Class<?>>();
    addAll(list, FileUtils.class);
  }
}
