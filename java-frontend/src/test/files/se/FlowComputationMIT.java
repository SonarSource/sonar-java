import java.util.*;

class A {

  private boolean trueIfNull(Object a) {
    if (a == null) { // flow@arg [[order=2]] {{Implies 'a' is null.}} flow@nested [[order=3]] {{Implies 'a' is null.}}
      return true;
    }
    return false;
  }

  private Object throwIfNull(Object a) { // flow@ex [[order=2]] {{Implies 'a' has the same value as 'o'.}} flow@ex2 [[order=2]] {{Implies 'a' has the same value as 'o'.}}
    if (a == null) throw new IllegalStateException(); // flow@ex [[order=3]] {{Implies 'a' is null.}}  flow@ex2 [[order=3]] {{Implies 'a' is non-null.}}
    return a;
  }

  void exceptions2(Object o) {
    throwIfNull(o); // flow@ex2 [[order=1]] {{'o' is passed to 'throwIfNull()'.}} flow@ex2 [[order=4]] {{Implies 'o' is non-null.}}
    if (o != null) { // Noncompliant [[flows=ex2]] flow@ex2 [[order=5]] {{Expression is always true.}}

    }
  }

  void test(Object a) {
    if (trueIfNull(a)) { // flow@arg [[order=1]] {{'a' is passed to 'trueIfNull()'.}} flow@arg [[order=3]] {{Implies 'a' is null.}}
      a.toString(); // Noncompliant [[flows=arg]] {{A "NullPointerException" could be thrown; "a" is nullable here.}}  flow@arg [[order=4]] {{'a' is dereferenced.}}
    }
  }

  void exceptions(Object o) {
     try {
       throwIfNull(o); // flow@ex [[order=1]] {{'o' is passed to 'throwIfNull()'.}} flow@ex [[order=4]] {{Implies 'o' is null.}}
     } catch (IllegalStateException ex) {
       o.toString(); // Noncompliant [[flows=ex]] {{A "NullPointerException" could be thrown; "o" is nullable here.}} flow@ex [[order=5]] {{'o' is dereferenced.}}
     }
  }

  private boolean callTrueIfNull(Object a) {
    return trueIfNull(a); // flow@nested [[order=2]] {{'a' is passed to 'trueIfNull()'.}} flow@nested [[order=4]] {{Implies 'a' is null.}} 
  }

  void nestedTest(Object a) {
    if (callTrueIfNull(a)) { // flow@nested [[order=1]] {{'a' is passed to 'callTrueIfNull()'.}} flow@nested [[order=5]] {{Implies 'a' is null.}}
      a.toString(); // Noncompliant [[flows=nested]] flow@nested [[order=6]] {{'a' is dereferenced.}}
    }
  }

  private Object getNull() {
    return null;  // _flow@return  FIXME if result SV == SV_0 there is not flow in the yield
  }

  private Object sundayIsAGoodDay() {
    if (cond) {
      return getNull(); // flow@return {{'getNull()' returns null.}}
    } else {
      return new Object();
    }
  }

  void returnValue() {
    Object o = sundayIsAGoodDay(); // flow@return {{'sundayIsAGoodDay()' can return null.}} flow@return {{'o' is assigned null.}}
    o.toString(); // Noncompliant [[flows=return]] flow@return
  }

}

