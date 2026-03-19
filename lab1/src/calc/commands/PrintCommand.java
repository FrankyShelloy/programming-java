package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;

public class PrintCommand implements Command {
    @Override
    public void execute(ExecutionContext context, String[] args) throws ExecutionException {
        if (context.getStack().isEmpty()) {
            throw new ExecutionException("PRINT: Стек пуст");
        }
        System.out.println(context.getStack().peek());
    }
}
