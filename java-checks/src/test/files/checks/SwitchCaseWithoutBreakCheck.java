class A {
  private void f() {
    switch (myVariable) {
      case 0:
      case 1: // Non-Compliant
        System.out.println();
      case 2: // Compliant
        break;
      case 3: // Compliant
        return;
      case 4: // Compliant
        throw new IllegalStateException();
      case 5: // Non-Compliant
        System.out.println();
      default: // Non-Compliant
        System.out.println();
    }
  }
}
