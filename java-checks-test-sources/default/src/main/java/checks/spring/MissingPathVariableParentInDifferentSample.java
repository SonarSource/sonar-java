package checks.spring;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

public class MissingPathVariableParentInDifferentSample {
  @ModelAttribute("parentView")
  public String getView(@PathVariable("view") final String view){
    return "";
  }
}
