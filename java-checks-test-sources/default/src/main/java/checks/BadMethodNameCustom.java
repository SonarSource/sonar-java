package checks;

class BadMethodNameCustom extends Bad {
  public BadMethodNameCustom() {
  }

  void Bad() {
  }

  void good() {
  }

  @Override
  void BadButOverrides(){
  }

  @Deprecated
  void Bad2() {
  }

  public String toString() { //Overrides from object
    return "...";
  }
}
