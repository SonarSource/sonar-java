package checks.spring;


import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class SomeTestClass {
  void NO_camel_case() {}
}

@interface MyAnnotationWithValue {
  String value();
}

public class SpringBeanNamingConventionCheckSample {

  @Bean("my_bean") // Noncompliant {{Rename this bean to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
//      ^^^^^^^^^
  SomeTestClass myBean1() {
    return null;
  }

  @Bean(name = "MyBean") // Noncompliant
  SomeTestClass myBean2() {
    return null;
  }

  @Bean(autowire = Autowire.NO, initMethod = "toString", name = "my_bean", destroyMethod = "toString") // Noncompliant
//                                                       ^^^^^^^^^^^^^^^^
  SomeTestClass myBean3() {
    return null;
  }

  @Bean(initMethod = "NO_camel_case", value = "my_bean") // Noncompliant
  SomeTestClass myBean5() {
    return null;
  }

  @Bean("1bean") // Noncompliant
  SomeTestClass myBean8() {
    return null;
  }

  public final static String MY_BEAN = "my_bean";
  @Bean(MY_BEAN) // Noncompliant
  SomeTestClass myBean10() {
    return null;
  }

  @Bean(initMethod = "NO_camel_case") // Compliant, we are only interested in the bean name
  SomeTestClass myBean4() {
    return null;
  }

  @Bean(initMethod = "NO_camel_case", name = "myBean") // Compliant, name is camel-cased
  SomeTestClass myBean6() {
    return null;
  }

  @Bean("a") // Compliant, name is camel-cased
  SomeTestClass myBean7() {
    return null;
  }

  @Bean // Compliant, no name provided
  SomeTestClass myBean9() {
    return null;
  }

  public final static String MY_BEAN2 = "myBean";
  @Bean(MY_BEAN2) // Compliant, name is camel-cased
  SomeTestClass myBean11() {
    return null;
  }
}

@Configuration("my_config") // Noncompliant
class MyConfig1 {}

@Configuration(value = "my_Config") // Noncompliant
class MyConfig3 {}

@Configuration("myConfig") // Compliant
class MyConfig2 {}

@Configuration(value = "myConfig") // Compliant
class MyConfig4 {}

@Controller("my_controller") // Noncompliant
class MyController1 {}

@Controller("myController") // Compliant
class MyController2 {}

@Component("my_component") // Noncompliant
class MyComponent1 {}

@Component("myComponent") // Compliant
class MyComponent2 {}

@Qualifier("my_qualifier") // Noncompliant
class MyQualifier1 {}

@Qualifier("myQualifier") // Compliant
class MyQualifier2 {}

@Component("my_component") // Noncompliant
@Qualifier("my_qualifier") // Noncompliant
class MyComponentAndQualifier1 {}

@Repository("my_repository") // Noncompliant
class MyRepository1 {}

@Repository("myRepository") // Compliant
class MyRepository2 {}

@Service("my_service") // Noncompliant
class MyService1 {}

@Service("myService") // Compliant
class MyService2 {}

@RestController("my_rest_controller") // Noncompliant
class MyRestController1 {}

@RestController("myRestController") // Compliant
class MyRestController2 {}

@MyAnnotationWithValue("my_value") // Compliant, not an annotation of interest
class MyValue1 {}
