package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.*;
import java.util.logging.Logger;

public class MulCommand implements Command {
    private static final Logger logger = Logger.getLogger(MulCommand.class.getName());

    @Override
    public void execute(ExecutionContext context, String[] args) throws CalcException {
        if (context.getStack().size() < 2) {
            String msg = "*: В стеке менее 2 элементов";
            logger.warning(msg);
            throw new StackSizeException(msg);
        }
        double a = context.getStack().pop();
        double b = context.getStack().pop();
        double result = a * b;
        context.getStack().push(result);
        logger.info("*: Результат " + result + " помещен в стек (изъяты " + a + " и " + b + ")");
    }
}
