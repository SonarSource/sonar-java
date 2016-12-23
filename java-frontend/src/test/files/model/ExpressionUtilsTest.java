class SimpleAssignments {

  Object myField;

  public void mixedReference() {
    myField = null; // Simple
    this.myField = null; // Simple
    this.getOtherInstance().myField = null; // Not simple
  }

  public SimpleAssignments getOtherInstance() {
    return new SimpleAssignments();
  }
}
