class A{
  void foo(){
    Runtime.runFinalizersOnExit(true);
    System.runFinalizersOnExit(false);
  }
}
