abstract class A {
  private int x;
  private int y;
  private boolean started;
  protected String name;
  private String lastName;

  public void setX(int val) { // Noncompliant {{Refactor this setter so that it actually refers to the field "x".}}
    this.y = val;
  }

  public int getY() { // Noncompliant  {{Refactor this getter so that it actually refers to the field "y".}}
    return this.x;
  }

  public String getLastName() {  // Noncompliant  {{Refactor this getter so that it actually refers to the field "lastName".}}
    return name;
  }

  public boolean isStarted() {
    return started;
  }

  public boolean getStarted() {
    return isStarted();
  }

  int get() {
    return 0;
  }

  boolean is() {
    return true;
  }

  private void set(int i) {

  }

  Set<Integer> getSet() {
    return MAX;
  }

  private static final int MAX = 42;

  int getMax() {
    return MAX;
  }

  void setMax(int max) {
    return;
  }

  public abstract double getZ();
  public abstract void setZ(double z);

  boolean compute() {
    return false;
  }

  boolean compute2(int arg) {
    return false;
  }

  boolean setAndReturnValue(double status) {
    return started;
  }
}


class B extends A {

  public String getName() {
    return name;
  }

}

public class C {

  private String field;
  private boolean isField;

  boolean isField() { // compliant, we already return the correct value, "field" is simply not used in this context
    return isField;
  }

  private B someB;
  private boolean someOtherB;

  A getSomeB() { // compliant
    return someB;
  }
}
