class A extends Unkwown {
    int hashcode() { // Compliant as it might override from Unkwown
        return 0;
    }
}
