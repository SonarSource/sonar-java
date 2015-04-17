/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class UselessConditionCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/UselessConditionCheck.java"),
      new VisitorsBridge(new UselessConditionCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(15).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(19).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(20).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(26).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(28).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(30).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(32).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(34).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(38).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(47).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(49).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(53).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(55).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(57).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(61).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(77).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(80).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(90).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(92).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(109).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(111).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(130).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(132).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(138).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(140).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(142).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(144).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(146).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(148).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(157).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(159).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(182).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(184).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(186).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(188).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(190).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(192).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(205).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(207).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(223).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(226).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(232).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(239).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(246).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(253).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(268).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(274).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(283).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(304).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(311).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(325).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(327).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(329).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(331).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(333).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(335).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(337).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(339).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(341).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(343).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(358).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(371).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(384).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(397).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(465).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(472).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(478).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(480).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(503).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(505).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(507).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(509).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(511).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(513).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(522).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(524).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(551).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(553).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(559).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(561).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(575).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(581).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(592).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(607).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(615).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(617).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(626).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(628).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(637).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(639).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(646).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(649).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(655).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(657).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(668).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(684).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(686).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(707).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(717).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(724).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(727).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(730).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(733).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(741).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(744).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(749).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(752).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(762).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(776).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(779).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(788).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(793).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(803).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(810).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(815).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(817).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(822).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(827).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(830).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(841).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(848).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(854).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(870).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(878).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(880).withMessage("Change this condition so that it does not always evaluate to \"true\"");
  }
}
