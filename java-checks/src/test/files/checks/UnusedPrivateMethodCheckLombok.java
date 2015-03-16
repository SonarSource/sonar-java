import lombok.Getter;
import lombok.Setter;

public class UnusedPrivateMethodCheckLombok {

    private int unused;

    @Getter
    private int getter;

    @Setter
    private int setter;

    @Getter
    @Setter
    private int geterSetter;

    @lombok.Getter
    private int getter2;

    @lombok.Setter
    private int setter2;

    @lombok.Getter
    @lombok.Setter
    private int getterSetter2;
}
