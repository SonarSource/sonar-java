class InterfaceCycle {
  // This code does not compile as it contains a cycle
  public interface A extends B {}
  public interface B extends C {}
  public interface C extends A {}
}
