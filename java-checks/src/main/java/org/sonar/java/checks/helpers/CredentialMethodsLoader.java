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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    Gson gson = new GsonBuilder()
      .registerTypeAdapter(CredentialMethod.class, new Deserializer())
      .create();
    CredentialMethod[] credentialMethods = gson.fromJson(rawData, CredentialMethod[].class);
    return Arrays.stream(credentialMethods).collect(Collectors.groupingBy(m -> m.name));
  }

  private static class Deserializer implements JsonDeserializer<CredentialMethod> {
    @Override
    public CredentialMethod deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
      JsonObject object = jsonElement.getAsJsonObject();
      JsonArray argsInArray = object.get("args").getAsJsonArray();
      List<String> args = new ArrayList<>(argsInArray.size());
      for (var arg :argsInArray) {
        args.add(arg.getAsString());
      }
      JsonArray indexes = object.get("indexes").getAsJsonArray();
      List<Integer> indices = new ArrayList<>(indexes.size());
      for (var index: indexes) {
        int computed = index.getAsInt() - 1;
        indices.add(computed);
      }
      return new CredentialMethod(
        object.get("cls").getAsString(),
        object.get("name").getAsString(),
        args,
        indices
      );
    }
  }
}
