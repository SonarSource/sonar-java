package checks.naming;

import java.util.ResourceBundle;

class BadClassNameNoncompliant {
  class badClassName { } // Noncompliant {{Rename this class name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.}}
//      ^^^^^^^^^^^^
  class GoodClassName { } // Compliant

  interface should_not_be_checked_interface { } // Compliant
  enum should_not_be_checked_enum { } // Compliant
  @interface should_not_be_checked_annotation { } // Compliant

  Object o = new Object() {
    // anonymous class
  };

  abstract class ResourceBundle_de extends ResourceBundle { } // Compliant

  abstract class MyResources extends ResourceBundle { } // Compliant

  abstract class MyResources_en extends MyResources { } // Compliant

  abstract class MyResources_en_Latn_US_WINDOWS_VISTA extends MyResources_en { } // Compliant

  abstract class MyResources___123 extends MyResources_en { } // Compliant

  abstract class NoResources_en { } // Noncompliant

  abstract class NoResources_en_Latn_US_WINDOWS_VISTA { } // Noncompliant

}
