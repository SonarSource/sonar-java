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

  /** @see <a href="http://example.com">Example</a>
   */ // Compliant - HTML anchor @see tag
  void htmlAnchorSeeTag() {
  }

  /** @see "The Java Programming Language"
   */ // Compliant - quoted string @see tag
  void quotedStringSeeTag() {
  }

  /** @see InnerClass
   */ // Compliant - inner type of the current class
  void innerTypeReference() {
  }

  /** {@link InnerEnum}
   */ // Compliant - inner enum of the current class
  void innerEnumReference() {
  }

  /** @see Map.Entry
   */ // Compliant - inner type via imported outer class
  void innerTypeViaImport() {
  }

  void existingMethod() {
  }

  static class InnerClass {
  }

  enum InnerEnum {
    VALUE
  }
}
