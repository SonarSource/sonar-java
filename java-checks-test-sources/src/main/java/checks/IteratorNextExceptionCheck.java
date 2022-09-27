package checks;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PrimitiveIterator;

class IteratorNextExceptionCheckA implements Iterator<String> {

  public String next() { // Noncompliant [[sc=17;ec=21]] {{Add a "NoSuchElementException" for iteration beyond the end of the collection.}}
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

class IteratorNextExceptionCheckB implements Iterator<String> {

  public String next() { // Noncompliant
    if (!hasNext()){
      throw new IllegalStateException();
    }
    return "x";
  }
  @Override
  public boolean hasNext() {
    return false;
  }
}

class IteratorNextExceptionCheckC implements Iterator<String> {

  public String next() {
    if (!hasNext()){
      throw new NoSuchElementException();
    }
    return "x";
  }

  public String next(String argument) {
    return "x";
  }

  public String notNext() {
    return "x";
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckD implements Iterator<String> {

  public String next() { // Noncompliant
    if (!hasNext()){
      RuntimeException e = new RuntimeException();
      throw e;
    }
    return "x";
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckE { // Not an iterator

  public String next() {
    if (!hasNext()){
      return null;
    }
    return "x";
  }

  public boolean hasNext() {
    return false;
  }

}

abstract class IteratorNextExceptionCheckF implements Iterator<String>{
  public abstract String next();
}

class IteratorNextExceptionCheckG implements Iterator<String> {

  private Iterator<String> iter;

  public String next() {
    return iter.next();
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckI implements Iterator<String> {

  public String next() { // Noncompliant
    return throwsIndexOutOfBoundsException();
  }

  public String throwsIndexOutOfBoundsException() throws IndexOutOfBoundsException {
    throw new IndexOutOfBoundsException();
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckJ implements Iterator<String> {
  public String next() {
    if (!hasNext()){
      throw new NoSuchElementException();
    }
    return "x";
  }
  @Override
  public boolean hasNext() {
    return false;
  }
}
class IteratorNextExceptionCheckK implements Iterator<String> {
  IteratorNextExceptionCheckJ a;
  public String next() {
    return a.next(); // Compliant
  }
  @Override
  public boolean hasNext() {
    return false;
  }
}

abstract class IteratorNextExceptionCheckL implements Iterator<String> {
  @Override
  public String next() { // Compliant
    return getOptional().orElseThrow(NoSuchElementException::new);
  }

  Optional<String> getOptional() {
    return Optional.empty();
  }
}

class IteratorNextExceptionCheckM implements Iterator<String> {
  ListIterator<String> a;
  public String next() {
    return a.previous(); // Compliant
  }
  @Override
  public boolean hasNext() {
    return a.hasPrevious();
  }
}

class IteratorNextExceptionCheckN implements Iterator<Double> {
  PrimitiveIterator.OfDouble a;
  public Double next() {
    return a.nextDouble(); // Compliant
  }
  @Override
  public boolean hasNext() {
    return a.hasNext();
  }
}
