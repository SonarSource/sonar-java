package checks;

public class DeadStoreCheckBrokenSemantic {

  public void singleAssignment() {
    int a = 42;
    System.out.println(a);
  }
}
