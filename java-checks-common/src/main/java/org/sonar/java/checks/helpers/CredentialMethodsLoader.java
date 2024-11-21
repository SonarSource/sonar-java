/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CredentialMethodsLoader {
  private CredentialMethodsLoader() {
    /* Should not be invoked */
  }

  public static Map<String, List<CredentialMethod>> load(String resourcePath) throws IOException {
    String rawData;
    try (InputStream in = CredentialMethodsLoader.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IOException(String.format("Could not load methods from \"%s\".", resourcePath));
      }
      rawData = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    CredentialMethod[] credentialMethods = new Gson().fromJson(rawData, CredentialMethod[].class);
    return Arrays.stream(credentialMethods).collect(Collectors.groupingBy(m -> m.name));
  }
}
