package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;

public class DivCommand implements Command {
    @Override
    public void execute(ExecutionContext context, String[] args) throws ExecutionException {
        if (context.getStack().size() < 2) {
            throw new ExecutionException("/: В стеке менее 2 элементов");
        }
        double a = context.getStack().pop();
        if (a == 0) {
            context.getStack().push(a); 
            throw new ExecutionException("Деление на ноль");
        }
        double b = context.getStack().pop();
        context.getStack().push(b / a);
    }
}
