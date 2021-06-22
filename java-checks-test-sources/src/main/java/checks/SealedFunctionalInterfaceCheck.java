package checks;

public class SealedFunctionalInterfaceCheck {

  public sealed interface A1 permits A2 { // Noncompliant [[sc=10;ec=16]] {{Remove this "sealed" keyword if this interface is supposed to be functional.}}
    boolean singleMethod();
  }

  public sealed interface A2 extends A1 permits A3 { // Compliant, extends other classes
    boolean additionalMethod();
  }

  public non-sealed interface A3 extends A2 { // Compliant, 'non-sealed' modifier
  }

  public static abstract class B1 { // Compliant, not an interface
    abstract boolean singleMethod();
  }

  public interface B2 { // Compliant, no 'sealed' modifier
    boolean singleMethod();
  }

  public sealed interface C1 permits C2 { // Compliant, more than one member
    boolean method1();
    boolean method2();
  }

  public non-sealed interface C2 extends C1 {
  }

  public sealed interface E1 permits E2 { // Compliant, field
    int constant = 42;
  }

  public non-sealed interface E2 extends E1 {
  }

  public sealed interface F1 permits F2 { // Compliant, sub type
    interface SubInterface { }
  }

  public non-sealed interface F2 extends F1 {
  }

  public sealed interface G1 permits G2 { // Compliant, default method
    default void method() {
    }
  }

  public non-sealed interface G2 extends G1 {
  }

  public sealed interface H1 permits H2 { // Compliant, static method
    static void method() {
    }
  }

  public non-sealed interface H2 extends H1 {
  }

}
