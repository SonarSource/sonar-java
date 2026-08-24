package checks;

import java.util.List;
import java.util.Map;

/** @see java.util.List
 */ // Compliant - existing type
class JavadocReferencesExistingSymbolsCheckSample {

  /** @see java.util.NonExistentClass
   */ // Noncompliant {{Make sure this reference is valid.}}
  void nonExistentClass() {
  }

  /** @see java.util.NonExistentClass
   */ // Noncompliant {{Make sure this reference is valid.}}
  void nonExistentClass2() {
  }

  /** @see http://example.com
   */
  void externalUrl() {
  }

  /** @see #existingMethod
   */
  void methodReference() {
  }

  /** @see java.util.List#size()
   */
  void validMethodReference() {
  }

  void existingMethod() {
  }
}
