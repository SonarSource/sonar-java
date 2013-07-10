/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CheckstyleAuditListenerTest {
  @Test
  public void testUtilityMethods() {
    AuditEvent event;

    event = new AuditEvent(this, "", new LocalizedMessage(0, "", "", null, "", CheckstyleAuditListenerTest.class, "msg"));
    assertThat(CheckstyleAuditListener.getLineId(event)).isNull();
    assertThat(CheckstyleAuditListener.getMessage(event)).isEqualTo("msg");
    assertThat(CheckstyleAuditListener.getRuleKey(event)).isEqualTo(CheckstyleAuditListenerTest.class.getName());

    event = new AuditEvent(this, "", new LocalizedMessage(1, "", "", null, "", CheckstyleAuditListenerTest.class, "msg"));
    assertThat(CheckstyleAuditListener.getLineId(event)).isEqualTo(1);
    assertThat(CheckstyleAuditListener.getMessage(event)).isEqualTo("msg");
    assertThat(CheckstyleAuditListener.getRuleKey(event)).isEqualTo(CheckstyleAuditListenerTest.class.getName());

    event = new AuditEvent(this);
    assertThat(CheckstyleAuditListener.getLineId(event)).isNull();
    assertThat(CheckstyleAuditListener.getMessage(event)).isNull();
    assertThat(CheckstyleAuditListener.getRuleKey(event)).isNull();
  }

}
