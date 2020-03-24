class C implements java.io.Serializable {

  private UnknownType b;

  private void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException {
    UnknownType a = b;
    stream.writeObject(a); // Compliant - type of a is unresolved
    b = a;
  }
}
