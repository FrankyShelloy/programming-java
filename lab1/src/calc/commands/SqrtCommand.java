package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;

public class SqrtCommand implements Command {
    @Override
    public void execute(ExecutionContext context, String[] args) throws ExecutionException {
        if (context.getStack().isEmpty()) {
            throw new ExecutionException("SQRT: Стек пуст");
        }
        double value = context.getStack().pop();
        if (value < 0) {
            context.getStack().push(value);
            throw new ExecutionException("SQRT: Корень из отрицательного числа");
        }
        context.getStack().push(Math.sqrt(value));
    }
}
