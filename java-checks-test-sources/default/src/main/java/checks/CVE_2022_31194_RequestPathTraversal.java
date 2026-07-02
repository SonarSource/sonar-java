/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
import java.io.File;

class CVE_2022_31194_RequestPathTraversal {
  void upload(Request request, String tempDir) {
    String resumableIdentifier = request.getParameter("resumableIdentifier");
    tempDir = tempDir + File.separator + resumableIdentifier;
    File fileDir = new File(tempDir); // Noncompliant
    if (!fileDir.exists()) {
      fileDir.mkdir();
    }
  }
  interface Request { String getParameter(String name); }
}
