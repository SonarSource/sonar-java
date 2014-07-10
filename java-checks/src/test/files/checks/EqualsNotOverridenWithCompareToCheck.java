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

class Foo2 implements Comparable<Foo> {

  @Override
  public int compareTo(Foo o) {           // Non-Compliant
    return 0;
  }

}

class Foo3 implements Comparable<Foo> {

  @Override
  public boolean equals(Object obj) {
    return false;
  }

}

class Foo4 implements Comparable<Foo> {

  @Override
  public int compareTo() {
    return 0;
  }

}

class Foo5 implements Comparable<Foo> {

  @Override
  public int compareTo(Foo o) {           // Non-Compliant
    return 0;
  }

  @Override
  public int equals() {
    return 0;
  }

}

class Foo6 implements Comparable<Foo> {

  @Override
  public int compareTo(Foo o) {           // Non-Compliant
    return 0;
  }

  @Override
  public int foo(Object o) {
    return 0;
  }

}

class Foo7 implements Comparable<Foo> {

  ;

}

enum Foo8 implements Comparable<Foo> {
  ;

  @Override
  public int compareTo(Foo o) {           // Non-Compliant
    return 0;
  }

  @Override
  public int foo(Object o) {
    return 0;
  }

}

public interface Doc extends Comparable<Object> {

  int compareTo(Object obj);

}