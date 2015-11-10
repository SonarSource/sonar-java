package java.lang;
public class HelloWorld {

  public void sayHello() { // Noncompliant [[effortToFix=1;sc=15;ec=23;secondary=4,5]] {{The Cyclomatic Complexity of this method "sayHello" is 2 which is greater than 1 authorized.}}
    while (false) {
    }
  }

  public void sayHello2() { // Noncompliant [[effortToFix=3;sc=15;ec=24;secondary=9,10,14,15]] {{The Cyclomatic Complexity of this method "sayHello2" is 4 which is greater than 1 authorized.}}
    while (false) {
    }
    return
      a
      || b
      && c;
  }

  public boolean equals(Object o) {
    while (false) {
    }
    return
      a
        || b
        && c;
  }

  public boolean equals(Object o, Object other) { // Noncompliant
    while (false) {
    }
    return
      a
        || b
        && c;
  }

  public int hashCode() {
    while (false) {
    }
    if (
      a
        || b
        && c) {
      return 100;
    }
    return 42;
  }

}
