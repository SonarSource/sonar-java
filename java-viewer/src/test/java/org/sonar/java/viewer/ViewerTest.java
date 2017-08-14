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
      assertThat(EntityUtils.toString(resp.getEntity())).contains("<p>java.lang.NullPointerException: Unable to find a method in first class.<br/>");
    }

  }


  @Test
  public void test_complex_code_generation() throws Exception {
    String source = "class A {"
      + "  private Object foo(boolean b) {"
      + "    if (bar()) {"
      + "      if (b) {"
      + "        return null;"
      + "      }"
      + "      this.throwing();"
      + "    }"
      + "    return new Object();"
      + "  }"
      + "  private boolean bar() {"
      + "    return true;"
      + "  }"
      + "  private Object throwing() {"
      + "    throw new IllegalStateException(\"ise\");"
      + "  }"
      + "}";
    Map<String, String> values = Viewer.getValues(source);

    assertThat(values.get("cfg")).isNotEmpty();

    assertThat(values.get("dotAST")).isEqualTo("graph AST {\\n0[label=\"COMPILATION_UNIT L#1\",highlighting=\"firstNode\"];\\n1[label=\"CLASS L#1\",highlighting=\"classKind\"];\\n2[label=\"MODIFIERS\"];\\n1->2[];\\n3[label=\"TOKEN L#1\"];\\n3[label=\"class\",highlighting=\"tokenKind\"];\\n1->3[];\\n4[label=\"IDENTIFIER L#1\"];\\n5[label=\"TOKEN L#1\"];\\n5[label=\"A\",highlighting=\"tokenKind\"];\\n4->5[];\\n1->4[];\\n6[label=\"TYPE_PARAMETERS\"];\\n1->6[];\\n7[label=\"LIST\"];\\n1->7[];\\n8[label=\"TOKEN L#1\"];\\n8[label=\"{\",highlighting=\"tokenKind\"];\\n1->8[];\\n9[label=\"METHOD L#1\",highlighting=\"methodKind\"];\\n10[label=\"MODIFIERS L#1\"];\\n11[label=\"TOKEN L#1\"];\\n11[label=\"private\",highlighting=\"tokenKind\"];\\n10->11[];\\n9->10[];\\n12[label=\"TYPE_PARAMETERS\"];\\n9->12[];\\n13[label=\"IDENTIFIER L#1\"];\\n14[label=\"TOKEN L#1\"];\\n14[label=\"Object\",highlighting=\"tokenKind\"];\\n13->14[];\\n9->13[];\\n15[label=\"IDENTIFIER L#1\"];\\n16[label=\"TOKEN L#1\"];\\n16[label=\"foo\",highlighting=\"tokenKind\"];\\n15->16[];\\n9->15[];\\n17[label=\"TOKEN L#1\"];\\n17[label=\"(\",highlighting=\"tokenKind\"];\\n9->17[];\\n18[label=\"VARIABLE L#1\"];\\n19[label=\"MODIFIERS\"];\\n18->19[];\\n20[label=\"PRIMITIVE_TYPE L#1\"];\\n21[label=\"TOKEN L#1\"];\\n21[label=\"boolean\",highlighting=\"tokenKind\"];\\n20->21[];\\n18->20[];\\n22[label=\"IDENTIFIER L#1\"];\\n23[label=\"TOKEN L#1\"];\\n23[label=\"b\",highlighting=\"tokenKind\"];\\n22->23[];\\n18->22[];\\n9->18[];\\n24[label=\"TOKEN L#1\"];\\n24[label=\")\",highlighting=\"tokenKind\"];\\n9->24[];\\n25[label=\"BLOCK L#1\"];\\n26[label=\"TOKEN L#1\"];\\n26[label=\"{\",highlighting=\"tokenKind\"];\\n25->26[];\\n27[label=\"IF_STATEMENT L#1\"];\\n28[label=\"TOKEN L#1\"];\\n28[label=\"if\",highlighting=\"tokenKind\"];\\n27->28[];\\n29[label=\"TOKEN L#1\"];\\n29[label=\"(\",highlighting=\"tokenKind\"];\\n27->29[];\\n30[label=\"METHOD_INVOCATION L#1\"];\\n31[label=\"IDENTIFIER L#1\"];\\n32[label=\"TOKEN L#1\"];\\n32[label=\"bar\",highlighting=\"tokenKind\"];\\n31->32[];\\n30->31[];\\n33[label=\"ARGUMENTS L#1\"];\\n34[label=\"TOKEN L#1\"];\\n34[label=\"(\",highlighting=\"tokenKind\"];\\n33->34[];\\n35[label=\"TOKEN L#1\"];\\n35[label=\")\",highlighting=\"tokenKind\"];\\n33->35[];\\n30->33[];\\n27->30[];\\n36[label=\"TOKEN L#1\"];\\n36[label=\")\",highlighting=\"tokenKind\"];\\n27->36[];\\n37[label=\"BLOCK L#1\"];\\n38[label=\"TOKEN L#1\"];\\n38[label=\"{\",highlighting=\"tokenKind\"];\\n37->38[];\\n39[label=\"IF_STATEMENT L#1\"];\\n40[label=\"TOKEN L#1\"];\\n40[label=\"if\",highlighting=\"tokenKind\"];\\n39->40[];\\n41[label=\"TOKEN L#1\"];\\n41[label=\"(\",highlighting=\"tokenKind\"];\\n39->41[];\\n42[label=\"IDENTIFIER L#1\"];\\n43[label=\"TOKEN L#1\"];\\n43[label=\"b\",highlighting=\"tokenKind\"];\\n42->43[];\\n39->42[];\\n44[label=\"TOKEN L#1\"];\\n44[label=\")\",highlighting=\"tokenKind\"];\\n39->44[];\\n45[label=\"BLOCK L#1\"];\\n46[label=\"TOKEN L#1\"];\\n46[label=\"{\",highlighting=\"tokenKind\"];\\n45->46[];\\n47[label=\"RETURN_STATEMENT L#1\"];\\n48[label=\"TOKEN L#1\"];\\n48[label=\"return\",highlighting=\"tokenKind\"];\\n47->48[];\\n49[label=\"NULL_LITERAL L#1\"];\\n50[label=\"TOKEN L#1\"];\\n50[label=\"null\",highlighting=\"tokenKind\"];\\n49->50[];\\n47->49[];\\n51[label=\"TOKEN L#1\"];\\n51[label=\";\",highlighting=\"tokenKind\"];\\n47->51[];\\n45->47[];\\n52[label=\"TOKEN L#1\"];\\n52[label=\"}\",highlighting=\"tokenKind\"];\\n45->52[];\\n39->45[];\\n37->39[];\\n53[label=\"EXPRESSION_STATEMENT L#1\"];\\n54[label=\"METHOD_INVOCATION L#1\"];\\n55[label=\"MEMBER_SELECT L#1\"];\\n56[label=\"IDENTIFIER L#1\"];\\n57[label=\"TOKEN L#1\"];\\n57[label=\"this\",highlighting=\"tokenKind\"];\\n56->57[];\\n55->56[];\\n58[label=\"TOKEN L#1\"];\\n58[label=\".\",highlighting=\"tokenKind\"];\\n55->58[];\\n59[label=\"IDENTIFIER L#1\"];\\n60[label=\"TOKEN L#1\"];\\n60[label=\"throwing\",highlighting=\"tokenKind\"];\\n59->60[];\\n55->59[];\\n54->55[];\\n61[label=\"ARGUMENTS L#1\"];\\n62[label=\"TOKEN L#1\"];\\n62[label=\"(\",highlighting=\"tokenKind\"];\\n61->62[];\\n63[label=\"TOKEN L#1\"];\\n63[label=\")\",highlighting=\"tokenKind\"];\\n61->63[];\\n54->61[];\\n53->54[];\\n64[label=\"TOKEN L#1\"];\\n64[label=\";\",highlighting=\"tokenKind\"];\\n53->64[];\\n37->53[];\\n65[label=\"TOKEN L#1\"];\\n65[label=\"}\",highlighting=\"tokenKind\"];\\n37->65[];\\n27->37[];\\n25->27[];\\n66[label=\"RETURN_STATEMENT L#1\"];\\n67[label=\"TOKEN L#1\"];\\n67[label=\"return\",highlighting=\"tokenKind\"];\\n66->67[];\\n68[label=\"NEW_CLASS L#1\"];\\n69[label=\"TOKEN L#1\"];\\n69[label=\"new\",highlighting=\"tokenKind\"];\\n68->69[];\\n70[label=\"IDENTIFIER L#1\"];\\n71[label=\"TOKEN L#1\"];\\n71[label=\"Object\",highlighting=\"tokenKind\"];\\n70->71[];\\n68->70[];\\n72[label=\"ARGUMENTS L#1\"];\\n73[label=\"TOKEN L#1\"];\\n73[label=\"(\",highlighting=\"tokenKind\"];\\n72->73[];\\n74[label=\"TOKEN L#1\"];\\n74[label=\")\",highlighting=\"tokenKind\"];\\n72->74[];\\n68->72[];\\n66->68[];\\n75[label=\"TOKEN L#1\"];\\n75[label=\";\",highlighting=\"tokenKind\"];\\n66->75[];\\n25->66[];\\n76[label=\"TOKEN L#1\"];\\n76[label=\"}\",highlighting=\"tokenKind\"];\\n25->76[];\\n9->25[];\\n1->9[];\\n77[label=\"METHOD L#1\",highlighting=\"methodKind\"];\\n78[label=\"MODIFIERS L#1\"];\\n79[label=\"TOKEN L#1\"];\\n79[label=\"private\",highlighting=\"tokenKind\"];\\n78->79[];\\n77->78[];\\n80[label=\"TYPE_PARAMETERS\"];\\n77->80[];\\n81[label=\"PRIMITIVE_TYPE L#1\"];\\n82[label=\"TOKEN L#1\"];\\n82[label=\"boolean\",highlighting=\"tokenKind\"];\\n81->82[];\\n77->81[];\\n83[label=\"IDENTIFIER L#1\"];\\n84[label=\"TOKEN L#1\"];\\n84[label=\"bar\",highlighting=\"tokenKind\"];\\n83->84[];\\n77->83[];\\n85[label=\"TOKEN L#1\"];\\n85[label=\"(\",highlighting=\"tokenKind\"];\\n77->85[];\\n86[label=\"TOKEN L#1\"];\\n86[label=\")\",highlighting=\"tokenKind\"];\\n77->86[];\\n87[label=\"BLOCK L#1\"];\\n88[label=\"TOKEN L#1\"];\\n88[label=\"{\",highlighting=\"tokenKind\"];\\n87->88[];\\n89[label=\"RETURN_STATEMENT L#1\"];\\n90[label=\"TOKEN L#1\"];\\n90[label=\"return\",highlighting=\"tokenKind\"];\\n89->90[];\\n91[label=\"BOOLEAN_LITERAL L#1\"];\\n92[label=\"TOKEN L#1\"];\\n92[label=\"true\",highlighting=\"tokenKind\"];\\n91->92[];\\n89->91[];\\n93[label=\"TOKEN L#1\"];\\n93[label=\";\",highlighting=\"tokenKind\"];\\n89->93[];\\n87->89[];\\n94[label=\"TOKEN L#1\"];\\n94[label=\"}\",highlighting=\"tokenKind\"];\\n87->94[];\\n77->87[];\\n1->77[];\\n95[label=\"METHOD L#1\",highlighting=\"methodKind\"];\\n96[label=\"MODIFIERS L#1\"];\\n97[label=\"TOKEN L#1\"];\\n97[label=\"private\",highlighting=\"tokenKind\"];\\n96->97[];\\n95->96[];\\n98[label=\"TYPE_PARAMETERS\"];\\n95->98[];\\n99[label=\"IDENTIFIER L#1\"];\\n100[label=\"TOKEN L#1\"];\\n100[label=\"Object\",highlighting=\"tokenKind\"];\\n99->100[];\\n95->99[];\\n101[label=\"IDENTIFIER L#1\"];\\n102[label=\"TOKEN L#1\"];\\n102[label=\"throwing\",highlighting=\"tokenKind\"];\\n101->102[];\\n95->101[];\\n103[label=\"TOKEN L#1\"];\\n103[label=\"(\",highlighting=\"tokenKind\"];\\n95->103[];\\n104[label=\"TOKEN L#1\"];\\n104[label=\")\",highlighting=\"tokenKind\"];\\n95->104[];\\n105[label=\"BLOCK L#1\"];\\n106[label=\"TOKEN L#1\"];\\n106[label=\"{\",highlighting=\"tokenKind\"];\\n105->106[];\\n107[label=\"THROW_STATEMENT L#1\"];\\n108[label=\"TOKEN L#1\"];\\n108[label=\"throw\",highlighting=\"tokenKind\"];\\n107->108[];\\n109[label=\"NEW_CLASS L#1\"];\\n110[label=\"TOKEN L#1\"];\\n110[label=\"new\",highlighting=\"tokenKind\"];\\n109->110[];\\n111[label=\"IDENTIFIER L#1\"];\\n112[label=\"TOKEN L#1\"];\\n112[label=\"IllegalStateException\",highlighting=\"tokenKind\"];\\n111->112[];\\n109->111[];\\n113[label=\"ARGUMENTS L#1\"];\\n114[label=\"TOKEN L#1\"];\\n114[label=\"(\",highlighting=\"tokenKind\"];\\n113->114[];\\n115[label=\"STRING_LITERAL L#1\"];\\n116[label=\"TOKEN L#1\"];\\n116[label=\"\"ise\"\",highlighting=\"tokenKind\"];\\n115->116[];\\n113->115[];\\n117[label=\"TOKEN L#1\"];\\n117[label=\")\",highlighting=\"tokenKind\"];\\n113->117[];\\n109->113[];\\n107->109[];\\n118[label=\"TOKEN L#1\"];\\n118[label=\";\",highlighting=\"tokenKind\"];\\n107->118[];\\n105->107[];\\n119[label=\"TOKEN L#1\"];\\n119[label=\"}\",highlighting=\"tokenKind\"];\\n105->119[];\\n95->105[];\\n1->95[];\\n120[label=\"TOKEN L#1\"];\\n120[label=\"}\",highlighting=\"tokenKind\"];\\n1->120[];\\n0->1[];\\n121[label=\"TOKEN L#1\"];\\n121[label=\"\",highlighting=\"tokenKind\"];\\n0->121[];\\n\\n}");
    assertThat(values.get("dotCFG")).isEqualTo("graph CFG {\\n5[label=\"B5 (START)\",highlighting=\"firstNode\"];\\n4[label=\"B4\"];\\n3[label=\"B3\"];\\n2[label=\"B2\"];\\n1[label=\"B1\"];\\n0[label=\"B0 (EXIT)\",highlighting=\"exitNode\"];\\n5->1[label=\"FALSE\"];\\n5->4[label=\"TRUE\"];\\n4->2[label=\"FALSE\"];\\n4->3[label=\"TRUE\"];\\n3->0[label=\"EXIT\"];\\n2->1[];\\n1->0[label=\"EXIT\"];\\n\\n}");
    // FIXME: dot graph of EG is not consistent between calls
    assertThat(values.get("dotEG")).isNotEmpty();

    assertThat(values.get("errorMessage")).isEmpty();
    assertThat(values.get("errorStackTrace")).isEmpty();
  }
}
