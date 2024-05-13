package org.sonar.java.checks;

import java.io.IOException;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.chrono.HijrahDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalLong;

public class SerializableBaseClass implements Serializable {
}

public class SerializableClass extends SerializableBaseClass {

  static HijrahDate staticAttr0;
  
  String attr0;
  
  NonSerializableClass attr1;

  Optional<String> attr2a;         // Compliant, as Optional is not serializable (another rule accounts for non serializable fields)
  
  OptionalLong attr2b;             // Compliant, as Optional is not serializable (another rule accounts for non serializable fields)

  HijrahDate attr3; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//           ^^^^^

  Clock attr4;                     // Compliant, as Clock is not a value-based class

  LocalDateTime attr5; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//              ^^^^^

  transient Optional<String> attr6;

  transient Clock attr7;
  
  transient HijrahDate attr8; 
  
  HijrahDate[][] attr9; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//               ^^^^^
  
  List<LocalDateTime> attr10; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//                    ^^^^^^

  List<LocalDateTime[]> attr11; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//                      ^^^^^^
  
  Map<String, LocalDateTime> attr12; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//                           ^^^^^^
  
  List<String> attr13;
  
  Class<LocalDateTime> attr14;
  
  public LocalDateTime meth() {
    return null;
  }

  public void doSomething() {
    LocalDateTime var = null;

    Serializable obj = new Serializable() {
      
      private String attr1;
      
      NonSerializableClass attr2;
      
      Optional<String> attr3;    // Compliant, as Optional is not serializable 
      
      LocalDateTime attr4; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//                  ^^^^^
    };
  }

}

class NonSerializableClass {

  String attr1;

  Optional<String> attr2;   // Compliant, as this class is not serializable

  LocalDateTime attr3;      // Compliant, as this class is not serializable

}

class SerializableWithSpecialMethods implements Serializable {
  
  HijrahDate attr1;         // Compliant, as the class implements the serialiazion methods
  
  private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
  }

  private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
  }

}

enum MyEnum implements Serializable {
  
  VAL1, VAL2;
  
  String attr1;
  
  HijrahDate attr2; // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//           ^^^^^
  
}

public @interface MyAnnotation {

  String CONSTANT = "";

  String elem1();

  HijrahDate elem2(); // Noncompliant {{Make this value-based field transient so it is not included in the serialization of this class.}}
//           ^^^^^

}
