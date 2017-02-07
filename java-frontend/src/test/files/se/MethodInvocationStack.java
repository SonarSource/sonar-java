/*
 Methods starting with "test" are used to test method invocation. Comment before method declaration is used to define expected stack depth on checkPostStatement call
 */
abstract class MethodInvocationStack {

  private void void_0() {
  }

  // 1,1
  void test_void_0() {
    void_0();
  }

  private void void_1(int arg) {
  }

  // 1,2,1
  void test_void_1() {
    void_1(0);
  }

  private Object object_0() {
    return new Object();
  }

  // 1,1
  void test_object_0() {
    object_0();
  }

  private Object object_1(Object o) {
    return new Object();
  }

  // 1,2,1
  void test_object_1() {
    object_1(new Object());
  }

  private Object object_2(int i1, int i2) {
    return new Object();
  }

  // 1,2,3,1
  void test_object_2() {
    object_2(1, 2);
  }

  private void exception_0() {
    throw new RuntimeException();
  }

  // 1
  void test_exception_0() {
    exception_0();
  }

  abstract void no_yield();

  // 1,1
  void test_no_yield() {
    no_yield();
  }

  private void happy_and_exceptional() {
    if (c) throw new RuntimeException();
  }

  // 1,1
  void test_happy_and_exceptional() {
    happy_and_exceptional();
  }

  // 1,1
  void test_member_select() {
    this.void_0();  // this has the same behavior as calling without this, i.e. we don't have methodIdentifier on stack
  }

  // 1,1,1,1
  void test_member_select2() {
    a.b.c.call();  // on stack we have [a],[b],[c],[return value]
  }

}
