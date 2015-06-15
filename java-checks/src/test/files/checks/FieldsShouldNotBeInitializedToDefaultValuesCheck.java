
/**
 * Origin : <a href="https://github.com/checkstyle/checkstyle/blob/master/src/test/resources/com/puppycrawl/tools/checkstyle/coding/InputExplicitInit.java">InputExplicitInit</a>.</br>
 * Input for unit test <a href="https://github.com/checkstyle/checkstyle/blob/master/src/test/java/com/puppycrawl/tools/checkstyle/checks/coding/ExplicitInitializationCheckTest.java">ExplicitInitializationCheck</a>.
 */
public class FieldsShouldNotBeInitializedToDefaultValuesCheck {
    private int intHexa = 0x00; // Noncompliant {{Remove this initialization to "intHexa", the compiler will do that for you.}}
    private long underscore = 0b00000000_00000000_00000000_00000000; // Noncompliant {{Remove this initialization to "underscore", the compiler will do that for you.}}
    private int x = 0; // Noncompliant {{Remove this initialization to "x", the compiler will do that for you.}}
    private Object bar = /* comment test */null; // Noncompliant {{Remove this initialization to "bar", the compiler will do that for you.}}
    private int y = 1;
    private long y1 = 1 - 1;
    private long y3;
    private long y4 = 0L; // Noncompliant {{Remove this initialization to "y4", the compiler will do that for you.}}
    private boolean b1 = false; // Noncompliant {{Remove this initialization to "b1", the compiler will do that for you.}}
    private boolean b2 = true;
    private boolean b3;
    private String str = "";
    java.lang.String str1 = null, str3 = null; // Noncompliant 2
    int ar1[] = null; // Noncompliant {{Remove this initialization to "ar1", the compiler will do that for you.}}
    int ar2[] = new int[1];
    int ar3[];
    float f1 = 0f; // Noncompliant {{Remove this initialization to "f1", the compiler will do that for you.}}
    double d1 = 0.0; // Noncompliant {{Remove this initialization to "d1", the compiler will do that for you.}}

    static char ch;
    static char ch1 = 0; // Noncompliant {{Remove this initialization to "ch1", the compiler will do that for you.}}
    static char ch2 = '\0'; // Noncompliant {{Remove this initialization to "ch2", the compiler will do that for you.}}
    static char ch3 = '\1';

    void method() {
        int xx = 0;
        String s = null;
    }
}

interface interface1{
    int TOKEN_first = 0x00;
    int TOKEN_second = 0x01;
    int TOKEN_third = 0x02;
}

class InputExplicitInit2 {
    private Bar<String> bar = null; // Noncompliant {{Remove this initialization to "bar", the compiler will do that for you.}}
    private Bar<String>[] barArray = null; // Noncompliant {{Remove this initialization to "barArray", the compiler will do that for you.}}
}

enum InputExplicitInit3 {
    A,
    B
    {
        private int x = 0; // Noncompliant {{Remove this initialization to "x", the compiler will do that for you.}}
        private Bar<String> bar = null; // Noncompliant {{Remove this initialization to "bar", the compiler will do that for you.}}
        private Bar<String>[] barArray = null; // Noncompliant {{Remove this initialization to "barArray", the compiler will do that for you.}}
        private int y = 1;
    };
    private int x = 0; // Noncompliant {{Remove this initialization to "x", the compiler will do that for you.}}
    private Bar<String> bar = null; // Noncompliant {{Remove this initialization to "bar", the compiler will do that for you.}}
    private Bar<String>[] barArray = null; // Noncompliant {{Remove this initialization to "barArray", the compiler will do that for you.}}
    private int y = 1;
}

@interface annotation1{
    int TOKEN_first = 0x00;
    int TOKEN_second = 0x01;
    int TOKEN_third = 0x02;
}

class ForEach {
    public ForEach(java.util.Collection<String> strings)
    {
        for(String s : strings) //this should not even be checked
        {

        }
    }
}

class Bar<T> {
}
