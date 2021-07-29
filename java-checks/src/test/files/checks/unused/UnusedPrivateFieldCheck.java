class FooClass {

  private int unusedField; // Noncompliant [[sc=15;ec=26]] {{Remove this unused "unusedField" private field.}}

  @UsedBySomeFramework
  private int foo;

  int usedField; // Compliant

  public int foo2; // Compliant

  private static final long serialVersionUID = 4858622370623524688L; // Compliant
  
  private int usedPrivateField;
  private int unreadField; // Noncompliant {{Remove this unused "unreadField" private field.}}
  private int usedOnlyInAccessWithPostIncrement;
  private int usedOnlyInAssignmentExpression;

  private static class InnerClass {
    private int innerClassUsedField;
    private int innerClassUnreadField; // Noncompliant {{Remove this unused "innerClassUnreadField" private field.}}
  }

  public void f(int unusedParameter) {
    InnerClass innerClass = new InnerClass();
    unreadField = -usedPrivateField + usedOnlyInAccessWithPostIncrement++;
    this.unreadField = new InnerClass().innerClassUsedField;
    innerClass.innerClassUnreadField = 1;
    unreadField += 1;
    unreadField = (usedOnlyInAssignmentExpression += 1);
    
    ((unknownVar)) = 3;
    
    int unusedLocalVariable;

    int usedLocalVariable = 42 + usedField;
    System.out.println(usedLocalVariable);

    try {
    } catch (Exception e) { // Compliant
    }

    try (Stream foo = new Stream()) {
    }

    for (int a: new int[]{ 0, 1, 2 }) {
    }
  }

}

enum FooEnum {

  FOO;

}

interface FooInterface {

  int FOO = 0; // Compliant

}

class SpecialAnnotations {
  
  @lombok.Getter
  private int foo; // Compliant

  @javax.enterprise.inject.Produces
  private int bar; // Compliant

  @lombok.Setter
  private int foo2; // Compliant
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

class usageOfUnkownField {

  private Object field1; // Noncompliant
  private Object field2; // Noncompliant
  private Object field3; // Compliant
  private String field4; // Compliant
  private Object[] field5; // Compliant

  void foo(java.util.List<Integer> list) {
    field5[0] = new Object();
    field1(); // unknown method - ignored
    list.stream().filter(stuff::field2); // unknown method reference - ignored
    list.stream().filter(field3::equals);
    Object value = stuff.field4; // unknown field4
  }
}
