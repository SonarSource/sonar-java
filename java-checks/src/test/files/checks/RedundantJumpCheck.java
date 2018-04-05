abstract class A {

  public void loops() {

    while (condition1) {
      foo();
      continue; // Noncompliant
    }

    while (condition1) {
      foo();
      break;
    }

    while (condition1) {
      if (condition2) {
        continue; // Noncompliant
      } else {
        foo();
      }
    }
  }

  public int return_in_non_void_method() {
    foo();
    return 42;
  }

  public void useless_return() {
    foo();
    return; // Noncompliant
  }

  public void void_method_with_useful_return_without_expression() {
    if (condition) {
      return;
    }
    foo();
  }

  public void switch_statements(int param) {
    switch (param) {
      case 0:
        foo();
        break;
      default:
    }
    foo();
    switch (param) {
      case 0:
        foo();
        return;
      case 1:
        bar();
        return;
    }
  }

  public void throwing_exception() {
    throw new UnsupportedOperationException();
  }

  public abstract void abstract_method();

}
