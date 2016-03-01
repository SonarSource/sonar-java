/*
 * Header
 */

/**
 * Javadoc
 */
@SuppressWarnings("unused")
class Example {

  @java.lang.Deprecated
  int method() {
    return 42;
  }

}

@interface Example2 {
  Example3 method() default 0;
}
