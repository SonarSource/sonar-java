package checks.UselessImportCheck.subpackage;

import checks.UselessImportCheck.UselessImportCheckWithNestedClass.Nested; // Noncompliant {{Remove this unnecessary import: this nested class is already imported from the parent.}}
import checks.UselessImportCheck.UselessImportCheckWithNestedClass;

public class UselessImportCheckImportingNestedOrder extends UselessImportCheckWithNestedClass {
  Nested getNested() {
    return Nested.something;
  }
}
