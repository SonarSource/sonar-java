import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("all")
class TypesOfDeclarations {

  interface Interface1 extends List<byte[]>{
  }

  interface Interface2 extends Interface1 {
  }

  enum Enum implements Interface1, Interface2 {
  }

  static class Class1 extends Collection {
    Interface1 field;

    Interface1 method(Interface2 param) {
      Interface1 localVariable;
      for (Interface1 forLoopVariable : Collections.<Interface1>emptyList()) {
      }
      return null;
    }
  }

  static class Class2 extends Class1 implements Interface1, Interface2 {
  }

  private static class Outer {
    private static int a;

    private static class Bar {
      private static int a;

      void method() {
        a = 1;
      }
    }

    private static class Foo extends Bar {
      void method() {
        a = 1;
      }
    }
  }

}
