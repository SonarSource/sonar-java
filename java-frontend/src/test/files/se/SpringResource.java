// mimic spring package structure
package org.springframework.context;

import java.io.Closeable;

public interface ConfigurableApplicationContext extends Closeable {}

abstract class SpringResource {

  public static ConfigurableApplicationContext run() {
    return null;
  }

  void foo() {
    SpringResource.run(); // Compliant
  }
}
