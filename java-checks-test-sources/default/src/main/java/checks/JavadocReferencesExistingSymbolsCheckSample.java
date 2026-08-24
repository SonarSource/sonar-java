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

  /** @see https://example.com
   */
  void externalUrlHttps() {
  }

  /** @see #existingMethod
   */
  void methodReference() {
  }

  /** @see java.util.List#size()
   */
  void validMethodReference() {
  }

  /** {@link java.util.List}
   */
  void validLinkTag() {
  }

  /** {@link java.util.NonExistentClass}
   */ // Noncompliant {{Make sure this reference is valid.}}
  void invalidLinkTag() {
  }

  /** {@linkplain java.util.NonExistentClass}
   */ // Noncompliant {{Make sure this reference is valid.}}
  void invalidLinkplainTag() {
  }

  /** {@link java.util.Map#get(Object)}
   */
  void validLinkWithMethod() {
  }

  /** @see NonExistentSimpleClass
   */ // Noncompliant {{Make sure this reference is valid.}}
  void simpleNameNonExistent() {
  }

  /** @see java.util.List#size()
   * {@link java.util.NonExistentClass}
   */ // Noncompliant {{Make sure this reference is valid.}}
  void mixedValidSeeInvalidLink() {
  }

  /** @see String
   */ // Compliant - java.lang.String is implicitly available
  void simpleNameJavaLang() {
  }

  /** @see List
   */ // Compliant - List is imported
  void simpleNameImported() {
  }

  /** {@link Map}
   */ // Compliant - Map is imported
  void simpleNameImportedLink() {
  }

  /** @see JavadocReferencesExistingSymbolsCheckSample
   */ // Compliant - same-package class
  void simpleNameSamePackage() {
  }

  void existingMethod() {
  }
}
