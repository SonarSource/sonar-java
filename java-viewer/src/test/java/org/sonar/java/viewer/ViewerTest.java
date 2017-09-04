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
      + "    throw new IllegalStateException(\"ise?\");"
      + "  }"
      + "}";
    Map<String, String> values = Viewer.getValues(source);

    assertThat(values.get("cfg")).isNotEmpty();

    assertThat(values.get("dotAST")).isEqualTo("graph AST {0[label=\"COMPILATION_UNIT L#1\",highlighting=\"firstNode\"];1[label=\"CLASS L#1\",highlighting=\"classKind\"];2[label=\"MODIFIERS\"];1->2[];3[label=\"TOKEN L#1\"];3[label=\"class\",highlighting=\"tokenKind\"];1->3[];4[label=\"IDENTIFIER L#1\"];5[label=\"TOKEN L#1\"];5[label=\"A\",highlighting=\"tokenKind\"];4->5[];1->4[];6[label=\"TYPE_PARAMETERS\"];1->6[];7[label=\"LIST\"];1->7[];8[label=\"TOKEN L#1\"];8[label=\"{\",highlighting=\"tokenKind\"];1->8[];9[label=\"METHOD L#1\",highlighting=\"methodKind\"];10[label=\"MODIFIERS L#1\"];11[label=\"TOKEN L#1\"];11[label=\"private\",highlighting=\"tokenKind\"];10->11[];9->10[];12[label=\"TYPE_PARAMETERS\"];9->12[];13[label=\"IDENTIFIER L#1\"];14[label=\"TOKEN L#1\"];14[label=\"Object\",highlighting=\"tokenKind\"];13->14[];9->13[];15[label=\"IDENTIFIER L#1\"];16[label=\"TOKEN L#1\"];16[label=\"foo\",highlighting=\"tokenKind\"];15->16[];9->15[];17[label=\"TOKEN L#1\"];17[label=\"(\",highlighting=\"tokenKind\"];9->17[];18[label=\"VARIABLE L#1\"];19[label=\"MODIFIERS\"];18->19[];20[label=\"PRIMITIVE_TYPE L#1\"];21[label=\"TOKEN L#1\"];21[label=\"boolean\",highlighting=\"tokenKind\"];20->21[];18->20[];22[label=\"IDENTIFIER L#1\"];23[label=\"TOKEN L#1\"];23[label=\"b\",highlighting=\"tokenKind\"];22->23[];18->22[];9->18[];24[label=\"TOKEN L#1\"];24[label=\")\",highlighting=\"tokenKind\"];9->24[];25[label=\"BLOCK L#1\"];26[label=\"TOKEN L#1\"];26[label=\"{\",highlighting=\"tokenKind\"];25->26[];27[label=\"IF_STATEMENT L#1\"];28[label=\"TOKEN L#1\"];28[label=\"if\",highlighting=\"tokenKind\"];27->28[];29[label=\"TOKEN L#1\"];29[label=\"(\",highlighting=\"tokenKind\"];27->29[];30[label=\"METHOD_INVOCATION L#1\"];31[label=\"IDENTIFIER L#1\"];32[label=\"TOKEN L#1\"];32[label=\"bar\",highlighting=\"tokenKind\"];31->32[];30->31[];33[label=\"ARGUMENTS L#1\"];34[label=\"TOKEN L#1\"];34[label=\"(\",highlighting=\"tokenKind\"];33->34[];35[label=\"TOKEN L#1\"];35[label=\")\",highlighting=\"tokenKind\"];33->35[];30->33[];27->30[];36[label=\"TOKEN L#1\"];36[label=\")\",highlighting=\"tokenKind\"];27->36[];37[label=\"BLOCK L#1\"];38[label=\"TOKEN L#1\"];38[label=\"{\",highlighting=\"tokenKind\"];37->38[];39[label=\"IF_STATEMENT L#1\"];40[label=\"TOKEN L#1\"];40[label=\"if\",highlighting=\"tokenKind\"];39->40[];41[label=\"TOKEN L#1\"];41[label=\"(\",highlighting=\"tokenKind\"];39->41[];42[label=\"IDENTIFIER L#1\"];43[label=\"TOKEN L#1\"];43[label=\"b\",highlighting=\"tokenKind\"];42->43[];39->42[];44[label=\"TOKEN L#1\"];44[label=\")\",highlighting=\"tokenKind\"];39->44[];45[label=\"BLOCK L#1\"];46[label=\"TOKEN L#1\"];46[label=\"{\",highlighting=\"tokenKind\"];45->46[];47[label=\"RETURN_STATEMENT L#1\"];48[label=\"TOKEN L#1\"];48[label=\"return\",highlighting=\"tokenKind\"];47->48[];49[label=\"NULL_LITERAL L#1\"];50[label=\"TOKEN L#1\"];50[label=\"null\",highlighting=\"tokenKind\"];49->50[];47->49[];51[label=\"TOKEN L#1\"];51[label=\";\",highlighting=\"tokenKind\"];47->51[];45->47[];52[label=\"TOKEN L#1\"];52[label=\"}\",highlighting=\"tokenKind\"];45->52[];39->45[];37->39[];53[label=\"EXPRESSION_STATEMENT L#1\"];54[label=\"METHOD_INVOCATION L#1\"];55[label=\"MEMBER_SELECT L#1\"];56[label=\"IDENTIFIER L#1\"];57[label=\"TOKEN L#1\"];57[label=\"this\",highlighting=\"tokenKind\"];56->57[];55->56[];58[label=\"TOKEN L#1\"];58[label=\".\",highlighting=\"tokenKind\"];55->58[];59[label=\"IDENTIFIER L#1\"];60[label=\"TOKEN L#1\"];60[label=\"throwing\",highlighting=\"tokenKind\"];59->60[];55->59[];54->55[];61[label=\"ARGUMENTS L#1\"];62[label=\"TOKEN L#1\"];62[label=\"(\",highlighting=\"tokenKind\"];61->62[];63[label=\"TOKEN L#1\"];63[label=\")\",highlighting=\"tokenKind\"];61->63[];54->61[];53->54[];64[label=\"TOKEN L#1\"];64[label=\";\",highlighting=\"tokenKind\"];53->64[];37->53[];65[label=\"TOKEN L#1\"];65[label=\"}\",highlighting=\"tokenKind\"];37->65[];27->37[];25->27[];66[label=\"RETURN_STATEMENT L#1\"];67[label=\"TOKEN L#1\"];67[label=\"return\",highlighting=\"tokenKind\"];66->67[];68[label=\"NEW_CLASS L#1\"];69[label=\"TOKEN L#1\"];69[label=\"new\",highlighting=\"tokenKind\"];68->69[];70[label=\"IDENTIFIER L#1\"];71[label=\"TOKEN L#1\"];71[label=\"Object\",highlighting=\"tokenKind\"];70->71[];68->70[];72[label=\"ARGUMENTS L#1\"];73[label=\"TOKEN L#1\"];73[label=\"(\",highlighting=\"tokenKind\"];72->73[];74[label=\"TOKEN L#1\"];74[label=\")\",highlighting=\"tokenKind\"];72->74[];68->72[];66->68[];75[label=\"TOKEN L#1\"];75[label=\";\",highlighting=\"tokenKind\"];66->75[];25->66[];76[label=\"TOKEN L#1\"];76[label=\"}\",highlighting=\"tokenKind\"];25->76[];9->25[];1->9[];77[label=\"METHOD L#1\",highlighting=\"methodKind\"];78[label=\"MODIFIERS L#1\"];79[label=\"TOKEN L#1\"];79[label=\"private\",highlighting=\"tokenKind\"];78->79[];77->78[];80[label=\"TYPE_PARAMETERS\"];77->80[];81[label=\"PRIMITIVE_TYPE L#1\"];82[label=\"TOKEN L#1\"];82[label=\"boolean\",highlighting=\"tokenKind\"];81->82[];77->81[];83[label=\"IDENTIFIER L#1\"];84[label=\"TOKEN L#1\"];84[label=\"bar\",highlighting=\"tokenKind\"];83->84[];77->83[];85[label=\"TOKEN L#1\"];85[label=\"(\",highlighting=\"tokenKind\"];77->85[];86[label=\"TOKEN L#1\"];86[label=\")\",highlighting=\"tokenKind\"];77->86[];87[label=\"BLOCK L#1\"];88[label=\"TOKEN L#1\"];88[label=\"{\",highlighting=\"tokenKind\"];87->88[];89[label=\"RETURN_STATEMENT L#1\"];90[label=\"TOKEN L#1\"];90[label=\"return\",highlighting=\"tokenKind\"];89->90[];91[label=\"BOOLEAN_LITERAL L#1\"];92[label=\"TOKEN L#1\"];92[label=\"true\",highlighting=\"tokenKind\"];91->92[];89->91[];93[label=\"TOKEN L#1\"];93[label=\";\",highlighting=\"tokenKind\"];89->93[];87->89[];94[label=\"TOKEN L#1\"];94[label=\"}\",highlighting=\"tokenKind\"];87->94[];77->87[];1->77[];95[label=\"METHOD L#1\",highlighting=\"methodKind\"];96[label=\"MODIFIERS L#1\"];97[label=\"TOKEN L#1\"];97[label=\"private\",highlighting=\"tokenKind\"];96->97[];95->96[];98[label=\"TYPE_PARAMETERS\"];95->98[];99[label=\"IDENTIFIER L#1\"];100[label=\"TOKEN L#1\"];100[label=\"Object\",highlighting=\"tokenKind\"];99->100[];95->99[];101[label=\"IDENTIFIER L#1\"];102[label=\"TOKEN L#1\"];102[label=\"throwing\",highlighting=\"tokenKind\"];101->102[];95->101[];103[label=\"TOKEN L#1\"];103[label=\"(\",highlighting=\"tokenKind\"];95->103[];104[label=\"TOKEN L#1\"];104[label=\")\",highlighting=\"tokenKind\"];95->104[];105[label=\"BLOCK L#1\"];106[label=\"TOKEN L#1\"];106[label=\"{\",highlighting=\"tokenKind\"];105->106[];107[label=\"THROW_STATEMENT L#1\"];108[label=\"TOKEN L#1\"];108[label=\"throw\",highlighting=\"tokenKind\"];107->108[];109[label=\"NEW_CLASS L#1\"];110[label=\"TOKEN L#1\"];110[label=\"new\",highlighting=\"tokenKind\"];109->110[];111[label=\"IDENTIFIER L#1\"];112[label=\"TOKEN L#1\"];112[label=\"IllegalStateException\",highlighting=\"tokenKind\"];111->112[];109->111[];113[label=\"ARGUMENTS L#1\"];114[label=\"TOKEN L#1\"];114[label=\"(\",highlighting=\"tokenKind\"];113->114[];115[label=\"STRING_LITERAL L#1\"];116[label=\"TOKEN L#1\"];116[label=\"&quot;ise&quest;&quot;\",highlighting=\"tokenKind\"];115->116[];113->115[];117[label=\"TOKEN L#1\"];117[label=\")\",highlighting=\"tokenKind\"];113->117[];109->113[];107->109[];118[label=\"TOKEN L#1\"];118[label=\";\",highlighting=\"tokenKind\"];107->118[];105->107[];119[label=\"TOKEN L#1\"];119[label=\"}\",highlighting=\"tokenKind\"];105->119[];95->105[];1->95[];120[label=\"TOKEN L#1\"];120[label=\"}\",highlighting=\"tokenKind\"];1->120[];0->1[];121[label=\"TOKEN L#1\"];121[label=\"\",highlighting=\"tokenKind\"];0->121[];}");
    assertThat(values.get("dotCFG")).isEqualTo("graph CFG {5[label=\"B5 (START)\",highlighting=\"firstNode\"];4[label=\"B4\"];3[label=\"B3\"];2[label=\"B2\"];1[label=\"B1\"];0[label=\"B0 (EXIT)\",highlighting=\"exitNode\"];5->1[label=\"FALSE\"];5->4[label=\"TRUE\"];4->2[label=\"FALSE\"];4->3[label=\"TRUE\"];3->0[label=\"EXIT\"];2->1[];1->0[label=\"EXIT\"];}");
    String dotEG = values.get("dotEG");
    // FIXME: dot graph of EG is not consistent between calls
    assertThat(dotEG).isNotEmpty();
    // check for correctly built yields
    assertThat(dotEG).contains("?methodName?:?bar?");
    assertThat(dotEG).contains("?methodYields?:[{?params?:[],?result?:[?NOT_NULL?,?TRUE?],?resultIndex?:-1}]");

    assertThat(values.get("errorMessage")).isEmpty();
    assertThat(values.get("errorStackTrace")).isEmpty();
  }

}
