package checks;

import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

public class PathVariableAnnotationShouldBePresentIfPathVariableIsUsedCheckSample {

  @GetMapping("/{id}")
  public String get(String id) { // Noncompliant [[sc=27;ec=37]] {{Add the "@PathVariable" annotation to "id".}}
    return "Hello World";
  } // Noncompliant [[sc=3;ec=4]] {{Add the "@PathVariable" annotation to "id".}}

  @PostMapping("/{id}")
  public String post(String id) { // Noncompliant [[sc=27;ec=38]] {{Add the "@PathVariable" annotation to "id".}}
    return "Hello World";
  } // Noncompliant

  @PutMapping("/{id}")
  public String put(String id) { // Noncompliant
    return "Hello World";
  } // Noncompliant

  @DeleteMapping("/{id}")
  public String delete(String id) { // Noncompliant
    return "Hello World";
  } // Noncompliant

  @GetMapping("/{id}")
  public String getCompliant(@PathVariable  String id) { // compliant
    return "Hello World";
  } // compliant

  @PostMapping("/{id}")
  public String postCpmpliant(@PathVariable  String id) { // Noncompliant [[sc=27;ec=38]] {{Add the "@PathVariable" annotation to "id".}}
    return "Hello World";
  } // compliant

  @PutMapping("/{id}")
  public String putCompliant(@PathVariable  String id) { // compliant
    return "Hello World";
  } // compliant

  @DeleteMapping("/{id}")
  public String deleteCompliant(@PathVariable String id) { // compliant
    return "Hello World";
  } // compliant

  @GetMapping("/{id}")
  public String getOtherThanString(@PathVariable Integer id) { // compliant, is it good
    return "Hello World";
  }

  @GetMapping("/{id}")
  public String getFullyQualified(@org.springframework.web.bind.annotation.PathVariable String id) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}/{name}")
  public String get2PathVariables(@PathVariable String id, @PathVariable String name) { // compliant
    return "Hello World";
  } // compliant

  @GetMapping("/{id}")
  public String getBadName(@PathVariable String a) { // compliant, does it even compile
    return "Hello World";
  } // Noncompliant

  @GetMapping("/{id}/{name}/{age}")
  public String getNotSameName(@PathVariable("name") String a, @PathVariable(name = "name") String b, @PathVariable(value = "id", required=false) String c) { // compliant
    return "Hello World";
  }

  @GetMapping("/{id}")
  public String getMap(@PathVariable Map<String, String> map) { // compliant
    return "Hello World";
  } // compliant

  @GetMapping("/{id}/{name}")
  public String getMap2(@PathVariable Map<String, String> map) { // compliant
    return "Hello World";
  } // compliant

  @GetMapping("/{id}/{name}/{age}")
  public String getMapMixed(@PathVariable Map<String, String> map, @PathVariable String age) { // compliant
    return "Hello World";
  }

  @GetMapping(value = {"/a/{id}", "/b/{id}", "/c"})
  public String getSeveralPaths(@PathVariable Optional<String> id) { // compliant
    return "Hello World";
  } // compliant

  @GetMapping({"/a/{id}", "/b/{id}", "/c"})
  public String getSeveralPathsDefault(@PathVariable Optional<String> id) { // compliant
    return "Hello World";
  } // compliant

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
  } // compliant

  public String withoutRequestMappingAnnotation(@PathVariable  String id) { // compliant, does it compile ?
    return "Hello World";
  } // compliant


  @GetMapping(name="aName",
    path={"/{id}", "/{name}"},
    produces={"application/json", "application/xml"},
    consumes={"application/json", "application/xml"},
    headers={"aHeader=aValue", "anotherHeader=anotherValue"},
    params={"aPara", "anotherParam=anotherValue"}
  )
  public String getFullExample(@PathVariable Map<String,String> x) { // compliant
    return "Hello World";
  }

}
