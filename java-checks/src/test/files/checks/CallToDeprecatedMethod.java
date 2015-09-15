public class CallToDeprecatedMethodCheck {

  public CallToDeprecatedMethodCheck() {
    String string = new String("my string");
    string.getBytes(1, 1, new byte[3], 7); // Noncompliant {{Method 'String.getBytes(...)' is deprecated.}}
    new DeprecatedConstructor(); // Noncompliant {{Constructor 'DeprecatedConstructor(...)' is deprecated.}}
    new MyDeprecatedClass();
  }

  @Deprecated
  private static class MyDeprecatedClass {
  }

  private static class DeprecatedConstructor {
    @Deprecated
    public DeprecatedConstructor() {
    }
  }

}
