import com.google.common.base.Function;
import java.util.Date;

enum foo  {
  FOO{
    @Override
    public String method() {
      return "foo";
    }
  },
  BAR{
    @Override
    public String method() {
      return "bar";
    }
  };


  public String method(){
    return "";
  }
}

interface Handler {
  String handle();
}

interface MyInterface {
  enum InnerEnum {
    A,B,C;
  }
}

class A {
  void toto() {
    new MyInterface() {}; // Compliant

    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda}}
      @Override
      public String handle() {
        return "handled";
      }
    }.handle();

    new Handler(){

      private String myMethod(){
        return "plop";
      }

      @Override
      public String handle() {
        return myMethod();
      }
    }.handle();

    new Handler(){
      @Override
      public String handle() {
        return this.toString();
      }
    }.handle();

    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda}}
      @Override
      public String handle() {
        class C{
          String meth(){
            return "";
          }
          String fun(){
            return this.meth();
          }
        }
        return new C().fun();
      }
    };

    new Handler(){ // Compliant
      int myVar;

      @Override
      public String handle() {
        return "";
      }
    };

    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda}}
      @Override
      public String handle() {
        return "";
      }; // this empty statement should not be counted!
    };

    new
    Handler // Noncompliant {{Make this anonymous inner class a lambda}}
    (){
      @Override
      public String handle() {
        return A.this.toStr();
      }
    };
  }

  String toStr(){
    return "";
  }

  interface MyHandler extends Handler{}
  public abstract class Main {

    public abstract void myMethod();

    public static void main(String[] args) {
      Main main = new Main() {

        @Override
        public void myMethod() {
        }
      };
      main.myMethod();
      Object o1 = new Object() {
        @Override
        public String toString(){
          return null;
        }
      };
      Object o12 = new MyHandler() { // Noncompliant
        @Override
        public String handle() {
          return null;
        }
      };
    }
  }
}

class SamWithException {

  class MyCheckedException extends Exception {}
  interface I {
    void apply(String s) throws MyCheckedException;
  }
  void foo(I i) {
    foo(new I() { // Compliant : Cannot be nicely refactored as lamdba because of the checked exception
      @Override
      public void apply(String s) throws MyCheckedException {
        // doSomething
      }
    });
  }
}

abstract class WithinLambda {

  @FunctionalInterface
  interface Action<T> {
    T run();
  }

  abstract <T> T doSomething(Action<T> action);

  private void bar(WithinLambda a) {
    a.doSomething(
      (Action<Void>) () -> {
        new Thread(
          new Runnable() { // Noncompliant
            @Override
            public void run() {
              // do somehting
            }
          });
        return null;
      });
  }
}

interface AB {
  default void foo() {
  }

  default void bar() {
  }

  static void main() {
    AB a = new AB() { // Compliant
      @Override
      public void foo() {
      }
    };
  }
}

interface BA {
  default void foo() {
  }

  void bar();

  static void main() {
    BA a = new BA() { // Noncompliant
      @Override
      public void bar() {
      }
    };
  }
}

class Alpha {

  interface Lvl1 {
    void foo();
  }

  interface Lvl2 extends Lvl1 {
    @Override
    void foo();
  }

  Lvl2 level = new Lvl2() { // Noncompliant
    @Override
    public void foo() {
    }
  };
  Lvl2 level2 = () -> {};

  Function<Object, Date> a = new Function<Object, Date>() { // Noncompliant - function overrides equals from object
    @Override
    public Date apply(Object o) {
      return new Date();
    }
  };

  Function<Object, Date> b = o -> new Date();
}

abstract class AbstractClass {
  public abstract void foo();

  static void bar() {
    AbstractClass ac1 = new AbstractClass() { // Compliant: not a SAM
      @Override
      public void foo() {
      }
    };

    Unknown u = new Unknown() { // Compliant: can not resolve parent
      @Override
      void foo() {
      }
    };
  }
}

class ThisInstanceTest {

  interface WithDefault {
    default String defaultMethod() { return "defaultMethod"; }
    String funcMethod();
  }
  void testDefault() {
    WithDefault f = new WithDefault() {  // Compliant, invoke a default method
      @Override
      public String funcMethod() {
        return defaultMethod();
      }
    };
  }

  interface Math {
    int powerOfTwo(int n);
  }

  void testRecursion() {
    Math f = new Math() {  // Compliant, recursion
      @Override
      public int powerOfTwo(int n) {
        return n == 0 ? 1 : 2 * powerOfTwo(n -1);
      }
    };
  }

  int globalPowerOfTwo(int n) {
    return n == 0 ? 1 : 2 * globalPowerOfTwo(n -1);
  }

  void testNotThisInstanceMethod() {
    Math f = new Math() {  // Noncompliant
      @Override
      public int powerOfTwo(int n) {
        return globalPowerOfTwo(n);
      }
    };
  }

  interface InvokeStatic {

    int func();

    static int staticFunc() {
      InvokeStatic f = new InvokeStatic() {  // Noncompliant
        @Override
        public int func() {
          unknown();
          staticFunc();
          return 0;
        }
      };
      return f.func();
    }
  }

}

abstract class GenericType<X> {

  void foo(GenericType<String> something) {
    bar(something, new MyComparable() { // Compliant - compare is a generic method
      @Override
      public <T extends Comparable<T>> int compare(T obj1, T obj2) {
        return 0;
      }
    });
  }

  abstract <T extends Comparable<T>> void bar(GenericType<T> object, MyComparable comp);

  interface MyComparable {
    public <T extends Comparable<T>> int compare(T obj1, T obj2);
  }
}
