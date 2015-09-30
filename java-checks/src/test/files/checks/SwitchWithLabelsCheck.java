class A {
  public void f() {
    switch (0) {
      case 0:
        break;
      case 1:
        break;
      foo: // Noncompliant {{Remove this misleading "foo" label.}}
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
