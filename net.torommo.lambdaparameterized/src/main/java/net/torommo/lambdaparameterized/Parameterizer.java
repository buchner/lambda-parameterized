/*
 * Copyright 2015 Bjoern Buchner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.torommo.lambdaparameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public class Parameterizer {
    
    private final Complete<?> complete;

    private Parameterizer(Complete<?> complete) {
        this.complete = complete;
    }    
    
    public static <U> NeedsSourceAndTarget<U> of(Class<U> clazz) {
        return new NeedsSourceAndTarget<>(clazz);
    }        
    
    public List<? extends ParameterizedCall<?>> generateCalls() {
        return complete.generateCalls();
    }
    
    public abstract static class ParameterizedCall<T> {
        
        private final Class<T> targetType;
                     
        public ParameterizedCall(Class<T> targetType) {
            super();
            this.targetType = targetType;
        }

        public final Class<T> getTargetType() {
            return targetType;
        }
        
        public abstract List<Object> getParameters();
        
        public abstract void execute(Object instance);
    }
               
    private static class ParameterizedCall2<T> extends ParameterizedCall<T> {
        
        private final Object parameter;
        private final BiConsumer<T, Object> call;
        
        public ParameterizedCall2(Class<T> targetType, Object parameter, BiConsumer<T, Object> call) {
            super(targetType);
            this.parameter = parameter;
            this.call = call;
        }
        
        @Override
        public List<Object> getParameters() {
            return ImmutableList.of(parameter);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void execute(Object instance) {
            call.accept((T)instance, parameter);
        }
    }
    
    private static class ParameterizedCall3<T> extends ParameterizedCall<T> {
        
        private final Tuple<? extends Object, ? extends Object> parameters;
        private final Consumer3<T, Object, Object> call;
        
        public ParameterizedCall3(Class<T> targetType, Tuple<? extends Object, ? extends Object> parameters,
                Consumer3<T, Object, Object> call) {
            super(targetType);
            this.parameters = parameters;
            this.call = call;
        }        
        
        @Override
        public List<Object> getParameters() {
            return ImmutableList.of(parameters.getFirst(), parameters.getSecond());
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void execute(Object instance) {
            call.accept((T)instance, parameters.getFirst(), parameters.getSecond());
        }
    }    
    
    public static class NeedsSourceAndTarget<T> {            
        
        private final Class<T> clazz;
                                
        public NeedsSourceAndTarget(Class<T> clazz) {
            super();
            this.clazz = clazz;
        }

        public <U> NeedsSource2<T, U> run(BiConsumer<T, U> target) {
            return new NeedsSource2<T, U>(clazz, target);
        }
        
        public <U, V> NeedsSource3<T, U, V> run(Consumer3<T, U, V> target) {
            return new NeedsSource3<T, U, V>(clazz, target);
        }
    }
    
    public static class NeedsSource2<T, U> {
        
        private final Class<T> clazz;
        private final BiConsumer<T, U> target;
        
        public NeedsSource2(Class<T> clazz, BiConsumer<T, U> target) {
            this.clazz = clazz;
            this.target = target;
        }
        
        public Parameterizer with(Supplier<Iterable<U>> source) {
            return new Parameterizer(new Parameterizer.Complete2<T, U>(clazz, source, target));
        }
    }
    
    public static class NeedsSource3<T, U, V> {
        
        private final Class<T> clazz;
        private final Consumer3<T, U, V> target;
        
        public NeedsSource3(Class<T> clazz, Consumer3<T, U, V> target) {
            this.clazz = clazz;
            this.target = target;
        }
        
        public Parameterizer with(Supplier<Iterable<? extends Tuple<U, V>>> source) {
            return new Parameterizer(new Parameterizer.Complete3<T, U, V>(clazz, source, target));
        }
    }
    
    public static interface Tuple<U, V> {
        
        U getFirst();
        V getSecond();
    }
    
    public static abstract class Complete<T> {
        
        private final Class<T> clazz;
                       
        public Complete(Class<T> clazz) {
            super();
            this.clazz = clazz;
        }

        public Class<T> getTargetClass() {
            return clazz;
        }
        
        public abstract List<? extends ParameterizedCall<?>> generateCalls();
    }
    
    private static class Complete2<T, U> extends Complete<T> {
        
        private final Supplier<Iterable<U>> source;
        private final BiConsumer<T, U> target;
        
        public Complete2(Class<T> clazz, Supplier<Iterable<U>> source,
                BiConsumer<T, U> target) {
            super(clazz);
            this.source = source;
            this.target = target;
        }   
        
        @SuppressWarnings("unchecked") // Even though the compiler is not, we can be sure that the types are correct.        
        @Override
        public List<ParameterizedCall2<T>> generateCalls() {
            return ImmutableList.copyOf(source.get()).stream()
                    .map(object -> new ParameterizedCall2<T>(getTargetClass(), object, (BiConsumer<T, Object>)target))
                        .collect(Collectors.toList());
        }
    }
    
    private static class Complete3<T, U, V> extends Complete<T> {
        
        private final Supplier<Iterable<? extends Tuple<U, V>>> source;
        private final Consumer3<T, U, V> target;
        
        public Complete3(Class<T> clazz, Supplier<Iterable<? extends Tuple<U, V>>> source,
                Consumer3<T, U, V> target) {
            super(clazz);
            this.source = source;
            this.target = target;
        }   
        
        @SuppressWarnings("unchecked") // Even though the compiler is not, we can be sure that the types are correct.
        @Override
        public List<Parameterizer.ParameterizedCall3<T>> generateCalls() {            
            return ImmutableList.copyOf(source.get()).stream()
                    .map(object -> new ParameterizedCall3<T>(getTargetClass(), object, (Consumer3<T, Object, Object>)target))
                        .collect(Collectors.toList());
        }
    }
    
    @FunctionalInterface
    public static interface Consumer3<T, U, V> {
        
        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @param v the third input argument
         */
        void accept(T t, U u, V v);
    }
    
    public static class TestConfiguration {
        
        public static <U, V> WithTwoParameters<U, V> with(U firstParameter, V secondParameter) {
            return new WithTwoParameters<U, V>(firstParameter, secondParameter);
        }
        
        public static class WithTwoParameters<U, V> {
            
            private final List<Tuple<U, V>> tuples;

            public WithTwoParameters(U firstParameter, V secondParameter) {
                tuples = new ArrayList<>();
                tuples.add(new DefaultTuple<U, V>(firstParameter, secondParameter));
            }
            
            public WithTwoParameters<U, V> and(U firstParameter, V secondParameter) {
                tuples.add(new DefaultTuple<U, V>(firstParameter, secondParameter));            
                return this;
            }
            
            public List<Tuple<U, V>> build() {
                return ImmutableList.copyOf(tuples);
            }
        }
        
        private static class DefaultTuple<U, V> implements Tuple<U, V> {

            private final U first;
            private final V second;
            
            public DefaultTuple(U first, V second) {
                super();
                this.first = first;
                this.second = second;
            }

            @Override
            public U getFirst() {
                return first;
            }

            @Override
            public V getSecond() {
                return second;
            }            
        }
     }
}