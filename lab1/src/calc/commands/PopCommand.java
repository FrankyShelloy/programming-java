package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;
import java.util.EmptyStackException;

public class PopCommand implements Command {
    @Override
    public void execute(ExecutionContext context, String[] args) throws ExecutionException {
        try {
            context.getStack().pop();
        } catch (EmptyStackException e) {
            throw new ExecutionException("POP: Стек пуст");
        }
    }
}
