package checks;

import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

public class PathVariableAnnotationShouldBePresentIfPathVariableIsUsedCheckSample {
  @GetMapping("/{id}") // Noncompliant {{Bind path variable "id" to a method parameter.}}
  public String get(String id) {
    return "Hello World";
  }

  @PostMapping(value = "/{name}") // Noncompliant {{Bind path variable "name" to a method parameter.}}
  public String post(String id) {
    return "Hello World";
  }

  @PutMapping(path = "/{id}") // Noncompliant
  public String put(String id) {
    return "Hello World";
  }

  @DeleteMapping("/{id}") // Noncompliant
  public String delete(String id) {
    return "Hello World";
  }

  @PutMapping("/id")
  @DeleteMapping("/{id}") // Noncompliant
  public String deletePut(String id) {
    return "Hello World";
  }


  @GetMapping("/{id}")
  public String getCompliant(@PathVariable  String id) { // compliant
    return "Hello World";
  }

  @PostMapping("/{id}")
  public String postCompliant(@PathVariable  String id) { // compliant
    return "Hello World";
  }

  @PutMapping("/{id}")
  public String putCompliant(@PathVariable  String id) { // compliant
    return "Hello World";
  }


  @DeleteMapping("/{id}")
  public String deleteCompliant(@PathVariable String id) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}")
  @DeleteMapping({"/{id}"})
  public String deleteGetCompliant(@PathVariable String id) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}")
  public String getOtherThanString(@PathVariable Integer id) { // compliant
    return "Hello World";
  }

  @GetMapping()
  public String getEmpty() { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}")
  public String getFullyQualified(@org.springframework.web.bind.annotation.PathVariable String id) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}/{name}")
  public String get2PathVariables(@PathVariable String id, @PathVariable String name) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}") // Noncompliant
  public String getBadName(@PathVariable String a) {
    return "Hello World";
  }

  @GetMapping("/{id}/{name}/{age}") // Noncompliant
  public String get2SameName(@PathVariable("name") String a, @PathVariable(name = "name") String b, @PathVariable(value = "id", required=false) String c) {
    return "Hello World";
  }

  @GetMapping("/{id}/{name}/{age}")
  public String get3Name(@PathVariable("name") String a, @PathVariable(name = "age") String b, @PathVariable(value = "id", required=false) String c) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}")
  public String getMap(@PathVariable Map<String, String> map) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}/{name}")
  public String getMap2(@PathVariable Map<String, String> map) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}/{name}/{age}")
  public String getMapMixed(@PathVariable Map<String, String> map, @PathVariable String age) { // compliant
    return "Hello World";
  }

  @GetMapping(value = {"/a/{id}", "/b/{id}", "/c"})
  public String getSeveralPaths(@PathVariable Optional<String> id) { // compliant
    return "Hello World";
  }

  @GetMapping({"/a/{id}", "/b/{id}", "/c"})
  public String getSeveralPathsDefault(@PathVariable Optional<String> id) { // compliant
    return "Hello World";
  }

  @GetMapping("/a/{id:.+}")
  public String getRegex(@PathVariable String id) { // compliant
    return "Hello World";
  }

  @GetMapping("/a/{id:.+}/{name:.+}")
  public String getRegex2(@PathVariable String id, @PathVariable String name) { // compliant
    return "Hello World";
  }

  public String withoutAnnotation(String id) { // compliant
    return "Hello World";
  }

  public String withoutRequestMappingAnnotation(@PathVariable  String id) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id/name}")
  public String stangePath(@PathVariable String id) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}/{name}") // Noncompliant
  public String mapStringToInt(@PathVariable Map<String,Integer> map) {
    return "Hello World";
  }


  @GetMapping(
    path={"/{id}", "/{name}"},
    produces={"application/json", "application/xml"},
    consumes={"application/json", "application/xml"},
    headers={"aHeader=aValue", "anotherHeader=anotherValue"},
    params={"aPara", "anotherParam=anotherValue"},
    name="aName"
  )
  public String getFullExample(@PathVariable Map<String,String> x) { // compliant
    return "Hello World";
  }

}
