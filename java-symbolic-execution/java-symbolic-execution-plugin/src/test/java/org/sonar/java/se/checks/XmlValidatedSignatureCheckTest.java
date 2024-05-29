/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.se.checks;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class XmlValidatedSignatureCheckTest {

  @Test
  void test() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S6377_XmlValidatedSignatureCheckSample.java"))
      .withChecks(new XmlValidatedSignatureCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void sv_equals() {
    InternalSyntaxToken tree1 = new InternalSyntaxToken(0, 0, "a", Collections.emptyList(), false);
    InternalSyntaxToken tree2 = new InternalSyntaxToken(1, 2, "b", Collections.emptyList(), false);
    XmlValidatedSignatureCheck.DomValidateContextSymbolicValue sv1 = new XmlValidatedSignatureCheck.DomValidateContextSymbolicValue(tree1);
    XmlValidatedSignatureCheck.DomValidateContextSymbolicValue sv2 = new XmlValidatedSignatureCheck.DomValidateContextSymbolicValue(tree1);
    assertThat(sv1)
      .isEqualTo(sv1)
      .isEqualTo(sv2)
      .isNotEqualTo(new XmlValidatedSignatureCheck.DomValidateContextSymbolicValue(tree2))
      .isNotEqualTo(new Object());
    // explicit null check
    assertThat(sv1.equals(null)).isFalse();

    sv1.setField(true);
    assertThat(sv1).isNotEqualTo(sv2);
  }
}
