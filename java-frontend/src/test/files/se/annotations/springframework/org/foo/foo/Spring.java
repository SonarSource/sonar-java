package org.foo.foo;

import org.springframework.lang.Nullable;

interface A {
  Object methodAndParametersNotAnnotated(Object notAnnotatedParam);
}

abstract class B {
  Object field1;
  @Nullable Object field2;
}