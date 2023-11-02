package checks.spring;

import javax.annotation.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

public class ControllerWithRestControllerReplacementCheck {

  @Controller // Noncompliant [[sc=3;ec=14;secondary=14;quickfixes=qf1]] {{Replace the "@Controller" annotation by "@RestController" and remove all
              // "@ResponseBody" annotations.}}
  class ClassOne {

    @ResponseBody
    // fix@qf1 {{Remove "@ResponseBody" annotations.}}
    // edit@qf1 [[sl=14;el=14;sc=5;ec=18]] {{}}
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

  @Controller // Noncompliant [[sc=3;ec=14;secondary=67,73;quickfixes=qf2]] {{Replace the "@Controller" annotation by "@RestController" and remove all
              // "@ResponseBody" annotations.}}
  class ClassSeven {

    @ResponseBody
    // fix@qf2 {{Remove "@ResponseBody" annotations.}}
    // edit@qf2 [[sl=67;el=67;sc=5;ec=18]] {{}}
    public void m() {
    }

    @ResponseBody
    // fix@qf2 {{Remove "@ResponseBody" annotations.}}
    // edit@qf2 [[sl=73;el=73;sc=5;ec=18]] {{}}
    public void m2() {
    }
  }
}
