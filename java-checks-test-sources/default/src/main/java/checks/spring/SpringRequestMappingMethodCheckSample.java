package checks.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class SpringRequestMappingMethodCheckSample {

  @RequestMapping("/") // Noncompliant {{Make sure allowing safe and unsafe HTTP methods is safe here.}}
// ^^^^^^^^^^^^^^
  String home() {
    return "Hello from get";
  }

  @RequestMapping(path = "/index", method = RequestMethod.GET)
  String index() {
    return "Hello from index";
  }

  @RequestMapping(path = "/list", method = {RequestMethod.POST})
  String list() {
    return "Hello from list";
  }

  @GetMapping(path = "/get")
  String get() {
    return "Hello from get";
  }

  @RequestMapping(path = "/delete", method = {RequestMethod.GET, RequestMethod.POST}) // Noncompliant {{Make sure allowing safe and unsafe HTTP methods is safe here.}}
//                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  String delete(@RequestParam("id") String id) {
    return "Hello from delete";
  }

  @RequestMapping(path = "/safe", method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.TRACE}) // Compliant
  String safe(@RequestParam("id") String id) {
    return "safe";
  }

  @RequestMapping(path = "/unsafe", method = {RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT}) // Compliant
  String unsafe(@RequestParam("id") String id) {
    return "unsafe";
  }

  @RequestMapping(path = "/all", method = { // Noncompliant
    RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.TRACE,
    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT
  })
  String all(@RequestParam("id") String id) {
    return "all";
  }

  @RestController
  @RequestMapping(path = "/other", method = RequestMethod.GET)
  public static class OtherController {

    @RequestMapping("/")
    String get() {
      return "Hello from get";
    }

  }

  public static class DerivedController extends OtherController {

    @RequestMapping("/")
    String get() {
      return "Hello from get";
    }

  }

  interface X {}

  @RequestMapping(path = "/other", method = RequestMethod.GET)
  interface InterfaceController extends X {
  }

  @RestController
  public static class ControllerImpl implements InterfaceController {

    @RequestMapping("/")
    String get() {
      return "Hello from get";
    }

  }

  interface Dummy { }

  public static class DummyFoo implements Dummy {

    @RequestMapping(path = "/other") // Noncompliant
    String get() {
      return "Hello from get";
    }
  }

}
