package checks.unused;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

class UnusedPrivateFieldCheck {

  private int unusedField; // Noncompliant [[sc=15;ec=26]] {{Remove this unused "unusedField" private field.}}

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

    int unusedLocalVariable;

    int usedLocalVariable = 42 + usedField;
    System.out.println(usedLocalVariable);

    try {
    } catch (Exception e) { // Compliant
    }

    try (FileInputStream foo = new FileInputStream("path/to/some/file")) {
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (int a : new int[]{0, 1, 2}) {
    }
  }
}

enum UnusedPrivateFieldCheckFooEnum {

  FOO;

}

interface UnusedPrivateFieldCheckFooInterface {

  int FOO = 0; // Compliant

}

class UnusedPrivateFieldCheckSpecialAnnotations {

  @lombok.Getter
  private int foo; // Compliant

  @javax.enterprise.inject.Produces
  private int bar; // Compliant

  @lombok.Setter
  private int foo2; // Compliant
}

class UnusedPrivateFieldCheckTestSonar {
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

