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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1313")
public class HardcodedIpCheck extends BaseTreeVisitor implements JavaFileScanner {
  private static final String PROTOCOL_URL = "([^\\d.]*/)";
  private static final String PORT_URL = "(:\\d{1,5})?";
  private static final String PATH_URL = "((?![\\d.]))(/.*)?";
  private static final String END_URL = PORT_URL + PATH_URL;

  public static final String IP_V4_ALONE = "(?<ipv4>(?:\\d{1,3}\\.){3}\\d{1,3})";
  private static final String IP_V4_MAPPED_IP_V6 = "(::ffff:0:|::ffff:|0:0:0:0:0:ffff:0:|0:0:0:0:0:0:ffff:)" + IP_V4_ALONE;
  private static final String IP_V4_URL = (PROTOCOL_URL + "?") + IP_V4_ALONE + END_URL;

  private static final List<Pattern> IP_V4_REGEX_LIST = Arrays.asList(
    Pattern.compile(IP_V4_ALONE),
    Pattern.compile(IP_V4_URL),
    Pattern.compile(IP_V4_MAPPED_IP_V6));



  private static final String IP_V6_WITH_FIRST_PART = "(\\p{XDigit}{1,4}::?){1,7}\\p{XDigit}{0,4}";
  private static final String IP_V6_WITHOUT_FIRST_PART = "::((\\p{XDigit}{1,4}:){0,6}\\p{XDigit}{1,4})?";
  public static final String IP_V6_ALONE = ("(?<ip>" + IP_V6_WITH_FIRST_PART + "|" + IP_V6_WITHOUT_FIRST_PART + ")");
  private static final String IP_V6_BRACKET = "\\[" + IP_V6_ALONE + "\\]";
  private static final String IP_V6_URL = PROTOCOL_URL + IP_V6_BRACKET + END_URL;

  private static final List<Pattern> IP_V6_REGEX_LIST = Arrays.asList(
    Pattern.compile(IP_V6_ALONE),
    Pattern.compile(IP_V6_BRACKET),
    Pattern.compile(IP_V6_URL));

  private static final Pattern IP_V6_LOOPBACK = Pattern.compile("[0:]++0*+1");
  private static final Pattern IP_V6_NON_ROUTABLE = Pattern.compile("[0:]++");

  private static final String MESSAGE = "Make sure using this hardcoded IP address is safe here.";

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      String value = LiteralUtils.trimQuotes(tree.value());
      extractIPV4(value).map(Optional::of).orElseGet(() -> extractIPV6(value))
        .filter(ip -> !isLoopbackAddress(ip) && !isNonRoutableAddress(ip) && !isBroadcastAddress(ip) && !isDocumentationRange(ip))
        .ifPresent(ip -> context.reportIssue(this, tree, MESSAGE));
    }
  }

  private static boolean isDocumentationRange(String ip) {
    return ip.startsWith("192.0.2.") || ip.startsWith("198.51.100.") || ip.startsWith("203.0.113.") || ip.startsWith("2001:db8:");
  }

  private static boolean isLoopbackAddress(String ip) {
    return ip.startsWith("127.") || IP_V6_LOOPBACK.matcher(ip).matches();
  }

  private static boolean isNonRoutableAddress(String ip) {
    return "0.0.0.0".equals(ip) || IP_V6_NON_ROUTABLE.matcher(ip).matches();
  }

  private static boolean isBroadcastAddress(String ip) {
    return "255.255.255.255".equals(ip);
  }

  private static Optional<String> extractIPV4(String value) {
    return IP_V4_REGEX_LIST.stream()
      .map(pattern -> pattern.matcher(value))
      .filter(Matcher::matches)
      .map(match -> match.group("ipv4"))
      .filter(HardcodedIpCheck::isValidIPV4Parts)
      .filter(ip -> !looksLikeAsn1ObjectIdentifier(ip))
      .findFirst();
  }

  private static boolean looksLikeAsn1ObjectIdentifier(String ip) {
    return ip.startsWith("2.5.");
  }

  private static boolean isValidIPV4Parts(String ip) {
    return Arrays.stream(ip.split("\\."))
      .mapToInt(Integer::parseInt)
      .allMatch(i -> i < 255);
  }

  private static Optional<String> extractIPV6(String value) {
    return IP_V6_REGEX_LIST.stream()
      .map(pattern -> pattern.matcher(value))
      .filter(Matcher::matches)
      .map(match -> match.group("ip"))
      .filter(HardcodedIpCheck::isValidIPV6PartCount)
      .findFirst();
  }

  private static boolean isValidIPV6PartCount(String ip) {
    int partCount = ip.split("::?").length;
    int compressionSeparatorCount = StringUtils.countMatches(ip, "::");
    boolean validUncompressed = compressionSeparatorCount == 0 && partCount == 8;
    boolean validCompressed = compressionSeparatorCount == 1 && partCount <= 7;
    return validUncompressed || validCompressed;
  }

}
