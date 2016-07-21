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

// inline comment
@interface Example2 {
  Example3 method() default 0;
}
