import foo.SuppressWarnings;

class JavaFilesCacheTestWithSuppressWarnings {

  @SuppressWarnings // this annotation will be resolved to java.lang.SuppressWarnings as 'foo.SuppressWarnings' can not be resolved
  static class A {
  }

  @java.lang.SuppressWarnings("foo")
  static class C {
  }

  private static final String FOO = "foo";

  @java.lang.SuppressWarnings(FOO)
  static class D {
  }
}
