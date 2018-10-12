import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@SessionAttributes("foo") // Noncompliant [[sc=2;ec=19]] {{Add a call to "setComplete()" on the SessionStatus object in a "@RequestMapping" method.}}
public class Foo {
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
public class Foo2 {
  @RequestMapping(value = "/end", method = RequestMethod.GET)
  public void baz(SessionStatus status) {
    status.setComplete();
  }
}

@Controller
@SessionAttributes("foo")
public class Foo3 {
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
public class Bar {
  @RequestMapping(method = RequestMethod.POST)
  public void bar(SessionStatus status) {
    status.setComplete();
  }
}

@Controller
@SessionAttributes("x")
public class Baw {
  @PostMapping
  public void baw(SessionStatus status) {
    status.setComplete();
  }
}

@SessionAttributes("foo")
public class Boo { // not a controller
  @RequestMapping("/foo")
  public String boo(String foo) {
    return "foo" + foo;
  }
}

public abstract class AbstractController {
  @PostMapping
  public void end(SessionStatus status) {
    status.setComplete();
  }
}

@Controller
@SessionAttributes("foo") // Noncompliant, FP
public class ResultControllerHosting extends AbstractController {

}

public class X { // not a controller
  @RequestMapping("/foo")
  public String boo(String foo) {
    return "foo" + foo;
  }
}
