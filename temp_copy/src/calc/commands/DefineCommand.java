package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.*;
import java.util.logging.Logger;

public class DefineCommand implements Command {
    private static final Logger logger = Logger.getLogger(DefineCommand.class.getName());

    @Override
    public void execute(ExecutionContext context, String[] args) throws CalcException {
        if (args.length < 2) {
            String msg = "DEFINE: Недостаточно аргументов";
            logger.severe(msg);
            throw new InvalidCommandArgumentsException(msg);
        }
        try {
            double value = Double.parseDouble(args[1]);
            context.getConstants().put(args[0], value);
            logger.info("DEFINE: Параметр " + args[0] + " задан как " + value);
        } catch (NumberFormatException e) {
            String msg = "DEFINE: Неверное значение константы: " + args[1];
            logger.warning(msg);
            throw new InvalidCommandArgumentsException(msg);
        }
    }
}
