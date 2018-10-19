package org.foo.bar;

import org.springframework.lang.Nullable;

interface A {
  Object nonNullParametersReturnNonNull(Object nonNullParameter);
}

abstract class B {
  Object field;
}

interface C {
  public @Nullable String getStringNullable();
}
