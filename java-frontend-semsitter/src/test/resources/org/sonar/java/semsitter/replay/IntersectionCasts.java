import java.io.Serializable;

class IntersectionCasts {
  Serializable keep(Comparable<String> comparable) {
    Serializable serializable = (Comparable<String> & Serializable) comparable;
    return serializable;
  }

  Object redundant(String text) {
    Object widened = (Comparable<String> & Serializable) text;
    return widened;
  }
}
