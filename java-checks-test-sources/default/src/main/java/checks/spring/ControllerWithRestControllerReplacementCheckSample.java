package checks.spring;

import javax.annotation.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

public class ControllerWithRestControllerReplacementCheckSample {

  @Controller // Noncompliant [[quickfixes=qf1,qf2]]
//^^^^^^^^^^^
              // "@ResponseBody" annotations.}}
  // fix@qf2 {{Replace "@Controller" by "@RestController".}}
  // edit@qf2 [[sl=16;el=16;sc=3;ec=14]] {{@RestController}}
  class ClassOne {

    @ResponseBody
//  ^^^^^^^^^^^^^<
    // fix@qf1 {{Remove "@ResponseBody" annotations.}}
    // edit@qf1 [[sl=23;el=23;sc=5;ec=18]] {{}}
    public void m() {
    }
  }

  @Controller // Noncompliant
  @ResponseBody
  class ClassTwo {

    public void m() {
    }
  }

  @Controller // Noncompliant
  class ClassThree {

    @ResponseBody
    public void m() {
    }

    public void m2() {
    }
  }

  @Controller // Compliant
  class ClassFour {

    public void m() {
    }
  }

  @Controller // Compliant
  class ClassFive {

    @Nullable
    public void m() {
    }
  }

  @Service // Compliant
  class ClassSix {

    @ResponseBody
    public void m() {
    }
  }

  @Controller // Noncompliant [[quickfixes=qf3,qf4]]
//^^^^^^^^^^^
              // "@ResponseBody" annotations.}}
  // fix@qf4 {{Replace "@Controller" by "@RestController".}}
  // edit@qf4 [[sl=73;el=73;sc=3;ec=14]] {{@RestController}}
  class ClassSeven {

    @ResponseBody
//  ^^^^^^^^^^^^^<
    // fix@qf3 {{Remove "@ResponseBody" annotations.}}
    // edit@qf3 [[sl=80;el=80;sc=5;ec=18]] {{}}
    public void m() {
    }

    @ResponseBody
//  ^^^^^^^^^^^^^<
    // fix@qf3 {{Remove "@ResponseBody" annotations.}}
    // edit@qf3 [[sl=87;el=87;sc=5;ec=18]] {{}}
    public void m2() {
    }
  }

  @Controller // Noncompliant
  class GetAndResponseBody {
    String foo = "hello";

    @ResponseBody
    @GetMapping
    public void m() {}

    public void n() {}
  }

  @Controller // compliant
  class MixedResponseBodyAndGet {
    @GetMapping
    public void m() {}
    @ResponseBody
    public void n() {}
  }

  @Controller // compliant
  class MixedResponseBodyAndPost {
    @PostMapping
    public void m() {}
    @PostMapping
    @ResponseBody
    public void n() {}
  }

  @Controller // compliant
  class MixedResponseBodyAndRequest {
    @RequestMapping
    public void m() {}
    @ResponseBody
    public void n() {}
  }

  @Controller // compliant
  class MixedResponseBodyAndPatch {
    @PatchMapping
    public void m() {}
    @ResponseBody
    public void n() {}
    @ResponseBody
    public void k() {}
  }

  @Controller // compliant
  class MixedResponseBodyAndDelete {
    @DeleteMapping
    public void m() {}
    @ResponseBody
    public void n() {}
  }

  @Controller // compliant
  class MixedResponseBodyAndPut {
    @PutMapping
    public void m() {}
    @ResponseBody
    public void n() {}
  }

}
