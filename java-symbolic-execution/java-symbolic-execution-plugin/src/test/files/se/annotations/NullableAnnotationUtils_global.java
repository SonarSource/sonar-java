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
  abstract Object returnType();
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

@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.FIELD)
abstract class Eclipse3 {
  Object nonNullField;
}

@org.eclipse.jdt.annotation.NonNullByDefault({DefaultLocation.RETURN_TYPE, DefaultLocation.PARAMETER, DefaultLocation.FIELD})
abstract class Eclipse4 {
  Object nonNullField;
  abstract void nonNull(Object p1);
  abstract Object returnType();
}

@org.eclipse.jdt.annotation.NonNullByDefault
abstract class Eclipse5 {
  Object nonNullField;
  abstract void nonNull(Object p1);
  abstract Object returnType();
}