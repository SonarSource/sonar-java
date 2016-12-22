/*
 * Header
 */
public class HelloWorld {
  Object a = new

    java.lang.
    Object() {
    void foo() {
      try {
        System.out.println("try");
      } catch (Exception e) {
        System.out.println("e");
      }
    }
  };

  Object b;

  {
    System.out.println("");
  }
  static {
    System.out.println("");
    if(
      a ? b :
        c
      ) {
      System.out.println("");
    }
  }
  public HelloWorld() {
    System.out.println(""); // instructions in constructors
  }
  void foo() {
    for(;;) {
    }
  }
  static final String s = "s";
  Object o1 = a -> a;
  Object o2 = a ->  {
    return a;
  };
}
interface I {
  String s = "s"; // Wrongly counted as executable because it is implicitely static final
}
abstract class B {
  static long l1 = 1L;
  static final long l2 = 1L;
  final long l3 = 1L;
  abstract void method();
  String method2() {
    return null;
  }
}