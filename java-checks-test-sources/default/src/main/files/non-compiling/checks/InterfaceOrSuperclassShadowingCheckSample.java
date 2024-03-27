import java.util.Map;

abstract class Entry implements Map.Entry {} // Noncompliant
class MyMap {
  interface Entry extends Map.Entry {} // Compliant
  interface Unkonwn extends Unknown {} // Compliant
}

interface Object {} // Noncompliant
