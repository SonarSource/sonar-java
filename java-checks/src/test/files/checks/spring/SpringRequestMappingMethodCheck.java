package files.checks.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class Controller {

  @RequestMapping("/") // Noncompliant [[sc=4;ec=18]] {{Add a "method" parameter to this "@RequestMapping" annotation.}}
  String get() {
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

  @RequestMapping(path = "/delete", method = {RequestMethod.GET, RequestMethod.POST}) // Noncompliant [[sc=46;ec=85]] {{Consider narrowing this list of methods to one.}}
  String delete(@RequestParam("id") String id) {
    return "Hello from delete";
  }

}

@RestController
@RequestMapping(path = "/other", method = RequestMethod.GET)
public class OtherController {

  @RequestMapping("/")
  String get() {
    return "Hello from get";
  }

}

public class DerivedController extends OtherController {

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
public class ControllerImpl implements InterfaceController {

  @RequestMapping("/")
  String get() {
    return "Hello from get";
  }

}

interface Dummy { }

public class DummyFoo implements Dummy {

  @RequestMapping(path = "/other") // Noncompliant
  String get() {
    return "Hello from get";
  }
}
