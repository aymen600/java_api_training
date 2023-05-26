package fr.lernejo;

import org.junit.Test;
import org.junit.jupiter.api.*;

import static fr.lernejo.Sample.Operation.ADD;
import static fr.lernejo.Sample.Operation.MULT;
import static org.assertj.core.api.Assertions.*;

public class SampleTest {

    private Sample sample;

    @BeforeEach
    public void setUp() {
        sample = new Sample();
    }

    @Test
    public void testAddition() {
        int result = sample.op(ADD,2, 3);
        assertThat(result).isEqualTo(5);
    }


    @Test
    public void testMultiplication() {
        int result = sample.op(MULT,2, 3);
        assertThat(result).isEqualTo(6);
    }


    @Test
    public void testLowerBound() {
        int result = sample.fact(0);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testFactorial() {
        int result = sample.fact(5);
        assertThat(result).isEqualTo(120);
    }

    @Test
    public void testUpperBound() {
        assertThatThrownBy(() -> sample.fact(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("N should be positive");
    }
}
