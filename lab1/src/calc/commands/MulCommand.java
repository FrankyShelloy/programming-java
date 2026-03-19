package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;

public class MulCommand implements Command {
    @Override
    public void execute(ExecutionContext context, String[] args) throws ExecutionException {
        if (context.getStack().size() < 2) {
            throw new ExecutionException("*: В стеке менее 2 элементов");
        }
        double a = context.getStack().pop();
        double b = context.getStack().pop();
        context.getStack().push(a * b);
    }
}
