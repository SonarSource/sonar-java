package checks;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.POST;


/**
 * This class serves as a sample for older spring-web version (4.0) where @GetMapping, @PostMapping and so on were not present
 * hence no issues are expected to be reported here when using the generic @RequestMapping
 */
@RestController
@RequestMapping("/home")
public class SpringComposedRequestMappingCheckSample {

  @RequestMapping("/1")
  String m1() {
    return "";
  }

  @RequestMapping(method = RequestMethod.GET)
  String m2() {
    return "";
  }

  @RequestMapping(method = {POST})
  String m3() {
    return "";
  }

  @RequestMapping(method = {RequestMethod.PUT})
  String m4() {
    return "";
  }

  @RequestMapping(method = RequestMethod.PATCH)
  String m5() {
    return "";
  }

}
