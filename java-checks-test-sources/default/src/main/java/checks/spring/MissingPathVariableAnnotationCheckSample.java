package checks.spring;

import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public class MissingPathVariableAnnotationCheckSample {

  @GetMapping("/{name:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{ext:\\.[a-z]+}") // Noncompliant
  public void handleWithoutExt(@PathVariable String name, @PathVariable String version) {}

  @GetMapping("/something/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}") // Compliant
  public String getObj(@PathVariable("id") String id){
    return "";
  }

  @GetMapping("/{name:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{ext:\\.[a-z]+}") // Compliant
  public void handle(@PathVariable String name, @PathVariable String version, @PathVariable String ext) {}



  @GetMapping("/{id}") // Noncompliant {{Bind template variable "id" to a method parameter.}}
//^^^^^^^^^^^^^^^^^^^^
  public String get(String id) {
    return "Hello World";
  }

  @PostMapping(value = "/{name}") // Noncompliant {{Bind template variable "name" to a method parameter.}}
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

  @RequestMapping("/{id}") // Noncompliant
  public String request(String id) {
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

  @RequestMapping("/{id}")
  public String requestCompliant(@PathVariable String id) {
    return "Hello World";
  }

  @GetMapping("/{id}")
  @DeleteMapping({"/{id}"})
  public String deleteGetCompliant(@PathVariable String id) { // compliant
    return "Hello World";
  }

  @interface NotRequestMappingAnnotation {}

  @NotRequestMappingAnnotation
  public void notAnnotatedWithRequestMapping(){}

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
  public String getBadName(@PathVariable String a) { // Noncompliant
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

  public String withoutRequestMappingAnnotation(@PathVariable  String id) { // Noncompliant
    return "Hello World";
  }

  @GetMapping(
    produces={"application/json", "application/xml"},
    consumes={"application/json", "application/xml"},
    headers={"aHeader=aValue", "anotherHeader=anotherValue"},
    params={"aPara", "anotherParam=anotherValue"},
    name="aName",
    path={"/{id}", "/{name}"}
  )
  public String getFullExample(@PathVariable Map<String,String> x) { // compliant
    return "Hello World";
  }

  @GetMapping( // Noncompliant
    produces={"application/json", "application/xml"},
    consumes={"application/json", "application/xml"},
    headers={"aHeader=aValue", "anotherHeader=anotherValue"},
    params={"aPara", "anotherParam=anotherValue"},
    name="aName",
    path={"/{id}", "/name"}
  )
  public String getFullExampleNonCompliant(Map<String,String> x) {
    return "Hello World";
  }

  @GetMapping("/id-{id:.+}")
  public String getCrazyPath(@PathVariable String id) { // compliant
    return "Hello World";
  } // compliant

  @GetMapping("/id-{id:.+}") // Noncompliant
  public String getCrazyPathNonCompliant(String id) {
    return "Hello World";
  }

  @GetMapping("/{id}/{a:${placeHolder}xxxx}/{b:${{placeHolder}}}")
  public String getPlaceHolder(@PathVariable String id, @PathVariable String a, @PathVariable String b) { // compliant, we don't consider this case
    return "Hello World";
  }

  static class ModelA {
    @ModelAttribute("user")
    public String getUser(@PathVariable String id, @PathVariable String name) { // always compliant when method  annotated with @ModelAttribute
      return "user"; // because the case is too complex to handle
    }

    @ModelAttribute("empty")
    public String emptyModel(String notPathVariable){
      return "";
    }

    @GetMapping("/{id}/{name}")
    public String get() { // compliant, @ModelAttribute is always called before @GetMapping to generate the model. In our case model attribute
      // consume the id and name path variables
      return "Hello World";
    }

    @GetMapping("/{id}/{name}/{age}") // Compliant
    public String get2(@PathVariable String age) { // compliant
      return "Hello World";
    }

    @GetMapping("/{id}/{name}/{age}") // Noncompliant {{Bind template variable "age" to a method parameter.}}
    public String get3() {
      return "Hello World";
    }
  }

  static class ModelB {
    @ModelAttribute("user")
    public String getUser(@PathVariable String id) {
      return "user";
    }

    @ModelAttribute("id")
    public String getId(@PathVariable String name) {
      return "id";
    }

    @GetMapping("/{id}/{name}")
    public String get() { // compliant
      return "Hello World";
    }

    @GetMapping("/{id}/{name}/{age}")
    public String get2(@PathVariable String age) { // compliant
      return "Hello World";
    }

    @GetMapping("/{id}/{name}/{age}") // Noncompliant
    public String get3() {
      return "Hello World";
    }
  }


  @GetMapping("/a/path")
  public String pathVariableWithoutParameter(@PathVariable String aVar){ // Noncompliant {{Bind method parameter "aVar" to a template variable.}}
//                                           ^^^^^^^^^^^^^^^^^^^^^^^^^
    return "";
  }

  @GetMapping("/a/path/{aVar1}")
  public String twoPathVariables(@PathVariable String aVar1, @PathVariable String aVar2){ // Noncompliant
//                                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^
    return "";
  }

  @GetMapping("/a/path")
  public String pathVariableWithValue(@PathVariable(value = "aVar") String foo){ // Noncompliant
    return "";
  }

  @GetMapping("/a/path")
  public String pathVariableWithName(@PathVariable(name = "aVar") String foo){ // Noncompliant
    return "";
  }

  @GetMapping("/a/path")
  public String pathVariableWithDefault(@PathVariable("aVar") String foo){ // Noncompliant
    return "";
  }

  @GetMapping("/a/path")
  public String pathVariableEmptyName(@PathVariable("") String foo){ // Noncompliant
    return "";
  }

  @RequestMapping("/path/{id}")
  static class Controller {
    void fromRequestMapping(@PathVariable String id){}

    @GetMapping("/{age}")
    void fromBoth(@PathVariable String id, @PathVariable int age){}

    @GetMapping()
    void missingTemplateParameter(@PathVariable String missing){} // Noncompliant
  }

  @RequestMapping()
  static class EmptyRequestMapping {}

  @RequestMapping("/{age")
  static class WrongPathRequestMapping {}
}
