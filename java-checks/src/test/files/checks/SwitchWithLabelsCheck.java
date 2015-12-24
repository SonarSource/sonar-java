class A {
  public void f() {
    switch (0) {
      case 0:
        break;
      case 1:
        break;
      foo: // Noncompliant [[sc=7;ec=10]] {{Remove this misleading "foo" label.}}
        break;
      bar: // Noncompliant
        break;
      case 2:
        int a = 0;
        break;
      default:
        break;
    }
  }
}
