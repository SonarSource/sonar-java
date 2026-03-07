package checks.spring.s6856;

import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

public class MissingPathVariableAnnotationCheck_ModelAttribute {

  class ParentController {
    @ModelAttribute("viewCfg")
    public String getView(@PathVariable("view") final String view){
      return "";
    }
  }
  class ChildController extends ParentController {
    @GetMapping("/model/{view}") //Compliant, parent class defines 'view' path var in the model attribute
    public String list(@ModelAttribute("viewCfg") final String viewConfig){
      return "";
    }
  }
  class MissingParentChildController extends MissingPathVariableParentInDifferentSample {
    @GetMapping("/model/{view}") // Noncompliant
    // FP: parent class in different file, cannot collect the model attribute
    public String list(@ModelAttribute("parentView") final String viewConfig){
      return "";
    }
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

  static class ReportPeriod {
    private String project;
    private int year;
    private String month;

    public String getProject() {
      return project;
    }

    public int getYear() {
      return year;
    }

    public String getMonth() {
      return month;
    }

    public void setProject(String project) {
      this.project = project;
    }

    public void setYear(int year) {
      this.year = year;
    }

    public void setMonth(String month) {
      this.month = month;
    }
  }

  static class ModelAttributeBindToClass {
    @GetMapping("/reports/{project}/{year}/{month}")
    public String getReport(@ModelAttribute ReportPeriod period) {
      // Spring sees {project} in the URL and calls period.setProject()
      // Spring sees {year} in the URL and calls period.setYear()
      return "reportDetails";
    }
  }

  // Test case: Parameter WITHOUT @ModelAttribute annotation should NOT extract properties
  static class WithoutModelAttributeAnnotation {
    @GetMapping("/api/{id}/{name}") // Noncompliant {{Bind template variable "name", "id" to a method parameter.}}
    public String process(ReportPeriod period) {
      return "result";
    }
  }

  // Test case: @ModelAttribute with STANDARD DATA TYPES should be skipped (no property extraction)
  static class ModelAttributeWithStandardDataTypes {
    @GetMapping("/string/{value}") // Noncompliant {{Bind template variable "value" to a method parameter.}}
    public String processString(@ModelAttribute String value) {
      return "result";
    }

    @GetMapping("/int/{count}") // Noncompliant {{Bind template variable "count" to a method parameter.}}
    public String processInt(@ModelAttribute int count) {
      return "result";
    }

    @GetMapping("/integer/{num}") // Noncompliant {{Bind template variable "num" to a method parameter.}}
    public String processInteger(@ModelAttribute Integer num) {
      return "result";
    }

    @GetMapping("/long/{id}") // Noncompliant {{Bind template variable "id" to a method parameter.}}
    public String processLong(@ModelAttribute Long id) {
      return "result";
    }

    @GetMapping("/double/{price}") // Noncompliant {{Bind template variable "price" to a method parameter.}}
    public String processDouble(@ModelAttribute Double price) {
      return "result";
    }

    @GetMapping("/float/{value}") // Noncompliant {{Bind template variable "value" to a method parameter.}}
    public String processFloat(@ModelAttribute Float value) {
      return "result";
    }

    @GetMapping("/boolean/{flag}") // Noncompliant {{Bind template variable "flag" to a method parameter.}}
    public String processBoolean(@ModelAttribute Boolean flag) {
      return "result";
    }

    @GetMapping("/optional/{id}") // Noncompliant {{Bind template variable "id" to a method parameter.}}
    public String processOptional(@ModelAttribute Optional<String> id) {
      return "result";
    }

    @GetMapping("/map/{key}") // Noncompliant {{Bind template variable "key" to a method parameter.}}
    public String processMap(@ModelAttribute Map<String, String> params) {
      // Map is a standard data type - no property extraction
      // Note: @ModelAttribute Map is different from @PathVariable Map
      // @PathVariable Map captures all path variables, but @ModelAttribute Map does not
      return "result";
    }
  }

  // Test case: Mixed scenario - complex type with standard type parameters
  static class MixedParameterTypes {
    @GetMapping("/data/{id}/{name}/{age}") // Noncompliant {{Bind template variable "name", "id", "age" to a method parameter.}}
    public String process(
      @ModelAttribute ReportPeriod period, // Complex type - extracts project, year, month
      String regularParam // Not @ModelAttribute - ignored
    ) {
      return "result";
    }

    @GetMapping("/user/{project}/{year}") // Compliant
    public String processPartial(
      @ModelAttribute ReportPeriod period, // Extracts project, year, month
      @PathVariable String year // Explicitly bound
    ) {
      return "result";
    }
  }

  // Test case: Multiple @ModelAttribute parameters
  static class MultipleModelAttributes {
    @GetMapping("/multi/{project}/{id}") // Noncompliant {{Bind template variable "id" to a method parameter.}}
    public String process(
      @ModelAttribute ReportPeriod period, // Extracts project, year, month
      @ModelAttribute String name // Standard type - no extraction
    ) {
      return "result";
    }
  }
}
