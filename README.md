Disclaimer: This is an experimental proof of concept which was initially created as a personal playground to learn more about method references in Java 8. In its current state it is neither intended nor suited for productive use.

By default, JUnit paramterized tests are not typesafe. You can not lean on the compiler to check if the types of the generated arguments and the types of the constructor arguments or the types of the injection fields are compatible. This is because there is only an indirect link through annotations between the method that generate the arguments and their targets. Thanks to Java 8 there is a way to create a direct link without writing much boiler plate code - namely method references.

Lets have a look on an example to understand how it works.
```java
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
```
The most obvious difference from JUnit parameterized and normal tests is, that the test method is not annotated with `@Test` and has arguments. Instead, the method test is 'marked' as a test method through the expression `run(FibonacciTest::test)`. In the background the method reference `FibonacciTest::test` is converted into a `Consumer3` instance - that is similar to a `Consumer` but with three arguments instead of one. The first argument will be the instance on which the method will be executed, in this case an instance of `FibonacciTest`. The second on third argument will be passed to the method, in this case input and expected.

Unlike in JUnit parameterized tests, the data method, which generates the test arguments, is not annotated. Similar to the test method, the generation method is 'marked' through the expression `with(FibonacciTest::data)`. The method reference is converted into a `Producer` instance that returns an `Iterable` of `Tuple` instances. The `Tuple` class is used, because the test method has two arguments. If the test method had only one argument we could return an `Iterable` with the values for the argument. The generation method must be static because it must be accessible by the `LambdaParameterizedRunner` without creating an instance of the surrounding class.

The test description is derived from the field name, that is `compute` in the example above. This is different from JUnit parameterized tests where the description is normally derived from the method name. The reason for this difference is, that it is impossible for the `LambdaParameterizedRunner` to find out the name of the referenced method. The method reference is converted into a `Consumer3` instance with a method that calls the referenced method. Through reflection it is not possible to find out the name of the method which is called from inside the `Consumer3` method.
