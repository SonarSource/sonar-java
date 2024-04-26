package checks.spring;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
class S3751_HelloWorld {

  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { return ""; } // Compliant

  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { return ""; } // Noncompliant {{Make this method non "private".}}
//               ^^^^^

  @GetMapping public String a() { return ""; }
  @GetMapping private String a1() { return ""; } // Noncompliant
  @PostMapping public String b() { return ""; }
  @PostMapping private String b1() { return ""; } // Noncompliant
  @PutMapping public String c() { return ""; }
  @PutMapping private String c1() { return ""; } // Noncompliant
  @DeleteMapping public String d() { return ""; }
  @DeleteMapping private String d1() { return ""; } // Noncompliant
  @PatchMapping public String e() { return ""; }
  @PatchMapping private String e1() { return ""; } // Noncompliant

  public String publicMethodNoAnnotation() { return ""; }
  private String privateMethodNoAnnotation() { return ""; }

}

@RestController
class S3751_Foo {
  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { return ""; }// Compliant

  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { return ""; } // Noncompliant {{Make this method non "private".}}
//               ^^^^^

  @GetMapping public String a() { return ""; }
  @GetMapping private String a1() { return ""; } // Noncompliant
  @PostMapping public String b() { return ""; }
  @PostMapping private String b1() { return ""; } // Noncompliant
  @PutMapping public String c() { return ""; }
  @PutMapping private String c1() { return ""; } // Noncompliant
  @DeleteMapping public String d() { return ""; }
  @DeleteMapping private String d1() { return ""; } // Noncompliant
  @PatchMapping public String e() { return ""; }
  @PatchMapping private String e1() { return ""; } // Noncompliant

  public String publicMethodNoAnnotation() { return ""; }
  private String privateMethodNoAnnotation() { return ""; }

}

class S3751_NonSpringComponentClazz {
  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { return ""; } // Compliant

  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { return ""; } // Compliant
}
