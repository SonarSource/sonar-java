class FooClass {

  private int unusedField; // Noncompliant {{Remove this unused "unusedField" private field.}}

  @UsedBySomeFramework
  private int foo;

  int usedField; // Compliant

  public int foo; // Compliant

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

@lombok.Getter
class ClassLevelAnnotations {
  private int foo; // Compliant
}
@lombok.Setter
class ClassLevelAnnotations2 {
  private int foo; // Compliant
}
@lombok.Data
class ClassLevelAnnotations3 {
  private int foo; // Compliant
}
@lombok.Value
class ClassLevelAnnotations4 {
  private int foo; // Compliant
}
@lombok.Builder
class ClassLevelAnnotations5 {
  private int foo; // Compliant
}
@lombok.ToString
class ClassLevelAnnotations6 {
  private int foo; // Compliant
}
@lombok.RequiredArgsConstructor
class ClassLevelAnnotations7 {
  private int foo; // Compliant
}
@lombok.AllArgsConstructor
class ClassLevelAnnotations8 {
  private int foo; // Compliant
}
@lombok.NoArgsConstructor
class ClassLevelAnnotations9 {
  private int foo; // Compliant
}
@lombok.EqualsAndHashCode
class ClassLevelAnnotations10 {
  private int foo; // Compliant
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
