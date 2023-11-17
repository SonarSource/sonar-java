package checks.unused;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import javax.validation.constraints.NotNull;

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

class UnusedPrivateFieldCheckQuickfix {

  private String unusedField; // Noncompliant [[sc=18;ec=29;quickfixes=qf0]]
  //  fix@qf0 {{Remove this unused private field}}
  // edit@qf0 [[sc=3;ec=30]]{{}}

  private String usedField;

  private String unusedFirst, usedSecond; // Noncompliant [[sc=18;ec=29;quickfixes=qf1]]
  //  fix@qf1 {{Remove this unused private field}}
  // edit@qf1 [[sc=18;ec=31]]{{}}

  private String usedFirst, unusedSecond; // Noncompliant [[sc=29;ec=41;quickfixes=qf2]]
  //  fix@qf2 {{Remove this unused private field}}
  // edit@qf2 [[sc=27;ec=41]]{{}}

  private static int unusedStatic; // Noncompliant [[sc=22;ec=34;quickfixes=qf3]]
  //  fix@qf3 {{Remove this unused private field}}
  // edit@qf3 [[sc=3;ec=35]]{{}}

  private final int unusedFinal = 0; // Noncompliant [[sc=21;ec=32;quickfixes=qf4]]
  //  fix@qf4 {{Remove this unused private field}}
  // edit@qf4 [[sc=3;ec=37]]{{}}

  private static final int unusedStaticFinal = 0; // Noncompliant [[sc=28;ec=45;quickfixes=qf5]]
  //  fix@qf5 {{Remove this unused private field}}
  // edit@qf5 [[sc=3;ec=50]]{{}}

  private static int unusedStaticFirst, usedStaticSecond; // Noncompliant [[sc=22;ec=39;quickfixes=qf6]]
  //  fix@qf6 {{Remove this unused private field}}
  // edit@qf6 [[sc=22;ec=41]]{{}}

  private final int unusedFinalFirst = 0, usedFinalSecond = 0; // Noncompliant [[sc=21;ec=37;quickfixes=qf7]]
  //  fix@qf7 {{Remove this unused private field}}
  // edit@qf7 [[sc=21;ec=43]]{{}}

  private static final int unusedStaticFinalFirst = 0, usedStaticFinalSecond = 0; // Noncompliant [[sc=28;ec=50;quickfixes=qf8]]
  //  fix@qf8 {{Remove this unused private field}}
  // edit@qf8 [[sc=28;ec=56]]{{}}

  /**
   * This comment will not be removed
   */
  /**
   * This documentation should be removed as part of the quickfix
   */
  private static final int FIELD_WITH_JAVADOC = 42; // Noncompliant [[sc=28;ec=46;quickfixes=qf9]]
  //  fix@qf9 {{Remove this unused private field}}
  // edit@qf9 [[sl=-3;sc=3;el=+0;ec=52]]{{}}

  private void f() {
    System.out.println(usedField);
    System.out.println(usedSecond);
    System.out.println(usedFirst);
    System.out.println(usedStaticSecond);
    System.out.println(usedFinalSecond);
    System.out.println(usedStaticFinalSecond);
  }
}

