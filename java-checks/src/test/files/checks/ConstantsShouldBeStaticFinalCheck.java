class A {
  private final int f1 = 0;                             // Noncompliant [[sc=21;ec=23]] {{Make this final field static too.}}
  private final static int f2 = 0;                      // Compliant
  private static final int f3 = 0;                      // Compliant
  public final int f4 = MyEnumOrInterface.MY_CONSTANT;  // Noncompliant
  private final int f5 = new Integer(42);               // Compliant
  private final int f6 = foo();                         // Compliant
  private int f7 = 0;                                   // Compliant
  private int f8;                                       // Compliant
  private final int f9;                                 // Compliant
  private final int
   f10 = 0,                                             // Noncompliant
   f11,                                                 // Compliant
   f12 = foo(),                                         // Compliant
   f13 = BAR;                                           // Noncompliant
  private final int object = (Type<?>) foo;// Noncompliant
  private final int[] foo = new int[42];                // Compliant
  public class InnerClass{
    private final int POSSIBLE = 4; // Noncompliant
    private final String POSSIBLE_2 = ""; // Noncompliant

    private final String[] NOT_POSSIBLE = {}; //Compliant
    private final Object NOT_POSSIBLE_2 = new Object(); //Compliant
    private final Object NOT_POSSIBLE_3 = MY_CONSTANT; //Compliant
  }
  enum enumTest {
    TEST1(4);
    private final int possible;
    private enumTest(int test) {
      possible = test;
    }
  }
  private final String[] NOT_POSSIBLE = {}; // compliant, array are not constants
  protected final Object [] a = new Object[] {"UTF-8", null}; // compliant, array are not constants
  private final Matcher[] matchers = new Matcher[]{ //should not raise issue
    matcher(g(DIGIT_SEQUENCE, "\\.", o2n(DIGIT), opt(EXPONENT_PART), opt(FLOATING_SUFFIX), CppLexer.OPT_UD_SUFFIX)),
  };
}

interface B {
  final int f0 = 0;                                     // Compliant
}

static class C {
  private final java.util.function.Consumer<Object> o = this::someMethod; // compliant
  private final java.util.function.Consumer<Object> o1 = new C()::someMethod; // compliant
  C c = new C();
  private final java.util.function.Consumer<Object> o2 = c::someMethod; // compliant
  private final java.util.function.Consumer<Object> o3 = C::someMethod2; // Noncompliant

  void someMethod(Object o) {
    return;
  }
  static void someMethod2(Object o) {
    return;
  }

}

public class Demo {

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
      private final long creation2;

      @Override
      public void printCreation() {
        System.out.println(this.creation);
      }
    };
  }
}

