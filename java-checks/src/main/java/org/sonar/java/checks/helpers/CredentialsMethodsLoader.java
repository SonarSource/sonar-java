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
package org.sonar.java.checks.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

/**
 * Provides the tooling to load method signatures from a JSON file provided by AppSec.
 */
public class CredentialsMethodsLoader {
  private static final Type CREDENTIALS_METHODS_JSON_TYPE = new TypeToken<List<List<String>>>() {}.getType();
  private CredentialsMethodsLoader() {
    /* No concrete instance of this helper should be created */
  }

  public static Map<String, List<CredentialsMethod>> load(String resourcePath) throws IOException {
    Gson gson = new Gson();
    String rawData;
    try (InputStream in = CredentialsMethodsLoader.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IOException(String.format("Could not load methods from \"%s\".", resourcePath));
      }
      rawData = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    List<List<String>> jsonRecords = gson.fromJson(rawData, CREDENTIALS_METHODS_JSON_TYPE);
    Map<String, List<CredentialsMethod>> methodsGroupedByName = new TreeMap<>();
    for (List<String> jsonRecord : jsonRecords) {
      CredentialsMethod method = new CredentialsMethod(jsonRecord);
      if (methodsGroupedByName.containsKey(method.methodName)) {
        methodsGroupedByName.get(method.methodName).add(method);
      } else {
        List<CredentialsMethod> methods = new ArrayList<>();
        methods.add(method);
        methodsGroupedByName.put(method.methodName, methods);
      }
    }
    return methodsGroupedByName;
  }

  public static class CredentialsMethod {
    public final String namespace;
    public final String classType;
    public final String className;
    public final String methodType;
    public final String modifiersAndReturnType;
    public final String method;
    public final String methodName;
    public final List<TargetArgument> targetArguments;
    public final MethodMatchers methodMatcher;

    public CredentialsMethod(List<String> entry) {
      this.namespace = entry.get(2);
      this.classType = entry.get(3);
      this.className = entry.get(4);
      this.methodType = entry.get(5);
      this.modifiersAndReturnType = entry.get(6);
      this.method = entry.get(7);
      this.methodName = extractMethodName(this.method);
      List<Integer> argumentIndices = Stream.of(entry.get(8).split(","))
        .map(index -> Integer.valueOf(index.trim()) - 1)
        .collect(Collectors.toList());
      this.targetArguments = extractArguments(this.method, argumentIndices);
      this.methodMatcher = convertToMatchers(this);
    }

    private static MethodMatchers convertToMatchers(CredentialsMethod credentialsMethod) {
      int argumentListStart = credentialsMethod.method.indexOf('(');
      int argumentListEnd = credentialsMethod.method.indexOf(')', argumentListStart);
      String type = credentialsMethod.namespace + "." + credentialsMethod.className;
      int numberOfArguments = credentialsMethod.method.substring(argumentListStart + 1, argumentListEnd).split(",").length;

      if (credentialsMethod.methodType.equals("Constructor")) {
        return MethodMatchers.create()
          .ofTypes(type)
          .constructor()
          .addParametersMatcher(argumentList -> argumentList.size() == numberOfArguments)
          .build();
      }

      return MethodMatchers.create()
        .ofTypes(type)
        .names(credentialsMethod.methodName)
        .addParametersMatcher(argumentList -> argumentList.size() == numberOfArguments)
        .build();
    }

    private static String extractMethodName(String signature) {
      int argumentListStart = signature.indexOf('(');
      return signature.substring(0, argumentListStart);
    }

    private static List<TargetArgument> extractArguments(String signature, List<Integer> indices) {
      List<List<String>> arguments = splitArgumentTypeAndName(signature);
      return indices.stream()
        .filter(index -> 0 <= index && index < arguments.size())
        .map(index -> new TargetArgument(arguments.get(index).get(0), arguments.get(index).get(1), index))
        .collect(Collectors.toList());
    }

    private static List<List<String>> splitArgumentTypeAndName(String signature) {
      return tokenizeArguments(signature).stream()
        .map(argumentString -> {
          int index = argumentString.lastIndexOf(" ");
          return List.of(
            matchType(argumentString.substring(0, index).trim()),
            argumentString.substring(index).trim()
          );
        }).collect(Collectors.toList());
    }

    private static List<String> tokenizeArguments(String signature) {
      int start = signature.indexOf('(');
      int end = signature.indexOf(')');
      String parameters = signature.substring(start + 1, end);
      List<String> types = new ArrayList<>();
      int depth = 0;
      start = 0;
      for (int index = 0; index < parameters.length(); index++) {
        char character = parameters.charAt(index);
        switch (character) {
          case ',':
            if (depth == 0) {
              end = index;
              types.add(parameters.substring(start, end));
              start = end + 1;
            }
            break;
          case '<':
            depth++;
            break;
          case '>':
            depth--;
            break;
          default:
            break;
        }
      }
      types.add(parameters.substring(start));
      return types;
    }

    private static String matchType(String type) {
      //FIXME handle type erasure
      switch (type) {
        case "byte[]":
          return "java.lang.byte[]";
        case "char[]":
          return "java.lang.char[]";
        case "String":
          return "java.lang.String";
        default:
          return type;
      }
    }
  }

  public static class TargetArgument {
    public final String type;
    public final String name;
    public final int index;

    TargetArgument(String type, String name, int index) {
      this.type = type;
      this.name = name;
      this.index = index;
    }
  }
}
