package checks;

public class StringLiteralDuplicatedCheckCustom {
  public void f() {
    System.out.println("aaaaa");
    System.out.println("bbbbb"); // Noncompliant {{Define a constant instead of duplicating this literal "bbbbb" 2 times.}}
    System.out.println("bbbbb");
    System.out.println("ccccc"); // Noncompliant {{Define a constant instead of duplicating this literal "ccccc" 3 times.}}
    System.out.println("ccccc");
    System.out.println("ccccc");
    System.out.println("dddd");
    System.out.println("dddd");
  }
}
