class A {
  int a = 0;                             // Compliant
  String b = "" + a;                     // Non-Compliant
  String c = a + "";                     // Non-Compliant
  String d = a + a + "foo";              // Compliant
  String e = foo("");                    // Compliant
  String f = "foo" + "bar" + "" + "baz"; // Non-Compliant
  String g = "foo" + bar.baz();          // Compliant
}

class A {
  private static final int DEFAULT_FOO = 0;

  @RuleProperty(defValue = "" + DEFAULT_FOO) // Compliant
  private int foo = DEFAULT_FOO;
}
