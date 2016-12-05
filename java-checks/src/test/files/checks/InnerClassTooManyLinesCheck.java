import java.lang.Object;

public class A {


  class B { // Noncompliant {{Reduce this class from 35 to the maximum allowed 25 or externalize it in a public class.}}
    void foo() {
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");
      System.out.println("");

    }
    class C {

    }
  }
  static class D { // Noncompliant {{Reduce this class from 30 to the maximum allowed 25 or externalize it in a public class.}}

    class E {
      void bar () {

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
      }


      void plop() {

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
      }
      // All this empty and vaccum space does not count





    }
  }
}
class B {
  class C {
    class D { }
  }
}
class F {
  void foo() {
    new Object() {











































    };
  }
}
