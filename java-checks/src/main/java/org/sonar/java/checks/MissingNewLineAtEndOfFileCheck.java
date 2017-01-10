/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

@Rule(key = "S00113")
@RspecKey("S113")
public class MissingNewLineAtEndOfFileCheck extends IssuableSubscriptionVisitor implements CharsetAwareVisitor {

  private Charset charset;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.emptyList();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.context = context;
    visitFile(context.getFile());
  }

  public void visitFile(File file) {
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
      if (!endsWithNewline(randomAccessFile)) {
        addIssueOnFile("Add a new line at the end of this file.");
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean endsWithNewline(RandomAccessFile randomAccessFile) throws IOException {
    if (randomAccessFile.length() < 1) {
      return false;
    }
    randomAccessFile.seek(randomAccessFile.length() - 1);
    byte[] chars = new byte[1];
    if (randomAccessFile.read(chars) < 1) {
      return false;
    }
    String ch = new String(chars, charset);
    return "\n".equals(ch) || "\r".equals(ch);
  }

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }
}
