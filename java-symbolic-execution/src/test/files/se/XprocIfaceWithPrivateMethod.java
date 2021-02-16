

interface IfaceWithPrivateMethod {

  default void test() {
    privateMethod();
  }

  private void privateMethod() {

  }

}
