package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;

public class DefineCommand implements Command {
    @Override
    public void execute(ExecutionContext context, String[] args) throws ExecutionException {
        if (args.length < 2) {
            throw new ExecutionException("DEFINE: Недостаточно аргументов");
        }
        try {
            double value = Double.parseDouble(args[1]);
            context.getConstants().put(args[0], value);
        } catch (NumberFormatException e) {
            throw new ExecutionException("DEFINE: Неверное значение константы: " + args[1]);
        }
    }
}
