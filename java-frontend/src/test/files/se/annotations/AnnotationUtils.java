package org.foo;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class A {

  @MyAnnotation void foo() { }

  @Nullable void nullable1() { }
  @CheckForNull void nullable2() { }
  @org.jetbrains.annotations.Nullable void nullable3() { }

  @Nonnull void nonnull1() { }
  @org.jetbrains.annotations.NotNull void nonnull2() { }
}

@interface MyAnnotation { }
