class A0 {
  /**
   * Starts at B5
   *   B5
   *   0:  INT_LITERAL                           0
   *   1:  VARIABLE                              i
   *     jumps to: B4
   *
   *   B4
   *   0:  IDENTIFIER                            words
   *     jumps to: B2
   *
   *   B3
   *   0:  IDENTIFIER                            i
   *   1:  POSTFIX_INCREMENT                     i++
   *     jumps to: B2
   *
   *   B2
   *   0:  VARIABLE                              word
   *   T:  FOR_EACH_STATEMENT                    for {word : words}
   *     jumps to: B1(false) B3(true)
   *
   *   B1
   *   0:  IDENTIFIER                            i
   *   T:  RETURN_STATEMENT                      return i
   *     jumps to: B0(exit)
   *
   *   B0 (Exit):
   */
  int count(java.util.Collection<String> words) {
    int i = 0;
    for (String word : words) {
      i++;
    }
    return i;
  }
}
