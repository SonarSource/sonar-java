package checks.unused;


class UnusedPrivateFieldCheck {

  private Object field1; // Noncompliant
  private Object field2; // Noncompliant
  private Object field3; // Compliant
  private String field4; // Compliant
  private Object[] field5; // Compliant
  @UsedBySomeUnknownFramework
  private Object field6; // Compliant

  void foo(java.util.List<Integer> list) {
    ((unknownVar)) = 3;
    field5[0] = new Object();
    field1(); // unknown method - ignored
    list.stream().filter(stuff::field2); // unknown method reference - ignored
    list.stream().filter(field3::equals);
    Object value = stuff.field4; // unknown field4
  }
}
