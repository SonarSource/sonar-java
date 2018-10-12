package files.checks.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

@RestController
@RequestMapping("/home")
public class Controller {

  @RequestMapping("/1")
  String m1() { return ""; }

  @RequestMapping(path = "/2", method = RequestMethod.GET) // Noncompliant [[sc=4;ec=18]] {{Replace "@RequestMapping(method = RequestMethod.GET)" with "@GetMapping"}}
  String m2() { return ""; }

  @RequestMapping(path = "/3", method = {POST}) // Noncompliant {{Replace "@RequestMapping(method = RequestMethod.POST)" with "@PostMapping"}}
  String m3() { return ""; }

  @RequestMapping(path = "/4", method = {RequestMethod.PUT}) // Noncompliant {{Replace "@RequestMapping(method = RequestMethod.PUT)" with "@PutMapping"}}
  String m4() { return ""; }

  @RequestMapping(path = "/5", method = RequestMethod.PATCH) // Noncompliant {{Replace "@RequestMapping(method = RequestMethod.PATCH)" with "@PatchMapping"}}
  String m5() { return ""; }

  @RequestMapping(method = DELETE) // Noncompliant {{Replace "@RequestMapping(method = RequestMethod.DELETE)" with "@DeleteMapping"}}
  String m6() { return ""; }

  @RequestMapping(method = {GET, POST})
  String m7() { return ""; }

  @RequestMapping(method = {})
  String m8() { return ""; }

  @RequestMapping(method = UNKNOWN)
  String m9() { return ""; }

  @RequestMapping(method = unknown())
  String m9() { return ""; }
}
