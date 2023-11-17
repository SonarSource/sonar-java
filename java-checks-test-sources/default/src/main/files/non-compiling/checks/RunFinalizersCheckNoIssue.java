package checks;

class RunFinalizersCheckNoIssue {
  void foo(){
    Runtime.runFinalizersOnExit(true);
    System.runFinalizersOnExit(false);
  }
}
