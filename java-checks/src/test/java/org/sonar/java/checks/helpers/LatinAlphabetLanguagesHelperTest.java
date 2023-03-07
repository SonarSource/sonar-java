/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LatinAlphabetLanguagesHelperTest {
  final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
  final DecimalFormat decimalFormat = new DecimalFormat("#.#######", symbols);

  @Test
  void test() {
    assertThat(
      formatHumanLanguageScore(
        "Hello02139710238712987",
        "This is an english text!",
        "Hello",
        "Hello hello hello hello",
        "Hleol",
        "Hleol hleol",
        "Hleol Hello hleol",
        "Hleol Incomprehensibility hleol",
        "Incomprehensibility ",
        "slrwaxquavy",
        "SlRwAxQuAvY",
        "012345678",
        "images/blob/50281d86d6ed5c61975971150adf",
        "js/commit/8863b9d04c722b278fa93c5d66ad1e",
        "net/core/builder/e426a9ae7167c5807b173d5",
        "net/more/builder/3ad489866f41084fa4f3307",
        "project/commit/c5acf965067478784b54e2d24",
        "/var/lib/openshift/51122e382d5271c5ca000",
        "examples/commit/16ad89c4172c259f15bce56e",
        "examples/commit/8e1d746900f5411e9700fea0",
        "examples/commit/c95b6a84b6fd1efc832a46cd",
        "examples/commit/d6f6ef7457d99e31990fa64b",
        "examples/commit/ea15f07ce79366a08fee5b60",
        "cn/res/chinapostplan/structure/181041269",
        "com/istio/proxy/blob/bcdc1684df0839a6125",
        "com/kriskowal/q/blob/b0fa72980717dc202ff",
        "com/ph/logstash/de2ba3f964ae7039b7b74a4a",
        "default/src/test/java/org/xwiki/componen",
        "search_my_organization-example.json",
        "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH",
        "org.apache.catalina.startup.EXIT_ON_INIT_FAILURE",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567",
        "0123456789ABCDEFGHIJKLMNOPQRSTUV",
        "abcdefghijklmnopqrstuvwxyz",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
        "org.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT",
        "org.apache.tomcat.websocket.WS_AUTHENTICATION_PASSWORD"))
      .isEqualTo("Hello02139710238712987\n" +
        "-> 1.3262863\n" +
        "\n" +
        "This is an english text!\n" +
        "-> 4.6352161\n" +
        "\n" +
        "Hello\n" +
        "-> 4.7598878\n" +
        "\n" +
        "Hello hello hello hello\n" +
        "-> 4.7598878\n" +
        "\n" +
        "Hleol\n" +
        "-> 2.0215728\n" +
        "\n" +
        "Hleol hleol\n" +
        "-> 2.0215728\n" +
        "\n" +
        "Hleol Hello hleol\n" +
        "-> 2.9343445\n" +
        "\n" +
        "Hleol Incomprehensibility hleol\n" +
        "-> 3.5209606\n" +
        "\n" +
        "Incomprehensibility \n" +
        "-> 4.3978577\n" +
        "\n" +
        "slrwaxquavy\n" +
        "-> 0.783135\n" +
        "\n" +
        "SlRwAxQuAvY\n" +
        "-> 0.5729079\n" +
        "\n" +
        "012345678\n" +
        "-> 1\n" +
        "\n" +
        "images/blob/50281d86d6ed5c61975971150adf\n" +
        "-> 1.1821769\n" +
        "\n" +
        "js/commit/8863b9d04c722b278fa93c5d66ad1e\n" +
        "-> 0.9126614\n" +
        "\n" +
        "net/core/builder/e426a9ae7167c5807b173d5\n" +
        "-> 1.9399531\n" +
        "\n" +
        "net/more/builder/3ad489866f41084fa4f3307\n" +
        "-> 1.9014789\n" +
        "\n" +
        "project/commit/c5acf965067478784b54e2d24\n" +
        "-> 1.2177787\n" +
        "\n" +
        "/var/lib/openshift/51122e382d5271c5ca000\n" +
        "-> 1.3230153\n" +
        "\n" +
        "examples/commit/16ad89c4172c259f15bce56e\n" +
        "-> 1.6869377\n" +
        "\n" +
        "examples/commit/8e1d746900f5411e9700fea0\n" +
        "-> 1.48724\n" +
        "\n" +
        "examples/commit/c95b6a84b6fd1efc832a46cd\n" +
        "-> 1.503256\n" +
        "\n" +
        "examples/commit/d6f6ef7457d99e31990fa64b\n" +
        "-> 1.4204883\n" +
        "\n" +
        "examples/commit/ea15f07ce79366a08fee5b60\n" +
        "-> 1.8357153\n" +
        "\n" +
        "cn/res/chinapostplan/structure/181041269\n" +
        "-> 3.494024\n" +
        "\n" +
        "com/istio/proxy/blob/bcdc1684df0839a6125\n" +
        "-> 1.5356048\n" +
        "\n" +
        "com/kriskowal/q/blob/b0fa72980717dc202ff\n" +
        "-> 1.3069352\n" +
        "\n" +
        "com/ph/logstash/de2ba3f964ae7039b7b74a4a\n" +
        "-> 1.4612998\n" +
        "\n" +
        "default/src/test/java/org/xwiki/componen\n" +
        "-> 2.6909549\n" +
        "\n" +
        "search_my_organization-example.json\n" +
        "-> 3.6890879\n" +
        "\n" +
        "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH\n" +
        "-> 3.5768316\n" +
        "\n" +
        "org.apache.catalina.startup.EXIT_ON_INIT_FAILURE\n" +
        "-> 4.2315959\n" +
        "\n" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\n" +
        "-> 1.2038558\n" +
        "\n" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_\n" +
        "-> 1.2038558\n" +
        "\n" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567\n" +
        "-> 1.2252754\n" +
        "\n" +
        "0123456789ABCDEFGHIJKLMNOPQRSTUV\n" +
        "-> 1.2310129\n" +
        "\n" +
        "abcdefghijklmnopqrstuvwxyz\n" +
        "-> 1.1127479\n" +
        "\n" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
        "-> 1.1127479\n" +
        "\n" +
        "org.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT\n" +
        "-> 3.2985092\n" +
        "\n" +
        "org.apache.tomcat.websocket.WS_AUTHENTICATION_PASSWORD\n" +
        "-> 4.061177\n" +
        "\n");

  }

  private String formatHumanLanguageScore(String... texts) {
    StringBuilder sb = new StringBuilder();
    for (String text : texts) {
      sb.append(text)
        .append("\n-> ")
        .append(decimalFormat.format(LatinAlphabetLanguagesHelper.humanLanguageScore(text)))
        .append("\n\n");
    }
    return sb.toString();
  }

}
