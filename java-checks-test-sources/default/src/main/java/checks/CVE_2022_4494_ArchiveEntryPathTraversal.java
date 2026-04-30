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

class CVE_2022_4494_ArchiveEntryPathTraversal {
  void extract(ZipEntry ze, File destDir) throws Exception {
    String fileName = ze.getName();
    File newFile = new File(destDir, fileName); // Noncompliant
    if (ze.isDirectory()) {
      newFile.mkdirs();
    } else {
      FileOutputStream fos = new FileOutputStream(newFile);
      fos.write(1);
      fos.close();
    }
  }
}
