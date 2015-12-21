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
