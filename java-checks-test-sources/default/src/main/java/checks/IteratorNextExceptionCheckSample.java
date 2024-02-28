package checks;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PrimitiveIterator;

class IteratorNextExceptionCheckSampleA implements Iterator<String> {

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

class IteratorNextExceptionCheckSampleB implements Iterator<String> {

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

class IteratorNextExceptionCheckSampleC implements Iterator<String> {

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

class IteratorNextExceptionCheckSampleD implements Iterator<String> {

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

class IteratorNextExceptionCheckSampleE { // Not an iterator

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

abstract class IteratorNextExceptionCheckSampleF implements Iterator<String>{
  public abstract String next();
}

class IteratorNextExceptionCheckSampleG implements Iterator<String> {

  private Iterator<String> iter;

  public String next() {
    return iter.next();
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckSampleI implements Iterator<String> {

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

class IteratorNextExceptionCheckSampleJ implements Iterator<String> {
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
class IteratorNextExceptionCheckSampleK implements Iterator<String> {
  IteratorNextExceptionCheckSampleJ a;
  public String next() {
    return a.next(); // Compliant
  }
  @Override
  public boolean hasNext() {
    return false;
  }
}

abstract class IteratorNextExceptionCheckSampleL implements Iterator<String> {
  @Override
  public String next() { // Compliant
    return getOptional().orElseThrow(NoSuchElementException::new);
  }

  Optional<String> getOptional() {
    return Optional.empty();
  }
}

class IteratorNextExceptionCheckSampleM implements Iterator<String> {
  ListIterator<String> a;
  public String next() {
    return a.previous(); // Compliant
  }
  @Override
  public boolean hasNext() {
    return a.hasPrevious();
  }
}

class IteratorNextExceptionCheckSampleM2 implements PrimitiveIterator.OfInt {
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
class IteratorNextExceptionCheckSampleM3 implements Iterator<String> {
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

class IteratorNextExceptionCheckSampleN implements Iterator<Double> {
  PrimitiveIterator.OfDouble a;
  public Double next() {
    return a.nextDouble(); // Compliant
  }
  @Override
  public boolean hasNext() {
    return a.hasNext();
  }
}

class IteratorNextExceptionCheckSampleO implements Iterator<String> {
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

class IteratorNextExceptionCheckSampleP implements Iterator<T> {
  private T elem;

  public T next() { // Compliant
    if (!hasNext()) {
      IteratorNextExceptionCheckSampleO.justThrow();
    }
    return elem;
  }

  @Override
  public boolean hasNext() {
    return false;
  }

}

class IteratorNextExceptionCheckSampleQ implements Iterator<T> {
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

class IteratorNextExceptionCheckSampleR implements Iterator<T> {
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

class IteratorNextExceptionCheckSampleS implements Iterator<T> {
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
class IteratorNextExceptionCheckSampleT implements Iterator<T> {
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
