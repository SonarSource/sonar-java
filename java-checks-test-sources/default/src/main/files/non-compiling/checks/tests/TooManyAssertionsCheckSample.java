/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * long with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.ce.queue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TooManyAssertionsNonCompilingCheck {

  @Test
  public void testMothod() {
    A a = new A();
    A a1 = new A(123);

    Arrays.asList(1, 2).forEach(A::test);
    Arrays.asList(1, 2).forEach(A::not_a_test);

    class A {
      public A() {
        System.out.println("Hello!");
      }

      public A(int a) {
        assertThat(a == 1).isTrue();
        assertThat(a == 3).isTrue();
      }

      static void test(int i) {
        assertThat(1 == i).isTrue();
      }

      static void not_a_test(int i) {
        System.out.println(i);
      }
    }
  }

  @Test
  public void verifyRestAssured() { // Compliant, considered as one assertion, can be revisited in the future and considered as false negative
    given().when().get("/garage").then()
      .body("name",equalTo("Acme garage"))
      .body("info.slots",equalTo(150))
      .body("info.status",equalTo("open"))
      .time(null)
      .content(null)
      .header("s", "s1")
      .cookie("cookie1")
      .cookie("cookie2")
      .cookie("cookie3")
      .cookie("cookie4")
      .cookie("cookie5")
      .cookie("cookie6")
      .cookie("cookie7")
      .cookie("cookie8")
      .cookie("cookie9")
      .cookie("cookie10")
      .cookie("cookie11")
      .cookie("cookie12")
      .cookie("cookie13")
      .cookie("cookie14")
      .cookie("cookie15")
      .cookie("cookie16")
      .cookie("cookie17")
      .spec(null)
      .specification(null)
      .statusCode(200);
  }

   @Test
   public abstract void abstractTest();

}
