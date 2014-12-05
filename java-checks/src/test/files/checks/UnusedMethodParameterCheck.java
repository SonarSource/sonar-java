class A extends B{
  void doSomething(int a, int b) {     // "b" is unused
    compute(a);
  }

  void doSomething(int a) {
    compute(a);
  }

  @Override
  void doSomethingElse(int a, int b) {     // no issue reported on b
    compute(a);
  }
}

class B {
  void doSomethingElse(int a, int b) {
    compute(a);
    compute(b);
  }
  void compute(int a){
    a++;
  }
}

class C extends B {
  int bar;
  void doSomethingElse(int a, int b) {     // no issue reported on b
    compute(a);
  }
  void foo(int a) {
    compute(a);
  }
}

class D extends C {
  void foo(int b, int a) {
    System.out.println("");
  }
}
class E extends C {
  void bar(int a){
    System.out.println("");
  }
}
interface inter {
  default void foo(int a) {
    compute(a);
  }
  default void bar(int a) { System.out.println("");}
  void qix(int a);
}
class F {
  public static void main(String[] args) { }
  public static int main(String[] args) { System.out.println("");}
  public static void main(int[] args) { System.out.println("");}
  public static Object main(String[] args) { System.out.println("");}
  public static void main(String args) { System.out.println("");}
  public static void main(Double[] args) { System.out.println("");}
}

class G implements inter {
  void foo(int a) {
    System.out.println("plop");
  }
  private void writeObject(ObjectOutputStream out)
      throws IOException {
    throw new NotSerializableException(getClass().getName());
  }

  private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    throw new NotSerializableException(getClass().getName());
  }
}

class OpenForExtension {
  public foo(int arg) {
    //no-op
  }
  protected bar(int arg) {
    //no-op
  }
  public void qix(int arg) {
    throw new UnsupportedOperationException("not implemented");
  }

  private baz(int arg) { //Noncompliant
    //no-op
  }

  public Supplier<String> parameterNotUsed(final Object o) {
    return o::toString;
  }

}