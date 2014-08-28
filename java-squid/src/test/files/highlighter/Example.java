/*
 * Header
 */

/**
 * Javadoc
 */
@SuppressWarnings("unused")
class Example {

  int method() {
    return 42;
  }

}

@interface Example2 {
  Example3 method() default 0;
}
