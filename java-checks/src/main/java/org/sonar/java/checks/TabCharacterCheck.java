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

import com.google.common.io.Files;
import org.sonar.check.Rule;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

@Rule(key = "S00105")
@RspecKey("S105")
public class TabCharacterCheck extends IssuableSubscriptionVisitor implements CharsetAwareVisitor {

  private Charset charset;
  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

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
    List<String> lines;
    try {
      lines = Files.readLines(file, charset);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    for (String line : lines) {
      if (line.contains("\t")) {
        addIssueOnFile("Replace all tab characters in this file by sequences of white-spaces.");
        break;
      }
    }
  }


}
