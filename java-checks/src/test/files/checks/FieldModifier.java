class A {
  int member; // Noncompliant [[sc=7;ec=13]] {{Explicitly declare the visibility for "member".}}
  public int member2;
  private int member3;
}
enum B{
  C,D;
  int varEnum; // Noncompliant {{Explicitly declare the visibility for "varEnum".}}
  final int varEnum2; // Noncompliant {{Explicitly declare the visibility for "varEnum2".}}
  static int varEnum3; // Noncompliant {{Explicitly declare the visibility for "varEnum3".}}
  private int varEnum4;
}

class D {
  @com.google.common.annotations.VisibleForTesting
  int member; // Compliant
  @com.google.common.annotations.VisibleForTesting
  final int var2; // Compliant
  @com.google.common.annotations.VisibleForTesting
  static int var3; // Compliant
}
enum E {
  @com.google.common.annotations.VisibleForTesting
  int varEnum; // Compliant
  @com.google.common.annotations.VisibleForTesting
  final int varEnum2; // Compliant
  @com.google.common.annotations.VisibleForTesting
  static int varEnum3; // Compliant
}
