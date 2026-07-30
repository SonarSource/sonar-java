package checks;

// Test file: no issues should be reported regardless of duplicated literals
public class StringLiteralDuplicatedCheckSampleTest {

  public void f() {
    System.out.println("ccccc");
    System.out.println("ccccc");
    System.out.println("ccccc");
  }

}
