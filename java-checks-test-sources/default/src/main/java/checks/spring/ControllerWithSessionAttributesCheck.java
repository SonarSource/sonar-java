package checks.spring;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@SessionAttributes("foo") // Noncompliant {{Add a call to "setComplete()" on the SessionStatus object in a "@RequestMapping" method.}}
^[sc=2;ec=19]
class S3753 {
  private int field;

  @RequestMapping("/foo")
  public String foo(String foo) {

    return "foo" + foo;
  }

  @RequestMapping(value = "/end", method = RequestMethod.POST)
  public void bar(SessionStatus status) {
    // no call to setComplete
    foo("foo");
    setComplete();
  }

  private void setComplete() { }
}

@Controller
@SessionAttributes("foo")
class S3753_2 {
  @RequestMapping(value = "/end", method = RequestMethod.GET)
  public void baz(SessionStatus status) {
    status.setComplete();
  }
}

@Controller
@SessionAttributes("foo")
class S3753_3 {
  @GetMapping
  public void baw(SessionStatus status) {
    help(status);
  }
  private void help(SessionStatus status) {
    status.setComplete();
  }
}

@Controller
@SessionAttributes("x")
class S3753_4 {
  @RequestMapping(method = RequestMethod.POST)
  public void bar(SessionStatus status) {
    status.setComplete();
  }
}

@Controller
@SessionAttributes("x")
class S3753_5 {
  @PostMapping
  public void baw(SessionStatus status) {
    status.setComplete();
  }
}

@SessionAttributes("foo")
class S3753_6 { // not a controller
  @RequestMapping("/foo")
  public String boo(String foo) {
    return "foo" + foo;
  }
}

abstract class S3753_AbstractController {
  @PostMapping
  public void end(SessionStatus status) {
    status.setComplete();
  }
}

@Controller
@SessionAttributes("foo") // Noncompliant
class S3753_ResultControllerHosting extends S3753_AbstractController { }

class S3753_X { // not a controller
  @RequestMapping("/foo")
  public String boo(String foo) {
    return "foo" + foo;
  }
}
