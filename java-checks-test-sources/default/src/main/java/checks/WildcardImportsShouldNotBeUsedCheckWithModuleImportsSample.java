package checks;

// When module imports are present, wildcard imports are compliant (allowed by S8445)
import module java.base;

import java.io.*; // Compliant - module imports are present
import java.util.*; // Compliant - module imports are present
import org.apache.commons.io.*; // Compliant - module imports are present
import java.sql.Date;
import static java.util.Arrays.*;
import static java.util.Collections.addAll;

public final class WildcardImportsShouldNotBeUsedCheckWithModuleImportsSample {

  private WildcardImportsShouldNotBeUsedCheckWithModuleImportsSample() {
    super();
  }

  public static boolean testMethodInput(InputStream input) {
    return input == null;
  }

  public static void testMethodArrays() {
    sort(new int[] {});
    List<Class<?>> list = new ArrayList<Class<?>>();
    addAll(list, FileUtils.class);
  }
}
