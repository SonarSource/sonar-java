package org.foo;

import org.eclipse.jdt.annotation.DefaultLocation;

@javax.annotation.ParametersAreNonnullByDefault
interface Javax1 {
  void nonNull(Object p1);
}

@javax.annotation.ParametersAreNullableByDefault
interface Javax2 {
  void nullable(Object p1);
}

@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
abstract class Spring {
  Object nonNullField;
  abstract void nonNull(Object p1);
  Object returnType();
}

@com.mongodb.lang.NonNullApi
interface MongoDB {
  void nonNull(Object p1);
  Object returnType();
}

@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.PARAMETER)
interface Eclipse1 {
  void nonNull(Object p1);
}

@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
interface Eclipse2 {
  Object returnType();
}

@org.eclipse.jdt.annotation.NonNullByDefault({DefaultLocation.RETURN_TYPE, DefaultLocation.PARAMETER})
interface Eclipse3 {
  void nonNull(Object p1);
  Object returnType();
}

@org.eclipse.jdt.annotation.NonNullByDefault
interface Eclipse4 {
  void nonNull(Object p1);
  Object returnType();
}