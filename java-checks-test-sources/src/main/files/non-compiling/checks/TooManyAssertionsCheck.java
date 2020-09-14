/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
  public void verifyRestAssured() { // Noncompliant [[sc=15;ec=32]]{{Refactor this method in order to have less than 10 assertions.}}
    given().when().get("/garage").then()
      .body("name",equalTo("Acme garage"))
      .body("info.slots",equalTo(150))
      .body("info.status",equalTo("open"))
      .time(null)
      .content(null)
      .header("s", "s1")
      .cookie("cookie1")
      .cookie("cookie2")
      .spec(null)
      .specification(null)
      .statusCode(200);
  }

   @Test
   public abstract void abstractTest();

}
