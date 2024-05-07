package checks.spring;

import javax.annotation.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

public class ControllerWithRestControllerReplacementCheck {

  @Controller // Noncompliant [[quickfixes=qf1,qf2]]
//^^^^^^^^^^^
              // "@ResponseBody" annotations.}}
  // fix@qf2 {{Replace "@Controller" by "@RestController".}}
  // edit@qf2 [[sl=10;el=10;sc=3;ec=14]] {{@RestController}}
  class ClassOne {

    @ResponseBody
//  ^^^^^^^^^^^^^<
    // fix@qf1 {{Remove "@ResponseBody" annotations.}}
    // edit@qf1 [[sl=17;el=17;sc=5;ec=18]] {{}}
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
  // edit@qf4 [[sl=67;el=67;sc=3;ec=14]] {{@RestController}}
  class ClassSeven {

    @ResponseBody
//  ^^^^^^^^^^^^^<
    // fix@qf3 {{Remove "@ResponseBody" annotations.}}
    // edit@qf3 [[sl=74;el=74;sc=5;ec=18]] {{}}
    public void m() {
    }

    @ResponseBody
//  ^^^^^^^^^^^^^<
    // fix@qf3 {{Remove "@ResponseBody" annotations.}}
    // edit@qf3 [[sl=81;el=81;sc=5;ec=18]] {{}}
    public void m2() {
    }
  }
}
