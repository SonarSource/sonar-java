import java.lang.Override;
import java.lang.String;

class A {

  void foo() {
    return 1; // Noncompliant [[sc=12;ec=13]] {{Remove this method and declare a constant for this value.}}
  }
  void foo() {
    return ""; // Noncompliant [[sc=12;ec=14]] {{Remove this method and declare a constant for this value.}}
  }
  void foo() {
    return ''; // Noncompliant [[sc=12;ec=14]] {{Remove this method and declare a constant for this value.}}
  }
  void foo() {
    System.out.println("foo");
    return 1;
  }
  abstract void foo();
  void foo(){
    return;
  }
  void foo(){
    System.out.println("");
  }

  @Override
  public String toString() {
    return "";  // compliant, this method is an override
  }
  void foo() {
    return 1L; // Noncompliant [[sc=12;ec=14]] {{Remove this method and declare a constant for this value.}}
  }
}