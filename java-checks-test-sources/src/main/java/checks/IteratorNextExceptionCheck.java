package checks;

import java.util.Iterator;
import java.util.LinkedList;
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

class IteratorNextExceptionCheckM2 implements PrimitiveIterator.OfInt {
  Iterator<Character> iterator;
  @Override
  public int nextInt() {
    return iterator.next().hashCode(); // Compliant
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }
}

/**
 * FALSE POSITIVE below. We currently are not able to tell which methods outside of this file (like `removeFirst`)
 * can throw `NoSuchElementException`. The case below is actually compliant.
 */
class IteratorNextExceptionCheckM3 implements Iterator<String> {
  LinkedList<String> list;
  @Override
  public String next() { // Noncompliant
    return list.removeFirst();
  }

  @Override
  public boolean hasNext() {
    return list.isEmpty();
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

class IteratorNextExceptionCheckO implements Iterator<String> {
  private int count = 10;

  public String next() { // Compliant
    return getNext();
  }

  private String getNext() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return "hello";
  }

  public static void justThrow() {
    throw new NoSuchElementException();
  }

  @Override
  public boolean hasNext() {
    count--;
    return count > 0;
  }

}

class IteratorNextExceptionCheckP implements Iterator<T> {
  private T elem;

  public T next() { // Compliant
    if (!hasNext()) {
      IteratorNextExceptionCheckO.justThrow();
    }
    return elem;
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckQ implements Iterator<T> {
  private T elem;

  public T next() { // Compliant
    if (!hasNext()) {
      class Foo extends NoSuchElementException {}
      throw new Foo();
    }
    return elem;
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckR implements Iterator<T> {
  public T next() { // Noncompliant
    return recurseA();
  }

  public T recurseA() {
    return recurseB();
  }

  public T recurseB() {
    return recurseA();
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckS implements Iterator<T> {
  private T elem;

  public T next() { // Compliant
    return a();
  }

  private T a() {
    return b();
  }

  private T b() {
    return false ? c() : d();
  }

  private T c() {
    return elem;
  }

  private T d() {
    throw new NoSuchElementException();
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

/**
 * FALSE NEGATIVE below. We currently do not handle try-catch statements. In the example below, the expected exception
 * is caught when it should not be. The example is actually non-compliant
 */
class IteratorNextExceptionCheckT implements Iterator<T> {
  private T elem;

  public T next() { // Compliant, FN
    try {
      return getNext();
    } catch (NoSuchElementException e) {
      return elem;
    }
  }

  private T getNext() {
    throw new NoSuchElementException();
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}
