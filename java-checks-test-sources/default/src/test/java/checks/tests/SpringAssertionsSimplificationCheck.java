package checks.tests;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringAssertionsSimplificationCheck {

  Object myObject;
  ModelAndView modelAndView;
  ModelAndView mav = getMyModelAndView();
  Object modelMap = getMyModelAndView().getModelMap().get("myAttribute");

  @Test
  void couldBeSimplified() {
    ModelAndView mav = getMyModelAndView();

    assertEquals("register", mav.getViewName()); // Noncompliant [[sc=5;ec=17]] {{Replace this assertion by "ModelAndViewAssert.assertViewName".}}
    assertTrue((Boolean) mav.getModelMap().get("myAttribute")); // Noncompliant [[sc=5;ec=15]] {{Replace this assertion by "ModelAndViewAssert.assertModelAttributeValue".}}
    assertFalse((Boolean) mav.getModelMap().get("myAttribute")); // Noncompliant {{Replace this assertion by "ModelAndViewAssert.assertModelAttributeValue".}}
    assertEquals(myObject, mav.getModelMap().get("myAttribute")); // Noncompliant {{Replace this assertion by "ModelAndViewAssert.assertModelAttributeValue".}}

    assertEquals("register", mav.getView().toString()); // Compliant
    helper(mav.getViewName()); // Compliant
    helper(mav.getModelMap().get("myAttribute")); // Compliant
    assertEquals(myObject, helper(mav.getModelMap())); // Compliant

    ModelMap modelMap = mav.getModelMap();
    assertTrue((Boolean) modelMap.get("myAttribute")); // Noncompliant
    assertFalse((Boolean) modelMap.get("myAttribute")); // Noncompliant
    assertEquals(myObject, modelMap.get("myAttribute")); // Noncompliant

    Object o = modelMap.get("myAttribute");

    // AssertJ
    assertThat(mav.getViewName()).isEqualTo("register"); // Noncompliant [[sc=5;ec=15]] {{Replace this assertion by "ModelAndViewAssert.assertViewName".}}
    assertThat((Boolean) mav.getModelMap().get("myAttribute")).isTrue(); // Noncompliant {{Replace this assertion by "ModelAndViewAssert.assertModelAttributeValue".}}
    assertThat((Boolean) mav.getModelMap().get("myAttribute")).isFalse(); // Noncompliant {{Replace this assertion by "ModelAndViewAssert.assertModelAttributeValue".}}
    assertThat(mav.getModelMap().get("myAttribute")).isEqualTo(myObject); // Noncompliant {{Replace this assertion by "ModelAndViewAssert.assertModelAttributeValue".}}

    assertThat(mav.getViewName()).isNotNull(); // Compliant
    assertThat(mav.getModelMap().get("myAttribute")).isInstanceOf(Boolean.class); // Compliant

    // Fest
    org.fest.assertions.Assertions.assertThat(mav.getViewName()).isEqualTo("register"); // Noncompliant
    org.fest.assertions.Assertions.assertThat((Boolean) mav.getModelMap().get("myAttribute")).isTrue(); // Noncompliant
    org.fest.assertions.Assertions.assertThat((Boolean) mav.getModelMap().get("myAttribute")).isFalse(); // Noncompliant
    org.fest.assertions.Assertions.assertThat(mav.getModelMap().get("myAttribute")).isEqualTo(myObject); // Noncompliant

    org.fest.assertions.Assertions.assertThat(mav.getViewName()).isNotNull(); // Compliant
    org.fest.assertions.Assertions.assertThat(mav.getModelMap().get("myAttribute")).isInstanceOf(Boolean.class); // Compliant
  }

  @Test
  void betterUsage() {
    ModelAndView mav = getMyModelAndView();

    ModelAndViewAssert.assertViewName(mav, "register");
    ModelAndViewAssert.assertModelAttributeValue(mav, "myAttribute", Boolean.TRUE);
    ModelAndViewAssert.assertModelAttributeValue(mav, "myAttribute", Boolean.FALSE);
    ModelAndViewAssert.assertModelAttributeValue(mav, "myAttribute", myObject);
  }

  private ModelAndView getMyModelAndView() {
    return modelAndView;
  }

  private void helper(String mav) {}

  private Object helper(Object mav) {
    return new Object();
  }
}
