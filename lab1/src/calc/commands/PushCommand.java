package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.*;
import java.util.logging.Logger;

public class PushCommand implements Command {
    private static final Logger logger = Logger.getLogger(PushCommand.class.getName());

    @Override
    public void execute(ExecutionContext context, String[] args) throws CalcException {
        if (args.length < 1) {
            String msg = "PUSH: Недостаточно аргументов";
            logger.severe(msg);
            throw new InvalidCommandArgumentsException(msg);
        }
        String arg = args[0];
        try {
            double value = Double.parseDouble(arg);
            context.getStack().push(value);
            logger.info("PUSH: Число " + value + " помещено в стек");
        } catch (NumberFormatException e) {
            if (context.getConstants().containsKey(arg)) {
                double val = context.getConstants().get(arg);
                context.getStack().push(val);
                logger.info("PUSH: Параметр " + arg + " (значение " + val + ") помещен в стек");
            } else {
                String msg = "PUSH: Неопределенный параметр: " + arg;
                logger.warning(msg);
                throw new InvalidCommandArgumentsException(msg);
            }
        }
    }
}
