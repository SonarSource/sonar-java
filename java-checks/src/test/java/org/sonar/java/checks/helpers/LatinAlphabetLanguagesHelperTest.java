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
      .isEqualTo("""
        Hello02139710238712987
        -> 1.3262863
        
        This is an english text!
        -> 4.6352161
        
        Hello
        -> 4.7598878
        
        Hello hello hello hello
        -> 4.7598878
        
        Hleol
        -> 2.0215728
        
        Hleol hleol
        -> 2.0215728
        
        Hleol Hello hleol
        -> 2.9343445
        
        Hleol Incomprehensibility hleol
        -> 3.5209606
        
        Incomprehensibility\s
        -> 4.3978577
        
        slrwaxquavy
        -> 0.783135
        
        SlRwAxQuAvY
        -> 0.5729079
        
        012345678
        -> 1
        
        images/blob/50281d86d6ed5c61975971150adf
        -> 1.1821769
        
        js/commit/8863b9d04c722b278fa93c5d66ad1e
        -> 0.9126614
        
        net/core/builder/e426a9ae7167c5807b173d5
        -> 1.9399531
        
        net/more/builder/3ad489866f41084fa4f3307
        -> 1.9014789
        
        project/commit/c5acf965067478784b54e2d24
        -> 1.2177787
        
        /var/lib/openshift/51122e382d5271c5ca000
        -> 1.3230153
        
        examples/commit/16ad89c4172c259f15bce56e
        -> 1.6869377
        
        examples/commit/8e1d746900f5411e9700fea0
        -> 1.48724
        
        examples/commit/c95b6a84b6fd1efc832a46cd
        -> 1.503256
        
        examples/commit/d6f6ef7457d99e31990fa64b
        -> 1.4204883
        
        examples/commit/ea15f07ce79366a08fee5b60
        -> 1.8357153
        
        cn/res/chinapostplan/structure/181041269
        -> 3.494024
        
        com/istio/proxy/blob/bcdc1684df0839a6125
        -> 1.5356048
        
        com/kriskowal/q/blob/b0fa72980717dc202ff
        -> 1.3069352
        
        com/ph/logstash/de2ba3f964ae7039b7b74a4a
        -> 1.4612998
        
        default/src/test/java/org/xwiki/componen
        -> 2.6909549
        
        search_my_organization-example.json
        -> 3.6890879
        
        org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH
        -> 3.5768316
        
        org.apache.catalina.startup.EXIT_ON_INIT_FAILURE
        -> 4.2315959
        
        ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/
        -> 1.2038558
        
        ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_
        -> 1.2038558
        
        ABCDEFGHIJKLMNOPQRSTUVWXYZ234567
        -> 1.2252754
        
        0123456789ABCDEFGHIJKLMNOPQRSTUV
        -> 1.2310129
        
        abcdefghijklmnopqrstuvwxyz
        -> 1.1127479
        
        ABCDEFGHIJKLMNOPQRSTUVWXYZ
        -> 1.1127479
        
        org.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT
        -> 3.2985092
        
        org.apache.tomcat.websocket.WS_AUTHENTICATION_PASSWORD
        -> 4.061177
        
        """);

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
