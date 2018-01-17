package org.foo.bar;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;

interface A {
  @NonNullByDefault
  Object nonnullParameters1(Object nonnullParameter);

  @NonNullByDefault(DefaultLocation.PARAMETER)
  Object nonnullParameters2(Object notnonNullParameter);

  @NonNullByDefault({DefaultLocation.PARAMETER, RETURN_TYPE})
  Object nonnullParameters3(Object nonnullParameter);

  @NonNullByDefault(value = {DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE})
  Object nonnullParameters4(Object nonnullParameter);

  @NonNullByDefault(RETURN_TYPE)
  Object notNonnullParameters1(Object notNonnullParameter);
  Object notNonnullParameters2(Object notNonnullParameter);
}

@NonNullByDefault
abstract class B {
  Object field;
  abstract Object nonnullParameters(Object nonnullParameter);
}
