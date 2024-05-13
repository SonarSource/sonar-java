package checks;

import java.util.Locale;
import org.apache.commons.lang.StringUtils;

abstract class CaseInsensitiveComparisonCheck {
  private static final String FOO = "foo";
  private static final String BAR = "BAR";

  private void f(String arg, String arg2) {
    boolean result = arg.toUpperCase().equals(arg2.toLowerCase()); // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
//                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    result = arg.toLowerCase().equals(BAR); // Noncompliant
    result = arg.toUpperCase().equals(BAR); // Noncompliant
    result = "foo".toUpperCase().equals(BAR); // Noncompliant
    result = FOO.equals(arg.toLowerCase()); // Noncompliant
    result = BAR.equals(arg.toUpperCase()); // Noncompliant
    result = "FOO".equals(arg.toUpperCase()); // Noncompliant
    // This one can't actually return true, but it's most likely *intended* to be a case-insensitive comparison:
    result = "foo".equals(arg.toUpperCase()); // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}

    result = "foo".equals("foo");                                  // Compliant - no use of toUpperCase or toLowerCase
    result = arg.equals(arg2);                                     // Compliant - ditto
    result = arg.toUpperCase().equals(arg);                        // Compliant - this is not case-insensitive - it checks whether arg is ALL_CAPS
    result = arg.toLowerCase().equals(arg2);                       // Compliant - it's only true if arg2 contains no upper case characters
    result = "foo".equals(arg2);                                   // Compliant

    Locale trLocale = new Locale("tr", "TR");
    result = arg.toUpperCase(trLocale).equals(BAR);                // Compliant when locale is specified because it does not behave like equalsIgnoreCase
    result = FOO.equals(arg.toLowerCase(trLocale));                // Compliant

    FOO.equals(arg2.toLowerCase()); // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    FOO.equals(toLowerCase());                                     // Compliant
    BAR.equals(something().somethingElse().toUpperCase()); // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    FOO.equals("bar".toLowerCase()); // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}

    result = arg.equalsIgnoreCase(arg2);                           // Compliant

    arg.toUpperCase().compareTo("FOO");                            // Compliant
    arg.substring(42).equals(BAR);                                 // Compliant

    StringUtils.equals("foo", "bar".toLowerCase());                // Compliant

    arg.toLowerCase().toLowerCase().equals(FOO); // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    arg.toLowerCase().toUpperCase().equals(BAR); // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}

    result = equals(arg.toLowerCase());                            // Compliant (for coverage)
  }

  abstract String toLowerCase();
  abstract String somethingElse();
  abstract CaseInsensitiveComparisonCheck something();
}
