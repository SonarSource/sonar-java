class ArrayTypes {
  String[] strings1;
  String[][] strings2 = {{"foo", "bar"}, {"qix"}};
  String[][][] strings3 = {{{}}};
  Object[][][] objects;

  String[][] foo() {
    return new String[][] {{}, {}};
  }
}
