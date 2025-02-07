package checks.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Value;

public class StaticFieldInjectionNotSupportedCheckSample {

  @Target({ElementType.FIELD})
  @interface AnAnnotation {

  }

  @Target({ElementType.FIELD})
  @Autowired
  @interface MyAutowired {}

  @Component
  public class StaticInjectComponent {

    @Inject
    StaticInjectComponent(String s){}

    @Autowired
    StaticInjectComponent(String s, String v){}

    @javax.inject.Inject // Noncompliant {{Remove this injection annotation targeting the static field.}}
  //^^^^^^^^^^^^^^^^^^^^
    private static Integer staticInject1;

    @Value("aValue") // Noncompliant
    private static Integer staticValue;

    @Inject // Noncompliant
    public static String staticInject2;

    @Autowired // Noncompliant
    static String staticAutowired;

    @Autowired // Noncompliant
    @Value("aValue") // Noncompliant
    static String staticTwoAnnotation;

    @Value("aValue") // Noncompliant
    @AnAnnotation
    static String unrelatedAnnotation;

    @MyAutowired // FN, for now we don't handle transitive annotations, it affects @Autowired and @Value
    static String staticMyAutowired;

    @Value("aValue")
    public String instanceValue;

    @Inject
    public String instanceInject;

    @Autowired
    public String instanceAutowired;

    @Inject // Noncompliant {{Remove this injection annotation targeting the static method.}}
  //^^^^^^^
    static void staticInjectMethod(String s){}

    @Autowired // Noncompliant
    static void staticAutowiredMethod(String s){}

    @Value("aValue") // Noncompliant
    static void staticValueMethod(String s){}

    @Inject
    void instanceInjectMethod(String s){}

    @Autowired
    void instanceAutowiredMethod(String s){}

    @Value("aValue")
    void instanceValueMethod(String s){}

    static void staticValueParameter(@Value("aValue") String s){} // Noncompliant  {{Remove this injection annotation targeting the parameter.}}
  //                                 ^^^^^^^^^^^^^^^^
    static void staticAutowiredParameter(@Autowired String s){} // Noncompliant

    void instanceValueParameter(@Value("aValue") String s){}
  }


}
