package net.torommo.lambdaparameterized;

import java.util.ArrayList;
import java.util.List;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.InitializationError;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;

import net.torommo.lambdaparameterized.Parameterizer.ParameterizedCall;

public class LambdaParameterizedRunner extends ParentRunner<Runner> {
       
    public LambdaParameterizedRunner(Class<?> testClass)
            throws InitializationError {
        super(testClass);
    }

    @Override
    protected List<Runner> getChildren() {
        List<Runner> result = new ArrayList<>();
        for (FrameworkField field : getTestClass().getAnnotatedFields(ParameterizedTest.class)) {
            try {
                Parameterizer parameterizer = (Parameterizer) field.get(null);
                for (ParameterizedCall<?> call : parameterizer.generateCalls()) {
                    result.add(new ParameterizeRunner(getTestClass().getName(), field.getName(), call));
                }
            } catch (IllegalAccessException e) {
                Throwables.propagate(e);
            }
        }
        return result;
    }
    
    @Override
    protected Description describeChild(Runner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(Runner child, RunNotifier notifier) {
        child.run(notifier);
    }
    
    private static class ParameterizeRunner extends Runner {
        
        private final String className;
        private final String fieldName;
        private final ParameterizedCall<?> call;

        public ParameterizeRunner(String className, String fieldName, ParameterizedCall<?> call) {
            super();
            this.className = className;
            this.fieldName = fieldName;
            this.call = call;
        }

        @Override
        public Description getDescription() {   
            return Description.createTestDescription(className, fieldName + 
                    ": (" + Joiner.on(',').join(call.getParameters()) + ")");
        }

        @Override
        public void run(RunNotifier notifier) {            
            EachTestNotifier eachNotifier = new EachTestNotifier(notifier, getDescription());
            eachNotifier.fireTestStarted();
            try {
                call.execute(call.getTargetType().newInstance());
            } catch (AssumptionViolatedException e) {
                eachNotifier.addFailedAssumption(e);
            } catch (Throwable e) {
                eachNotifier.addFailure(e);
            } finally {
                eachNotifier.fireTestFinished();
            }
        }        
    }
}
