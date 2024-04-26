package checks;

class StaticFieldUpateCheckSample {
  public static int staticValue = 0;
  private int value;
  private static int[] staticArray;

  public void nonCompliantAssignments() {
    staticValue = value + 1; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
//  ^^^^^^^^^^^
  }
  public void compliantCode() {
    MyUnknownClass.staticField = value; // Compliant
  }
}
