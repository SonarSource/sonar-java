package checks;

record HiddenFieldCheckWithRecords(int x) {
  int y = 23;

  void f() {
    int x = 42; // FN - no full support for records while it's still a preview feature.
    int y = 13; // Noncompliant {{Rename "y" which hides the field declared at line 4.}}
  }
}
