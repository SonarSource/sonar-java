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

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LaunchDarklyFeatureFlagsTest {

  private static final String FLAG = "sonar-java-semsitter-frontend";

  private final List<String> requests = new ArrayList<>();
  private HttpServer server;
  private int status;
  private String body;

  @BeforeEach
  void startServer() throws IOException {
    status = 200;
    body = "{}";
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", exchange -> {
      requests.add(exchange.getRequestURI().getPath());
      byte[] answer = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, answer.length);
      exchange.getResponseBody().write(answer);
      exchange.close();
    });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void flag_on_is_read() {
    body = "{\"" + FLAG + "\":{\"value\":true,\"version\":3}}";

    assertThat(flags("my-project").isEnabled(FLAG, false)).isTrue();
  }

  @Test
  void flag_off_is_read() {
    body = "{\"" + FLAG + "\":{\"value\":false,\"version\":3}}";

    assertThat(flags("my-project").isEnabled(FLAG, true)).isFalse();
  }

  @Test
  void request_targets_the_client_side_id_and_the_project_context() {
    flags("my-project");

    assertThat(requests).hasSize(1);
    String[] path = requests.get(0).split("/");
    assertThat(path).startsWith("", "sdk", "evalx", "client-id", "contexts");
    String context = new String(Base64.getUrlDecoder().decode(path[5]), StandardCharsets.UTF_8);
    assertThat(JsonParser.parseString(context).getAsJsonObject().get("kind").getAsString()).isEqualTo("project");
    assertThat(JsonParser.parseString(context).getAsJsonObject().get("key").getAsString()).isEqualTo("my-project");
  }

  @Test
  void unknown_flag_has_the_default_value() {
    FeatureFlags flags = flags("p");

    assertThat(flags.isEnabled("missing", true)).isTrue();
    assertThat(flags.isEnabled("missing", false)).isFalse();
  }

  @Test
  void flag_that_is_not_a_boolean_has_the_default_value() {
    body = "{\"" + FLAG + "\":{\"value\":\"yes\"},\"other\":3}";
    FeatureFlags flags = flags("p");

    assertThat(flags.isEnabled(FLAG, true)).isTrue();
    assertThat(flags.isEnabled(FLAG, false)).isFalse();
    assertThat(flags.isEnabled("other", false)).isFalse();
  }

  @Test
  void failing_service_leaves_the_default_value() {
    status = 500;
    body = "{\"" + FLAG + "\":{\"value\":true}}";
    FeatureFlags flags = flags("p");

    assertThat(flags.isEnabled(FLAG, false)).isFalse();
    assertThat(flags.isEnabled(FLAG, true)).isTrue();
  }

  @Test
  void malformed_answer_leaves_the_default_value() {
    body = "not json";

    assertThat(flags("p").isEnabled(FLAG, true)).isTrue();
  }

  @Test
  void unreachable_service_leaves_the_default_value() {
    server.stop(0);

    assertThat(flags("p").isEnabled(FLAG, true)).isTrue();
    assertThat(flags("p").isEnabled(FLAG, false)).isFalse();
  }

  private FeatureFlags flags(String project) {
    return LaunchDarklyFeatureFlags.connect(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), "client-id", project);
  }
}
