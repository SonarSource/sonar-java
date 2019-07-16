class A {
    void f() {
        boolean a = true, b = false;
        int d = 0, e = 0;

        if (a)
            if (b)
                d++;
        else // Noncompliant {{Add explicit curly braces to avoid dangling else.}}
            e++;

        if (a || b) {
            if (a)
                if (b)
                    d++;
            else // Noncompliant
                e++;
        }

        if (a)
            d++;

        if (a)
            if (b)
                d++;

        if (a)
            d++;
        else
            e++;

        if (a) {
            d++;
        } else {
            e++;
        }

        if (a) {
            if (b) {
                d++;
            } else {
                e++;
            }
        }

        if (a) {
            if (b)
                d++;
            else
                e++;
        }
    }
}