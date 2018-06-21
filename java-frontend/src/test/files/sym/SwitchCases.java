enum MyEnum {
  A,
  B,
  C
}

abstract class A {

  void foo(MyEnum v) {
    switch (v) {
      case A:
        bar();
        break;
      case B:
        bar();
        break;
      case UNKNOWN: // not part of enum, should not be resolved
        bar();
        break;
      default:
        break;
    }
  }

  void foo(java.util.concurrent.TimeUnit tu) {
    switch (tu) {
      case DAYS:
        bar();
        break;
      case HOURS:
        bar();
        break;
      default:
        break;
    }
  }

  abstract void bar();
}
