class A {

  void foo() {
    switch (0) {
      case 0:
      default: // Noncompliant [[sc=7;ec=15]] {{Move this default to the end of the switch.}}
        break;
      case 1:
        break;
    }

    switch (0) {
      case 0: // Compliant
        break;
    }

    switch (0) {
      default: // Compliant
        break;
    }

    switch (0) {
      case 0:
        break;
      default: // Noncompliant
        break;
      case 1:
        break;
    }

    switch (0) {
      default: // Compliant
      case 0:
        break;
      case 1:
        break;
    }

    switch (0) {
      default: // Noncompliant
        break;
      case 0:
        break;
      case 1:
        break;
    }
    
    switch (0) {
      case 0:
        break;
      case 1:
        break;
      case 2:
      default:  // Compliant default does not affect the normal execution of the switch, as it requires to match 3
      case 3:
        break;
    }
  }
}
