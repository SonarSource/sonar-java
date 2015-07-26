class A {
  private final int f1 = 0;                             // Noncompliant {{Make this final field static too.}}
  private final static int f2 = 0;                      // Compliant
  private static final int f3 = 0;                      // Compliant
  public final int f4 = MyEnumOrInterface.MY_CONSTANT;  // Noncompliant
  private final int f5 = new Date();                    // Compliant
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
  private final String[] NOT_POSSIBLE = {}; // Noncompliant
  protected final Object [] a = new Object[] {"UTF-8", null}; // Noncompliant
}

interface B {
  final int f0 = 0;                                     // Compliant
}