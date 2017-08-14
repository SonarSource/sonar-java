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
package org.sonar.java.viewer;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import spark.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ViewerTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void code_without_method_trigger_an_exception() {
    exception.expect(NullPointerException.class);
    exception.expectMessage("Unable to find a method in first class.");

    Viewer.getValues("class A { }");
  }

  @Test
  public void code_with_method_provide_everything_but_error_messages() {
    String source = "class A {"
      + "  int foo(boolean b) {"
      + "    if (b) {"
      + "      return 42;"
      + "    }"
      + "    return 21;"
      + "  }"
      + "}";
    Map<String, String> values = Viewer.getValues(source);

    assertThat(values.get("cfg")).isNotEmpty();

    assertThat(values.get("dotAST")).isNotEmpty();
    assertThat(values.get("dotCFG")).isNotEmpty();
    assertThat(values.get("dotEG")).isNotEmpty();

    assertThat(values.get("errorMessage")).isEmpty();
    assertThat(values.get("errorStackTrace")).isEmpty();
  }

  @Test
  public void values_with_error() {
    String message = "my exception message";
    Map<String, String> values = Viewer.getErrorValues(new Exception(message));

    assertThat(values.get("cfg")).isNull();

    assertThat(values.get("dotAST")).isNull();
    assertThat(values.get("dotCFG")).isNull();
    assertThat(values.get("dotEG")).isNull();

    assertThat(values.get("errorMessage")).isEqualTo(message);
    assertThat(values.get("errorStackTrace")).startsWith("java.lang.Exception: " + message);
  }

  @Test
  public void start_server_and_test_requests() throws Exception {
    ServerSocket serverSocket = new ServerSocket(0);
    int localPort = serverSocket.getLocalPort();
    serverSocket.close();
    String uri = "http://localhost:" + localPort + "/";
    Viewer.startWebServer(localPort, "class A {void fun() {}}");
    try(CloseableHttpClient client = HttpClients.createMinimal()) {
      CloseableHttpResponse resp = client.execute(new HttpGet(uri));
      assertThat(resp.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(EntityUtils.toString(resp.getEntity())).isEqualTo(IOUtils.toString(new FileInputStream(new File("src/test/resources/viewer_result1.html"))));

      // post with no data, answer with default code.
      HttpPost httpPost = new HttpPost(uri);
      resp = client.execute(httpPost);
      assertThat(resp.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(EntityUtils.toString(resp.getEntity())).isEqualTo(IOUtils.toString(new FileInputStream(new File("src/test/resources/viewer_result1.html"))));

      // render something besides default code
      httpPost = new HttpPost(uri);
      List<NameValuePair> postParameters = new ArrayList<>();
      postParameters.add(new BasicNameValuePair("javaCode", "class B{void meth() {}}"));
      httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
      resp = client.execute(httpPost);
      assertThat(resp.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(EntityUtils.toString(resp.getEntity())).isEqualTo(IOUtils.toString(new FileInputStream(new File("src/test/resources/viewer_result2.html"))));

      // send unproper code.
      httpPost = new HttpPost(uri);
      postParameters = new ArrayList<>();
      postParameters.add(new BasicNameValuePair("javaCode", "class B{}"));
      httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
      resp = client.execute(httpPost);
      assertThat(resp.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(EntityUtils.toString(resp.getEntity())).isEqualToIgnoringWhitespace(IOUtils.toString(new FileInputStream(new File("src/test/resources/viewer_result3.html"))));
    }

  }
}
