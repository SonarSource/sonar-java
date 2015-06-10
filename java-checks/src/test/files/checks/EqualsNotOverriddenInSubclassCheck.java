class A {
  static final String CONST = "constant";
  static String staticMember = "static member";
  String s1;

  public boolean equals(Object obj) {
    return true;
  }
}

class B extends A { // Noncompliant {{Override this superclass' "equals" method.}}
  String s2;
}

abstract class C {
  boolean equals;
  
  public abstract boolean equals(Object obj); 
}

class D extends C { // Compliant
  int i;

  @Override
  public boolean equals(Object obj) {
    return false;
  }
}

class E {
  public boolean equals() { 
    return true;
  }
}

class F extends E { // Compliant
  String s;
  
  public boolean equals(int i) {
    return true;
  }
}

class G {
  public boolean equals(Object obj) {
    return true;
  }
}

class H extends G { // Compliant
  String s;
  
  @Override
  public boolean equals(Object obj) {
    return false;
  }
}

class J {
  String s;
}

class K extends com.tst.UnknownClass {
  String s;
}

class L<T> extends com.tst.MyList<T> {
  int s;
}

class M {
  @Override
  public final boolean equals(Object obj) {
    return false;
  }
}

class N extends M { // Compliant - M.equals() is final
  int i;
}