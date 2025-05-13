package checks;

public class DeadStoreCheckBrokenSemantic {

  public void singleAssignment() {
    int a = 42; // Noncompliant
    System.out.println(a);
  }
}
