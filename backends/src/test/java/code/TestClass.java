package code;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class TestClass {

    private int a;
    private String s;

    public TestClass() {

    }

    @Getter
    @Setter
    public class InnerClass {

        private int a;
        private String s;

    }

}
