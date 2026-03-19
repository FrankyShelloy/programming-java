package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;

public class PushCommand implements Command {
    @Override
    public void execute(ExecutionContext context, String[] args) throws ExecutionException {
        if (args.length < 1) {
            throw new ExecutionException("PUSH: Недостаточно аргументов");
        }
        String arg = args[0];
        try {
            double value = Double.parseDouble(arg);
            context.getStack().push(value);
        } catch (NumberFormatException e) {
            if (context.getConstants().containsKey(arg)) {
                context.getStack().push(context.getConstants().get(arg));
            } else {
                throw new ExecutionException("PUSH: Неопределенный параметр: " + arg);
            }
        }
    }
}
