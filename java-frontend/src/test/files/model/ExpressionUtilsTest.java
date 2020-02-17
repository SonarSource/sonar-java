class SimpleAssignments {

  Integer myField;

  public void mixedReference() {
    myField = getSomething(); // Simple
    this.myField = getSomething(); // Simple
    this.getOtherInstance().myField = getSomething(); // Not simple
    this.myField /= getSomething(); // Not simple

    SimpleAssignments simpleAssignments1 = new SimpleAssignments();
    simpleAssignments.myField = getSomething();

    SimpleAssignments simpleAssignments2 = getOtherInstance();
  }

  public SimpleAssignments getOtherInstance() {
    return new SimpleAssignments();
  }

  private int getSomething() {
    return 1;
  }
}
