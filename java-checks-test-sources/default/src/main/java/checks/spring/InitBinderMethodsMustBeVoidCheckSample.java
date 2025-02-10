package checks.spring;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

public class InitBinderMethodsMustBeVoidCheckSample {
  @interface MyAnnotation {}

  @Controller
  public class MyController {
    MyController(){}

    @InitBinder()
    public String nonVoid(WebDataBinder binder) { // Noncompliant {{Methods annotated with @InitBinder must return void.}}
  //              ^^^^^^^
      return "OK";
    }


    @InitBinder()
    @MyAnnotation()
    public String nonVoidWithAnnotation(WebDataBinder binder) { // Noncompliant {{Methods annotated with @InitBinder must return void.}}
  //              ^^^^^^^^^^^^^^^^^^^^^
      return "OK";
    }


    @InitBinder()
    public void voidMethod(){}

    @InitBinder()
    @MyAnnotation
    public void voidMethodWithAnnotation(){}
  }
}
