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
package org.sonar.java.semsitter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FeatureFlags} read from LaunchDarkly through its client-side evaluation endpoint, which keeps the plugin free of
 * the LaunchDarkly SDK and of everything the SDK drags along. The flags of a project are fetched once when the service
 * is opened, no analytics event is sent, and a service that does not answer in time leaves every flag at its default.
 */
public final class LaunchDarklyFeatureFlags implements FeatureFlags {

  static final String PROJECT_KIND = "project";
  static final String ENDPOINT = "https://clientsdk.launchdarkly.com";
  static final Duration TIMEOUT = Duration.ofSeconds(3);

  private static final Logger LOG = LoggerFactory.getLogger(LaunchDarklyFeatureFlags.class);

  private final JsonObject flags;

  LaunchDarklyFeatureFlags(JsonObject flags) {
    this.flags = flags;
  }

  public static FeatureFlags connect(String clientSideId, String projectKey) {
    return connect(URI.create(ENDPOINT), clientSideId, projectKey);
  }

  static FeatureFlags connect(URI endpoint, String clientSideId, String projectKey) {
    try {
      return new LaunchDarklyFeatureFlags(fetch(endpoint, clientSideId, projectKey));
    } catch (IOException | RuntimeException e) {
      LOG.debug("LaunchDarkly is not reachable, every flag keeps its default value: {}", e.getMessage());
      return new LaunchDarklyFeatureFlags(new JsonObject());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new LaunchDarklyFeatureFlags(new JsonObject());
    }
  }

  private static JsonObject fetch(URI endpoint, String clientSideId, String projectKey) throws IOException, InterruptedException {
    JsonObject context = new JsonObject();
    context.addProperty("kind", PROJECT_KIND);
    context.addProperty("key", projectKey);
    String encodedContext = Base64.getUrlEncoder().encodeToString(context.toString().getBytes(StandardCharsets.UTF_8));
    URI uri = URI.create(endpoint + "/sdk/evalx/" + clientSideId + "/contexts/" + encodedContext);
    HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
    HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() != 200) {
      throw new IOException("unexpected status " + response.statusCode());
    }
    return JsonParser.parseString(response.body()).getAsJsonObject();
  }

  @Override
  public boolean isEnabled(String flagKey, boolean defaultValue) {
    JsonElement flag = flags.get(flagKey);
    if (flag == null || !flag.isJsonObject()) {
      return defaultValue;
    }
    JsonElement value = flag.getAsJsonObject().get("value");
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
      return defaultValue;
    }
    return value.getAsBoolean();
  }
}
