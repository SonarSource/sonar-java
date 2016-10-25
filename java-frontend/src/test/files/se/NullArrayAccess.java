class NullArrayAccess {
  public void testCheckNotNullArray() {
    Object[] array = checkForNullMethod();
    array[0] = new Object(); // Noncompliant
    int x = array.length; // compliant :  unreachable.
  }

  @CheckForNull
  private Object[] checkForNullMethod() {
    return null;
  }
}
