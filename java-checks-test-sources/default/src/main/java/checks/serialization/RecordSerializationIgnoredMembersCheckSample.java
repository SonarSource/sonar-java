package checks.serialization;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;

public class RecordSerializationIgnoredMembersCheckSample {

  record NoncompliantRecord() implements Serializable, Externalizable {
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0]; // Noncompliant [[sc=46;ec=68]] {{Remove this field that will be ignored during record serialization.}}

    private void writeObject(ObjectOutputStream out) throws IOException { } // Noncompliant [[sc=18;ec=29]] {{Remove this method that will be ignored during record serialization.}}
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException { } // Noncompliant
    private void readObjectNoData() throws ObjectStreamException { } // Noncompliant
    @Override public void writeExternal(ObjectOutput out) throws IOException { } // Noncompliant
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { } // Noncompliant
  }

  record CompliantRecord1() implements Serializable {
    private static final ObjectStreamField serialPersistentFields = null; // not an array
    private static final int CONST = 42;

    CompliantRecord1 { } // not a field, not a method
    ; // not a field, not a method
    void foo() { }
    private Object writeReplace() throws ObjectStreamException { return new CompliantRecord1(); }
    private Object readResolve() throws ObjectStreamException { return new CompliantRecord1(); }
    private void writeExternal(ObjectOutput out) { } // not an override, private
    public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException { } // should be public
  }

  record CompliantRecord2() implements Serializable {
    public static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0]; // should be private
  }

  record CompliantRecord3() implements Serializable {
    private static ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0]; // should be final
  }

  record simpleRecord(int foo) { }
}
