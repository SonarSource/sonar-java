package test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(value = "/ex")
public class SpringEndpoints {

  @RequestMapping(path = "/profile", method = RequestMethod.GET)
  @ResponseBody
  public UserProfile getUserProfile(String name) { // Noncompliant [[sc=22;ec=36]] {{Make sure that exposing this HTTP endpoint is safe here.}}

  }

  @GetMapping
  public Object get() {} // Noncompliant

  @PostMapping
  public Object post() {} // Noncompliant

  @PutMapping
  public Object put() {} // Noncompliant

  @DeleteMapping
  public Object delete() {} // Noncompliant

  @PatchMapping
  public Object patch() {} // Noncompliant

  public Object noIssueWithoutAnnotation() {}

}
