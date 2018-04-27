package org.foo.bar;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;

interface A {
  @NonNullByDefault
  Object nonNullParametersReturnNonNull1(Object nonNullParameter);

  @NonNullByDefault(DefaultLocation.PARAMETER)
  Object nonNullParameters2(Object noConstraintParameter);

  @NonNullByDefault({DefaultLocation.PARAMETER, RETURN_TYPE})
  Object nonNullParametersReturnNonNull3(Object nonNullParameter);

  @NonNullByDefault(value = {DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE})
  Object nonNullParametersReturnNonNull4(Object nonNullParameter);

  //should be annotated with @NonNullByDefault from package-info
  Object nonNullParametersReturnNonNull5(Object noConstraintParameter);

  @NonNullByDefault(RETURN_TYPE)
  Object returnNonNull(Object noConstraintParameter);
}

@NonNullByDefault
abstract class B {
  Object field;
  abstract Object nonNullParametersReturnNonNull(Object nonNullParameter);
}

interface C {
  public @Nullable String getStringNullable();
}
