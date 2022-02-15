package checks;

import java.util.Iterator;

class HasNextCallingNextCheck implements Iterator<String> {

  private Iterator<String> other;

  public boolean hasNext() {
    boolean b = false;
    b = next() != null; // Noncompliant [[sc=9;ec=13]] {{Refactor the implementation of this "Iterator.hasNext()" method to not call "Iterator.next()".}}
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
  public String otherMethod() { return ""; }
}

// Not Iterator
class HasNextCallingNextCheck_B {

  private HasNextCallingNextCheck iterator;

  public boolean hasNext() {
    boolean b = false;
    b = next() != null;
    b = iterator.next() != null;
    return true;
  }

  public String next() { return "a"; }
  public void remove() {}
}

class HasNextCallingNextCheck_C implements Iterator<String> {
  public boolean hasNext() { return true; }
  public String next() { return "a"; }
  public void remove() {}
}

class HasNextCallingNextCheck_D extends HasNextCallingNextCheck_C {
  public boolean hasNext() {
    boolean b = false;
    b = next() != null; // Noncompliant
    return true;
  }
}

class HasNextCallingNextCheck_E extends HasNextCallingNextCheck_C {
  public boolean hasNext() {
    class Internal extends HasNextCallingNextCheck_C {
      public void myMethod() {
        next();
      }
    }
    return true;
  }
}

abstract class HasNextCallingNextCheck_F implements Iterator<String> {
  public abstract boolean hasNext();
  public String next() { return "a"; }
  public void remove() {}
}

abstract class HasNextCallingNextCheck_FirstIterator implements Iterator<String> {

  private HasNextCallingNextCheck_OtherIterator otherIterator;

  @Override
  public boolean hasNext() {
    while (otherIterator.hasNext()) {
      otherIterator.next(); // Compliant
    }
    return true;
  }
}

abstract class HasNextCallingNextCheck_OtherIterator implements Iterator<String> {

  @Override
  public abstract String next();
}
