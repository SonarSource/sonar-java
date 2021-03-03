package checks.UselessImportCheck.subpackage;

import checks.UselessImportCheck.UselessImportCheckWithNestedClass;
import checks.UselessImportCheck.UselessImportCheckWithNestedClass.Nested; // Noncompliant {{Remove this unnecessary import: this nested class is already imported from the parent.}}

public class UselessImportCheckImportingNested extends UselessImportCheckWithNestedClass {
  Nested getNested() {
    return Nested.something;
  }
}
