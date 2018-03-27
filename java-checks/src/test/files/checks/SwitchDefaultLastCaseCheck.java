class A {
  
  void foo() {
    switch (0) {
    default:   // Noncompliant [[sc=5;ec=13]] {{Move this default to the end of the switch.}}
    case 0:
      break;
    }
  
    switch(0) {
      case 0:     // Compliant
        break;
    }
    
    switch(0) {
      default:  // Compliant
        break;
    }
    
    switch(0) {
      case 0:
        break;
      default:  // Noncompliant
        break;
      case 1:
        break;
    }
  }
}
