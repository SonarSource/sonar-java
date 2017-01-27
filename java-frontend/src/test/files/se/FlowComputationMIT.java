import java.util.*;

class A {

  private boolean trueIfNull(Object a) {
    if (a == null) { // flow@arg,nested {{Implies 'a' is null.}}
      return true;
    }
    return false;
  }

  private Object throwIfNull(Object a) {
    if (a == null) throw new IllegalStateException(); // flow@ex {{Implies 'a' is null.}}  flow@ex2 {{Implies 'a' is non-null.}}
    return a;
  }

  void exceptions2(Object o) {
    throwIfNull(o); // flow@ex2  {{Implies 'o' is non-null.}}
    if (o != null) { // Noncompliant [[flows=ex2]] {{Change this condition so that it does not always evaluate to "true"}} flow@ex2 {{Condition is always true.}}

    }
  }

  void test(Object a) {
    if (trueIfNull(a)) { // flow@arg {{Implies 'a' is null.}}
      a.toString(); // Noncompliant [[flows=arg]] {{NullPointerException might be thrown as 'a' is nullable here}}  flow@arg {{'a' is dereferenced.}}
    }
  }

  void exceptions(Object o) {
     try {
       throwIfNull(o); // flow@ex {{Implies 'o' is null.}}
     } catch (IllegalStateException ex) {
       o.toString(); // Noncompliant [[flows=ex]] {{NullPointerException might be thrown as 'o' is nullable here}} flow@ex {{'o' is dereferenced.}}
     }
  }

  private boolean callTrueIfNull(Object a) {
    return trueIfNull(a); // flow@nested
  }

  void nestedTest(Object a) {
    if (callTrueIfNull(a)) { // flow@nested
      a.toString(); // Noncompliant [[flows=nested]] flow@nested
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
    Object o = sundayIsAGoodDay(); // flow@return {{'sundayIsAGoodDay()' returns null.}} flow@return {{'o' is assigned null.}}
    o.toString(); // Noncompliant [[flows=return]] flow@return
  }

}

