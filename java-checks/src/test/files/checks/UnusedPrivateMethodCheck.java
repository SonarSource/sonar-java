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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02
*/
package org.sonar.java.checks.targets;

import javafx.fxml.FXML;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PostPersist;
import javax.persistence.PreUpdate;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import javax.persistence.PostRemove;
import javax.ejb.Remove;

public class UnusedPrivateMethod {

    public UnusedPrivateMethod(String s) {
        init();
    }

    private void init() {
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
// this method should not be considered as dead code, see Serializable contract
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
// this method should not be considered as dead code, see Serializable contract
    }

    private Object writeReplace() throws java.io.ObjectStreamException {
// this method should not be considered as dead code, see Serializable contract
        return null;
    }

    private Object readResolve() throws java.io.ObjectStreamException {
// this method should not be considered as dead code, see Serializable contract
        return null;
    }

    private void readObjectNoData() throws java.io.ObjectStreamException {
// this method should not be considered as dead code, see Serializable contract
    }

    // Noncompliant@+2 {{Private method 'unusedPrivateMethod' is never used.}}
    @SuppressWarnings("unused")
    private int unusedPrivateMethod() {
        return 1;
    }

    // Noncompliant@+1 {{Private method 'unusedPrivateMethod' is never used.}}
    private int unusedPrivateMethod(int a, String s) {
        return 1;
    }

    public enum Attribute {
        ID("plop", "foo", true);

        Attribute(String prettyName, String type, boolean hidden) {
        }
        // Noncompliant@+1 {{Private constructor 'Attribute(String,String[][],int)' is never used.}}
        Attribute(String prettyName, String[][] martrix, int i) {
        }
    }

    private class A {
        // Noncompliant@+1 {{Private constructor 'A()' is never used.}}
        private A() {
        }

        private <T> T foo(T t) {
            return null;
        }

        public void bar() {
            foo("");
        }
    }

    @PostConstruct
    private void unusedPrivateMethodPostConstruct() { }

    @PreDestroy
    private void unusedPrivateMethodPreDestroy() { }

    @Produces
    private void unusedPrivateMethodProduces() { }

    @PostLoad
    private void unusedPrivateMethodPostLoad() { }

    @PrePersist
    private void unusedPrivateMethodPrePersist() { }

    @PrePersist
    private void unusedPrivateMethodPrePersist() { }

    @PostPersist
    private void unusedPrivateMethodPostPersist() { }

    @PreUpdate
    private void unusedPrivateMethodPreUpdate() { }

    @PostUpdate
    private void unusedPrivateMethodPostUpdate() { }

    @PreRemove
    private void unusedPrivateMethodPreRemove() { }

    @PostRemove
    private void unusedPrivateMethodPostRemove() { }

    @Remove
    private void unusedPrivateMethodRemove() { }

    @FXML
    private void unusedPrivateMethodFXML() { }

}
