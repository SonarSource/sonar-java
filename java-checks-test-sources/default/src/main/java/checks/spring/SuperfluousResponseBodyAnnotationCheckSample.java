package checks.spring;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuperfluousResponseBodyAnnotationCheckSample {
  @ResponseBody // Noncompliant {{Remove this superfluous "@ResponseBody" annotation.}}
//^^^^^^^^^^^^^
  @GetMapping("foo")
  public String get() {
    return "Hello world!";
  }

  @ResponseBody // Noncompliant
  @GetMapping("bar")
  private String get2() {
    return "Hello world!";
  }

  @GetMapping("baz")
  public String get3() {
    return "Hello world!";
  }

  class InnerClass {
    @ResponseBody // Compliant
    @GetMapping("foo")
    public String get() {
      return "Hello world!";
    }
  }
}

@Controller
class RegularController {
  @ResponseBody // Compliant
  @GetMapping("foo")
  public String get() {
    return "Hello world!";
  }

  @GetMapping("baz")
  public String get3() {
    return "Hello world!";
  }
}

class NotAController {
  @ResponseBody // Compliant
  @GetMapping("foo")
  public String get() {
    return "Hello world!";
  }

  @GetMapping("baz")
  public String get3() {
    return "Hello world!";
  }
}
