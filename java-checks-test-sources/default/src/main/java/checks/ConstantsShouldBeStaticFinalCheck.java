package checks;

import java.util.regex.Matcher;

class ConstantsShouldBeStaticFinalCheck {

  private static final int MY_CONSTANT = 1;
  private static final Object obj = new Object();

  class Parent {
    int foo = 1;
  }

  abstract class A extends Parent {
    private final static int foo = 1;
    private final int f1 = 0;                             // Noncompliant [[sc=23;ec=25]] {{Make this final field static too.}}
    private final static int f2 = 0;                      // Compliant
    private static final int f3 = 0;                      // Compliant
    public final int f4 = ConstantsShouldBeStaticFinalCheckEnumTest.MY_CONSTANT; // Noncompliant
    private final int f5 = new Integer(42);         // Compliant
    private final int f6 = foo();                         // Compliant
    private final int f62 = this.foo;                     // Compliant
    private final int f63 = super.foo;                    // Compliant
    private final int f64 = foo;                          // Noncompliant
    private int f7 = 0;                                   // Compliant
    private int f8;                                       // Compliant
    private final int f9;                                 // Compliant
    private final checks.ConstantsShouldBeStaticFinalCheck.A myA = this; // Compliant
    private final int
      f10 = 0,                                             // Noncompliant
      f11,                                                 // Compliant
      f12 = foo(),                                         // Compliant
      f13 = MY_CONSTANT;                                   // Noncompliant
    private final int[] newInt = new int[42];              // Compliant
    private final String[] NOT_POSSIBLE = {}; // compliant, array are not constants
    protected final Object [] a = new Object[] {"UTF-8", null}; // compliant, array are not constants
    private final Matcher[] matchers = new Matcher[]{ //should not raise issue
      matcher(),
    };

    A() {
      f9 = 1;
      f11 = 1;
    }

    public class InnerClass{
      private final int POSSIBLE = 4; // Noncompliant
      private final String POSSIBLE_2 = ""; // Noncompliant

      private final String[] NOT_POSSIBLE = {}; //Compliant
      private final Object NOT_POSSIBLE_2 = new Object(); //Compliant
      private final Object NOT_POSSIBLE_3 = MY_CONSTANT; //Compliant
    }

    int foo() {
      return 1;
    }

    abstract Matcher matcher();
  }

  interface B {
    final int f0 = 0;                                     // Compliant
  }

  static class C {
    private final java.util.function.Consumer<Object> o = this::someMethod; // compliant
    private final java.util.function.Consumer<Object> o1 = new C()::someMethod; // compliant
    C c = new C();
    private final java.util.function.Consumer<Object> o2 = c::someMethod; // compliant
    private final java.util.function.Consumer<Object> o3 = C::someMethod2; // compliant

    void someMethod(Object o) {
      return;
    }
    static void someMethod2(Object o) {
      return;
    }
  }
}

class AvoidFPsWhenUsingFieldInInstance_SONARJAVA_4749 {
  private static long nextID = 1L;
  private final long localID = nextID++; // Compliant

  private final String foo = new String("test"); // Compliant
  private final String bar = foo; // Compliant

  private static final String sf1 = "Foo";
  private static final String sf2 = "Bar";

  private final String selection = nextID == 1 ? sf1 : sf2; // Compliant
  private final String selection2 = sf1 == "foo" ? sf1 : sf2; // Noncompliant
  private final String selection3 = sf1.equals("foo") ? sf1 : sf2; // Compliant, as we don't know whether `equals` always yields the same result.

  public long getID()
  {
    return localID;
  }
}

enum ConstantsShouldBeStaticFinalCheckEnumTest {
  TEST1(4);
  final static int MY_CONSTANT = 1;
  private final int possible;
  private ConstantsShouldBeStaticFinalCheckEnumTest(int test) {
    possible = test;
  }
}

class ConstantsShouldBeStaticFinalCheckDemo {

  final int[] coordinate = new int[] {0, 0, 0}; // compliant

  interface Something {
    public void printCreation();
  }

  long getValue() {
    return System.currentTimeMillis();
  }

  Something getSomething() {
    final long valueAtCreationTime = getValue();

    return new Something() {

      private final long creation = valueAtCreationTime; // compliant
      private final String creationStr = "" + valueAtCreationTime; // compliant

      @Override
      public void printCreation() {
        System.out.println(this.creation);
      }
    };
  }
}

class FieldAssignments {
  private final int[] finalFoo = new int[42]; // Compliant
  private final Object finalObject = finalFoo; // Compliant - finalFoo is not static

  private static int[] staticFoo = new int[42]; // Compliant

  private final Object staticObject = staticFoo; // Compliant - staticFoo is not final

  private static final int[] foo = new int[42];
  private final Object object = foo; // Noncompliant

  private final int bar = foo[2]; // Compliant (array access)
}

