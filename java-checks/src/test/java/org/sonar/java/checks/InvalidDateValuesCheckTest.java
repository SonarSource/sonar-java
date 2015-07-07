/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class InvalidDateValuesCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/InvalidDateValuesCheck.java"), new VisitorsBridge(new InvalidDateValuesCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
        //java.util.Date
      .next().atLine(14).withMessage("\"32\" is not a valid value for \"setDate\" method.")
      .next().atLine(17).withMessage("\"12\" is not a valid value for \"setMonth\" method.")
      .next().atLine(19).withMessage("\"24\" is not a valid value for \"setHours\" method.")
      .next().atLine(21).withMessage("\"61\" is not a valid value for \"setMinutes\" method.")
      .next().atLine(23).withMessage("\"63\" is not a valid value for \"setSeconds\" method.")
      .next().atLine(24).withMessage("\"-1\" is not a valid value for \"setSeconds\" method.")
        //java.sql.Date
      .next().atLine(27).withMessage("\"24\" is not a valid value for \"setHours\" method.")
      .next().atLine(29).withMessage("\"61\" is not a valid value for \"setMinutes\" method.")
      .next().atLine(31).withMessage("\"63\" is not a valid value for \"setSeconds\" method.")
        //Calendar
      .next().atLine(34).withMessage("\"12\" is not a valid value for setting \"MONTH\".")
      .next().atLine(36).withMessage("\"32\" is not a valid value for setting \"DAY_OF_MONTH\".")
      .next().atLine(38).withMessage("\"24\" is not a valid value for setting \"HOUR_OF_DAY\".")
      .next().atLine(40).withMessage("\"61\" is not a valid value for setting \"MINUTE\".")
      .next().atLine(42).withMessage("\"63\" is not a valid value for setting \"SECOND\".")
      .next().atLine(43).withMessage("\"-2\" is not a valid value for setting \"HOUR_OF_DAY\".")
      //Gregorian Calendar
        .next().atLine(46).withMessage("\"12\" is not a valid value for setting \"month\".")
        .next().atLine(48).withMessage("\"32\" is not a valid value for setting \"dayOfMonth\".")
        .next().atLine(50).withMessage("\"24\" is not a valid value for setting \"hourOfDay\".")
        .next().atLine(52).withMessage("\"61\" is not a valid value for setting \"minute\".")
        .next().atLine(54).withMessage("\"63\" is not a valid value for setting \"second\".")
        .next().atLine(55).withMessage("\"-1\" is not a valid value for setting \"month\".")
        .next().atLine(55).withMessage("\"63\" is not a valid value for setting \"second\".")
        .next().atLine(56).withMessage("\"63\" is not a valid value for setting \"second\".")
        //Comparisons
        .next().atLine(62).withMessage("\"12\" is not a valid value for \"MONTH\".")
        .next().atLine(65).withMessage("\"32\" is not a valid value for \"DAY_OF_MONTH\".")
        .next().atLine(68).withMessage("\"32\" is not a valid value for \"getDate\".")
        .next().atLine(69).withMessage("\"-1\" is not a valid value for \"getSeconds\".")
    ;
  }

}
