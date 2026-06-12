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
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;

class ArchiveEntryPathTraversalCheckSample {
  void bad(ZipEntry ze, File destDir) throws Exception {
    String fileName = ze.getName();
    File newFile = new File(destDir, fileName); // Noncompliant
    FileOutputStream fos = new FileOutputStream(newFile);
  }

  void good(File destDir) throws Exception {
    String fileName = "safe.txt";
    File newFile = new File(destDir, fileName);
    FileOutputStream fos = new FileOutputStream(newFile);
  }
}
