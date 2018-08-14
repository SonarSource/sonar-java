package com.mycompany.myproject;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import javax.inject.Inject;
import javax.annotation.Resource;

@Controller
public class MyController {

  @interface MyInjectionAnnotation {}

  @interface MyUnrelatedAnnotation {}

  String field1 = null; // Noncompliant

  @MyInjectionAnnotation
  String field2 = null;

  @MyUnrelatedAnnotation
  String field3 = null; // Noncompliant

  @Autowired
  String field4 = null;
}
