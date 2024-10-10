class Foo implements Comparable<Foo> {

  @Override
  public int compareTo(Foo o) {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return false;
  }

}

class Foo2 implements Comparable<Foo2> {

  @Override
  public int compareTo(Foo2 o) { // Noncompliant {{Override "equals(Object obj)" to comply with the contract of the "compareTo(T o)" method.}}
//           ^^^^^^^^^
    return 0;
  }

}

class Foo3 implements Comparable<Foo3> {

  @Override
  public boolean equals(Object obj) {
    return false;
  }

}

class Foo4 implements Comparable<Foo4> {

  @Override
  public int compareTo() {
    return 0;
  }

}

class Foo5 implements Comparable<Foo5> {

  @Override
  public int compareTo(Foo5 o) { // Noncompliant
    return 0;
  }

  @Override
  public int equals() {
    return 0;
  }

}

class Foo6 implements Comparable<Foo6> {

  @Override
  public int compareTo(Foo6 o) { // Noncompliant
    return 0;
  }

  @Override
  public int foo(Object o) {
    return 0;
  }

}

class Foo7 implements Comparable<Foo7> {

  ;

}

public class Timestamp extends java.util.Date {
  public boolean equals(Timestamp ts) {
    return false;
  }

  public int compareTo(Timestamp ts) {
    return 0;
  }
}

enum Foo8 implements Comparable<Foo8> {
  ;

  @Override
  public int compareTo(Foo8 o) { // Noncompliant
    return 0;
  }

  @Override
  public int foo(Object o) {
    return 0;
  }

}

public class Foo9 {

  public boolean equals(Foo9 o) {
    return false;
  }

  public int compareTo(Object o) {
    return 0;
  }

}

public interface Doc extends Comparable<Object> {

  int compareTo(Object obj);

}

public class Foo10 implements Doc {
  
}
