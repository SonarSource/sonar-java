import java.util.Iterator;

class A implements Iterator<String> {

  private Iterator<String> other;
  
  public boolean hasNext() {
    boolean b = false;
    b = next() != null; // Noncompliant
    b = this.next() != null; // Noncompliant
    b = other.next() != null;
    b = next("a") != null;
    b = otherMethod() != null;
    return true;
  }
  
  public boolean hasNext(String x) {
    return next() != null;
  }
  
  public void remove() {}
  public String next() { return "a"; }
  public String next(String a) { return "a"; }
  public String otherMethod() {}
}

// Not Iterator
class B {

  private A iterator;

  public boolean hasNext() {
    boolean b = false;
    b = next() != null;
    b = iterator.next() != null;
    return true;
  }
  
  public String next() { return "a"; }
  public void remove() {}  
}

class C implements Iterator<String> {
  public boolean hasNext() { return true; }  
  public String next() { return "a"; }
  public void remove() {}  
}

class D extends C {
  public boolean hasNext() {
    boolean b = false;
    b = next() != null; // Noncompliant
    return true;
  }
}

class E extends C {
  public boolean hasNext() {
    class Internal extends C {
      public void myMethod() {
        next();
      }      
    }
    return true;
  }
}

abstract class F implements Iterator<String> {
  public abstract boolean hasNext();  
  public String next() { return "a"; }
  public void remove() {}  
}
