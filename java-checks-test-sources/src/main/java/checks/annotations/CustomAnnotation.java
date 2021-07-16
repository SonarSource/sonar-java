package checks.annotations;

public @interface CustomAnnotation {
  String field1() default "field1Default";
  String field2() default "field" + "2Default";
  String field3();
}
