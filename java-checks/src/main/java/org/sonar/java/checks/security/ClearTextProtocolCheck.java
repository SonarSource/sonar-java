/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.HashMap;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S5332")
public class ClearTextProtocolCheck extends AbstractMethodDetection {

  private static final Map<String, Protocol> PROTOCOLS = new HashMap<>();

  private static final String MESSAGE = "Using %s protocol is insecure. Use %s instead.";

  static {
    PROTOCOLS.put("org.apache.commons.net.ftp.FTPClient", new Protocol("FTP", "SFTP, SCP or FTPS"));
    PROTOCOLS.put("org.apache.commons.net.smtp.SMTPClient", new Protocol("clear-text SMTP", "SMTP over SSL/TLS or SMTP with STARTTLS"));
    PROTOCOLS.put("org.apache.commons.net.telnet.TelnetClient", new Protocol("Telnet", "SSH"));
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes(PROTOCOLS.keySet().toArray(new String[0]))
      .constructor()
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Protocol protocol = PROTOCOLS.get(newClassTree.symbolType().fullyQualifiedName());
    String message = String.format(MESSAGE, protocol.protocolName, protocol.alternatives);
    reportIssue(newClassTree.identifier(), message);
  }

  private static class Protocol {
    String protocolName;
    String alternatives;

    Protocol(String protocolName, String alternatives) {
      this.protocolName = protocolName;
      this.alternatives = alternatives;
    }
  }
}
