/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.se;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaFileScanner;

public class SECheckVerifier implements CheckVerifier {
  
  private final InternalCheckVerifier checkVerifier;
  
  public static SECheckVerifier newVerifier() {
    return new SECheckVerifier();
  }

  private SECheckVerifier() {
    checkVerifier = (InternalCheckVerifier) CheckVerifier.newVerifier();
  }

  @Override
  public CheckVerifier withCheck(JavaFileScanner check) {
    return withChecks(check);
  }

  @Override
  public CheckVerifier withChecks(JavaFileScanner... checks) {
    List<SECheck> seChecks = Arrays.stream(checks)
      .filter(SECheck.class::isInstance)
      .map(SECheck.class::cast)
      .collect(Collectors.toList());
    List<JavaFileScanner> newCheckList = new ArrayList<>();
    if (!seChecks.isEmpty()) {
      newCheckList.add(new SymbolicExecutionVisitor(seChecks));
    }
    newCheckList.addAll(Arrays.asList(checks));
    checkVerifier.withChecks(newCheckList.toArray(new JavaFileScanner[0]));
    return this;
  }

  public CheckVerifier withCustomIssueVerifier(Consumer<Set<AnalyzerMessage>> customIssueVerifier) {
    checkVerifier.withCustomIssueVerifier(customIssueVerifier);
    return this;
  }


  @Override
  public CheckVerifier withClassPath(Collection<File> classpath) {
    checkVerifier.withClassPath(classpath);
    return this;
  }

  @Override
  public CheckVerifier withJavaVersion(int javaVersionAsInt) {
    checkVerifier.withJavaVersion(javaVersionAsInt);
    return this;
  }

  @Override
  public CheckVerifier withinAndroidContext(boolean inAndroidContext) {
    checkVerifier.withinAndroidContext(inAndroidContext);
    return this;
  }

  @Override
  public CheckVerifier onFile(String filename) {
    checkVerifier.onFile(filename);
    return this;
  }

  @Override
  public CheckVerifier onFiles(String... filenames) {
    checkVerifier.onFiles(filenames);
    return this;
  }

  @Override
  public CheckVerifier onFiles(Collection<String> filenames) {
    checkVerifier.onFiles(filenames);
    return this;
  }

  @Override
  public CheckVerifier withoutSemantic() {
    checkVerifier.withoutSemantic();
    return this;
  }

  @Override
  public void verifyIssues() {
    checkVerifier.verifyIssues();
  }

  @Override
  public void verifyIssueOnFile(String expectedIssueMessage) {
    checkVerifier.verifyIssueOnFile(expectedIssueMessage);
  }

  @Override
  public void verifyIssueOnProject(String expectedIssueMessage) {
    checkVerifier.verifyIssueOnProject(expectedIssueMessage);
  }

  @Override
  public void verifyNoIssues() {
    checkVerifier.verifyNoIssues();
  }
}
