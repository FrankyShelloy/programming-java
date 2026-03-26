package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.*;
import java.util.logging.Logger;

public class SqrtCommand implements Command {
    private static final Logger logger = Logger.getLogger(SqrtCommand.class.getName());

    @Override
    public void execute(ExecutionContext context, String[] args) throws CalcException {
        if (context.getStack().isEmpty()) {
            String msg = "SQRT: Стек пуст";
            logger.warning(msg);
            throw new StackSizeException(msg);
        }
        double value = context.getStack().pop();
        if (value < 0) {
            context.getStack().push(value);
            String msg = "SQRT: Корень из отрицательного числа (" + value + ")";
            logger.severe(msg);
            throw new MathCommandException(msg);
        }
        double result = Math.sqrt(value);
        context.getStack().push(result);
        logger.info("SQRT: Результат " + result + " помещен в стек (изъято " + value + ")");
    }
}
