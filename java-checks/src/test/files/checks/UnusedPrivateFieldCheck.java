class FooClass {

  private int unusedField; // Noncompliant

  @UsedBySomeFramework
  private int foo; // Noncompliant

  int usedField; // Compliant

  public int foo; // Compliant

  private static final long serialVersionUID = 4858622370623524688L; // Compliant
  
  private int usedPrivateField;
  private int unreadField; // Noncompliant
  private int usedOnlyInAccessWithPostIncrement;
  private int usedOnlyInAssignmentExpression;

  private static class InnerClass {
    private int innerClassUsedField;
    private int innerClassUnreadField; // Noncompliant
  }
  
  public void f(int unusedParameter) {
    InnerClass innerClass = new InnerClass();
    unreadField = -usedPrivateField + usedOnlyInAccessWithPostIncrement++;
    this.unreadField = new InnerClass().innerClassUsedField;
    innerClass.innerClassUnreadField = 1;
    unreadField += 1;
    unreadField = (usedOnlyInAssignmentExpression += 1);
    
    unknownVar = 3;
    
    int unusedLocalVariable;

    int usedLocalVariable = 42 + usedField;
    System.out.println(usedLocalVariable);

    try {
    } catch (Exception e) { // Compliant
    }

    try (Stream foo = new Stream()) { // Noncompliant
    }

    for (int a: new int[]{ 0, 1, 2 }) { // Noncompliant
    }
  }

}

enum FooEnum {

  FOO;

}

interface FooInterface {

  int FOO = 0; // Compliant

}

class TestSonar {
  private static Transformer TRANSFORMER = new Transformer();

  public void test() {
    Optional.ofNullable("10").map(TRANSFORMER::transform).ifPresent(System.out::print);
  }

  private static class Transformer {
    Long transform(String number) {
      return Long.valueOf(number);
    }
  }
}
