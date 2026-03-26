package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.*;
import java.util.logging.Logger;

public class PrintCommand implements Command {
    private static final Logger logger = Logger.getLogger(PrintCommand.class.getName());

    @Override
    public void execute(ExecutionContext context, String[] args) throws CalcException {
        if (context.getStack().isEmpty()) {
            String msg = "PRINT: Стек пуст";
            logger.warning(msg);
            throw new StackSizeException(msg);
        }
        double val = context.getStack().peek();
        System.out.println(val);
        logger.info("PRINT: Напечатано значение " + val);
    }
}
