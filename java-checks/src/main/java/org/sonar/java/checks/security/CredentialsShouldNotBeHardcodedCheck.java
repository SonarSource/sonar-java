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
package org.sonar.java.checks.security;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6437")
public class CredentialsShouldNotBeHardcodedCheck extends IssuableSubscriptionVisitor {
  private static final Logger LOG = Loggers.get(CredentialsShouldNotBeHardcodedCheck.class);

  private static List<MethodMatchers> methodMatchers;

  public CredentialsShouldNotBeHardcodedCheck() {
    loadSignatures();
  }

  private static synchronized void loadSignatures() {
    if (methodMatchers != null) {
      return;
    }
    try {
      methodMatchers = loadAppSecRecords(Path.of("..", "credentials-methods.json"));
    } catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    for (MethodMatchers matcher : methodMatchers) {
      if (matcher.matches(invocation)) {
        reportIssue(invocation, "");
        return;
      }
    }
  }

  static List<MethodMatchers> loadAppSecRecords(Path path) throws IOException {
    Gson gson = new Gson();
    Type appSecRecordsCollection = new TypeToken<List<List<String>>>() {
    }.getType();
    String rawData;
    try (InputStream in = new FileInputStream(path.toFile())) {
      rawData = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    List<List<String>> appSecRecords = gson.fromJson(rawData, appSecRecordsCollection);
    return appSecRecords.stream()
      .map(AppSecRecord::new)
      .map(CredentialsShouldNotBeHardcodedCheck::convertToMatchers)
      .collect(Collectors.toList());
  }

  private static MethodMatchers convertToMatchers(AppSecRecord appSecRecord) {
    int argumentListStart = appSecRecord.method.indexOf('(');
    int argumentListEnd = appSecRecord.method.indexOf(')', argumentListStart);
    String type = appSecRecord.artifactId + "." + appSecRecord.classType;
    int numberOfArguments = appSecRecord.method.substring(argumentListStart + 1, argumentListEnd).split(",").length;
    if (appSecRecord.methodType.equals("Constructor")) {
      return MethodMatchers.create()
        .ofTypes(type)
        .constructor()
        .addParametersMatcher(argumentList -> argumentList.size() == numberOfArguments)
        .build();
    }

    String methodName = appSecRecord.method.substring(0, argumentListStart);
    return MethodMatchers.create()
      .ofTypes(type)
      .names(methodName)
      .addParametersMatcher(argumentList-> argumentList.size() == numberOfArguments)
      .build();
  }

  static class AppSecRecord {
    public final String groupId;
    public final String artifactId;
    public final String namespace;
    public final String classType;
    public final String methodType;
    public final String methodModifiersAndReturnType;
    public final String method;
    public final String argumentIndex;

    public AppSecRecord(List<String> record) {
      this.groupId = record.get(1);
      this.artifactId = record.get(2);
      this.namespace = record.get(3);
      this.classType = record.get(4);
      this.methodType = record.get(5);
      this.methodModifiersAndReturnType = record.get(6);
      this.method = record.get(7);
      this.argumentIndex = record.get(8);
    }
  }
}
