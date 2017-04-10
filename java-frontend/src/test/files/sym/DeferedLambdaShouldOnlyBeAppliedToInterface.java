class A{
  private void foo(int x) {
    foo(() -> System.out.println("Hello"));
  }

  private void foo(Runnable action) {
    action.run();
  }
  private void foo(Object o) {

  }
}
