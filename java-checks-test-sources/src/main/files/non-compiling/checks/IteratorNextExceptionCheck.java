package checks;

import java.util.Iterator;
import java.util.NoSuchElementException;

class IteratorNextExceptionCheckH implements Iterator<String> {

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

class IteratorNextExceptionCheckA implements Iterator<String> {

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
