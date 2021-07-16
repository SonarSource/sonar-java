package checks;

class StaticFieldUpateCheck {
  public static int staticValue = 0;
  private int value;
  private static int[] staticArray;

  public void nonCompliantAssignments() {
    staticValue = value + 1; // Noncompliant [[sc=5;ec=16]] {{Make the enclosing method "static" or remove this set.}}
  }
  public void compliantCode() {
    MyUnknownClass.staticField = value; // Compliant
  }
}
