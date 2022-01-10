/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.plugins.surefire.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitTestIndexTest {

  @Test
  void shouldIndexNewClassname() {
    UnitTestIndex index = new UnitTestIndex();

    UnitTestClassReport report = index.index("org.sonar.Foo");

    assertThat(report.getTests()).isZero();
    assertThat(index.size()).isEqualTo(1);
    assertThat(report).isSameAs(index.get("org.sonar.Foo"));
  }

  @Test
  void shouldNotReIndex() {
    UnitTestIndex index = new UnitTestIndex();

    UnitTestClassReport report1 = index.index("org.sonar.Foo");
    UnitTestClassReport report2 = index.index("org.sonar.Foo");

    assertThat(report1).isSameAs(report2);
    assertThat(report1.getTests()).isZero();
    assertThat(index.size()).isEqualTo(1);
    assertThat(report1).isSameAs(index.get("org.sonar.Foo"));
  }

  @Test
  void shouldRemoveClassname() {
    UnitTestIndex index = new UnitTestIndex();

    index.index("org.sonar.Foo");
    index.remove("org.sonar.Foo");

    assertThat(index.size()).isZero();
    assertThat(index.get("org.sonar.Foo")).isNull();
  }

  @Test
  void shouldMergeClasses() {
    UnitTestIndex index = new UnitTestIndex();
    UnitTestClassReport innerClass = index.index("org.sonar.Foo$Bar");
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_OK).setDurationMilliseconds(200L));
    UnitTestClassReport publicClass = index.index("org.sonar.Foo");
    publicClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR).setDurationMilliseconds(1000L));
    publicClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_FAILURE).setDurationMilliseconds(350L));

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size()).isEqualTo(1);
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests()).isEqualTo(4);
    assertThat(report.getFailures()).isEqualTo(1);
    assertThat(report.getErrors()).isEqualTo(2);
    assertThat(report.getSkipped()).isZero();
    assertThat(report.getResults()).hasSize(4);
    assertThat(report.getDurationMilliseconds()).isEqualTo(500L + 200L + 1000L + 350L);
  }

  @Test
  void shouldRenameClassWhenMergingToNewClass() {
    UnitTestIndex index = new UnitTestIndex();
    UnitTestClassReport innerClass = index.index("org.sonar.Foo$Bar");
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_OK).setDurationMilliseconds(200L));

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size()).isEqualTo(1);
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests()).isEqualTo(2);
    assertThat(report.getFailures()).isZero();
    assertThat(report.getErrors()).isEqualTo(1);
    assertThat(report.getSkipped()).isZero();
    assertThat(report.getResults()).hasSize(2);
    assertThat(report.getDurationMilliseconds()).isEqualTo(500L + 200L);
  }

  @Test
  void shouldNotFailWhenMergingUnknownClass() {
    UnitTestIndex index = new UnitTestIndex();

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size()).isZero();
  }
}
