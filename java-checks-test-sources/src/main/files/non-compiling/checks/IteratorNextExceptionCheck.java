package checks;

import java.util.Iterator;
import java.util.NoSuchElementException;

class IteratorNextExceptionCheckA implements Iterator<String> {

  public String next() {
    unknownMethod();
    return throwsNoSuchElementException();
  }

  public String throwsNoSuchElementException() throws NoSuchElementException {
    throw new NoSuchElementException();
  }

  @Override
  public boolean hasNext() {
    return false;
  }
}

class IteratorNextExceptionCheckB implements Iterator<String> {

  public String next() { // Noncompliant
    if (!hasNext()){
      return null;
    }
    return "x";
  }
  @Override
  public boolean hasNext() {
    return false;
  }
}

class IteratorNextExceptionCheckC implements Unknown {

  public String next() { // Compliant, Unknown parent of enclosing class
    if (!hasNext()){
      return null;
    }
    return "x";
  }
  @Override
  public boolean hasNext() {
    return false;
  }
}

class IteratorNextExceptionCheckD implements Iterator<String> {

  public String next() { // Compliant, unknown method call in the body could throw the exception
    unknownMethod();
    return "something";
  }

  @Override
  public boolean hasNext() {
    return false;
  }
}

class IteratorNextExceptionCheckD2 implements Iterator<String> {

  Iterator<Unknown> delegate;

  public String next() { // Compliant, unknown method call in the body could throw the exception
    delegate.next();
    return "something";
  }

  @Override
  public boolean hasNext() {
    return false;
  }
}

class IteratorNextExceptionCheckE implements Iterator<String> {

  public String next() { // Compliant
    throw new SomethingUnknown();
  }

  @Override
  public boolean hasNext() {
    return false;
  }
}
