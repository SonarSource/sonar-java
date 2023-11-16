package checks;

class RunFinalizersCheck {
  void foo(){
    Runtime.runFinalizersOnExit(true); // Noncompliant [[sc=13;ec=32]] {{Remove this call to "Runtime.runFinalizersOnExit()".}}
    System.runFinalizersOnExit(false); // Noncompliant
  }
}
