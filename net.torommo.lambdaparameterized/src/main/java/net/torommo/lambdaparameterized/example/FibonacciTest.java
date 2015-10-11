package net.torommo.lambdaparameterized.example;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.runner.RunWith;

import net.torommo.lambdaparameterized.LambdaParameterizedRunner;
import net.torommo.lambdaparameterized.ParameterizedTest;
import net.torommo.lambdaparameterized.Parameterizer;
import net.torommo.lambdaparameterized.Parameterizer.TestConfiguration;
import net.torommo.lambdaparameterized.Parameterizer.Tuple;

@RunWith(LambdaParameterizedRunner.class)
public class FibonacciTest {
    
    @ParameterizedTest
    public final static Parameterizer compute = Parameterizer.of(FibonacciTest.class)
        .run(FibonacciTest::test).with(FibonacciTest::data);           
    
    public static List<Tuple<Integer, Integer>> data() {
        return TestConfiguration.with(0, 0).and(1, 1).and(2, 1).and(3, 2).and(4, 3)
                .and(5, 5).and(6, 8).build();
    }
    
    public void test(Integer input, Integer expected) {
        assertThat(Fibonacci.compute(input), is(equalTo(expected)));
    }    
}
