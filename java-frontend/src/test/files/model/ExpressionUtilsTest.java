class SimpleAssignments {

  Integer myField;

  public void mixedReference() {
    myField = null; // Simple
    this.myField = null; // Simple
    this.getOtherInstance().myField = null; // Not simple
    this.myField /= 5; // Not simple
  }

  public SimpleAssignments getOtherInstance() {
    return new SimpleAssignments();
  }
}
