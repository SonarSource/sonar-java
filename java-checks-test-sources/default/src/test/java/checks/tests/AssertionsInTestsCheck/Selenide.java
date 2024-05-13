package checks.tests.AssertionsInTestsCheck;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.openqa.selenium.By;

import org.junit.Test;

class SelenideTest {

  @Test
  public void selenide_verify_SelenideElement() { // Compliant
    Selenide.open("https://www.google.com/");
    Selenide.$(By.name("q")).val("selenide").pressEnter();
    Selenide.$("#ires .g", 0).shouldHave(Condition.text("Selenide: concise UI tests in Java"));
  }

  @Test
  public void selenide_verify_ElementsCollection() { // Compliant
    Selenide.open("https://www.google.com/");
    Selenide.$(By.name("q")).val("selenide").pressEnter();
    Selenide.$$("#ires .g").shouldHave(CollectionCondition.sizeGreaterThan(1));
  }

  @Test
  public void contains_no_assertions() { // Noncompliant {{Add at least one assertion to this test case.}}
//            ^^^^^^^^^^^^^^^^^^^^^^
    Selenide.open("https://www.google.com/");
    Selenide.$(By.name("q")).val("selenide").pressEnter();
  }

}
