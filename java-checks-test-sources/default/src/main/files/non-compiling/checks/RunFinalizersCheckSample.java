package checks;

class RunFinalizersCheckSample {
  void foo(){
    Runtime.runFinalizersOnExit(true); // Noncompliant {{Remove this call to "Runtime.runFinalizersOnExit()".}}
//          ^^^^^^^^^^^^^^^^^^^
    System.runFinalizersOnExit(false); // Noncompliant
  }
}
