package calc;

import calc.commands.*;
import calc.exceptions.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {
    private ExecutionContext context;

    @BeforeEach
    public void setup() {
        context = new ExecutionContext();
    }

    @Test
    public void testPushPop() throws ExecutionException {
        new PushCommand().execute(context, new String[]{"10.0"});
        assertEquals(10.0, context.getStack().peek());
        new PopCommand().execute(context, new String[]{});
        assertTrue(context.getStack().isEmpty());
    }

    @Test
    public void testDefineAndPush() throws ExecutionException {
        new DefineCommand().execute(context, new String[]{"var", "42.0"});
        new PushCommand().execute(context, new String[]{"var"});
        assertEquals(42.0, context.getStack().peek());
    }

    @Test
    public void testArithmetic() throws ExecutionException {
        new PushCommand().execute(context, new String[]{"10"});
        new PushCommand().execute(context, new String[]{"2"});
        new AddCommand().execute(context, new String[]{});
        assertEquals(12.0, context.getStack().peek());

        new PushCommand().execute(context, new String[]{"4"});
        new SubCommand().execute(context, new String[]{});
        assertEquals(8.0, context.getStack().peek());

        new PushCommand().execute(context, new String[]{"3"});
        new MulCommand().execute(context, new String[]{});
        assertEquals(24.0, context.getStack().peek());


        new PushCommand().execute(context, new String[]{"6"});
        new DivCommand().execute(context, new String[]{});
        assertEquals(4.0, context.getStack().peek());
    }

    @Test
    public void testSqrt() throws ExecutionException {
        new PushCommand().execute(context, new String[]{"16.0"});
        new SqrtCommand().execute(context, new String[]{});
        assertEquals(4.0, context.getStack().peek());
    }

    @Test
    public void testEmptyStackPop() {
        assertThrows(ExecutionException.class, () -> 
            new PopCommand().execute(context, new String[]{})
        );
    }

    @Test
    public void testEmptyStackArithmetic() {
        assertThrows(ExecutionException.class, () -> 
            new AddCommand().execute(context, new String[]{})
        );
    }

    @Test
    public void testDivByZero() {
        assertThrows(ExecutionException.class, () -> {
            new PushCommand().execute(context, new String[]{"10"});
            new PushCommand().execute(context, new String[]{"0"});
            new DivCommand().execute(context, new String[]{});
        });
    }

    @Test
    public void testSqrtNegative() {
        assertThrows(ExecutionException.class, () -> {
            new PushCommand().execute(context, new String[]{"-1"});
            new SqrtCommand().execute(context, new String[]{});
        });
    }

    @Test
    public void testUndefinedParameter() {
        assertThrows(ExecutionException.class, () -> 
            new PushCommand().execute(context, new String[]{"unknown_var бэд бой"})
        );
    }
}
