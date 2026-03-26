package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.*;
import java.util.EmptyStackException;
import java.util.logging.Logger;

public class PopCommand implements Command {
    private static final Logger logger = Logger.getLogger(PopCommand.class.getName());

    @Override
    public void execute(ExecutionContext context, String[] args) throws CalcException {
        try {
            double val = context.getStack().pop();
            logger.info("POP: Элемент " + val + " удален из стека");
        } catch (EmptyStackException e) {
            String msg = "POP: Стек пуст";
            logger.warning(msg);
            throw new StackSizeException(msg);
        }
    }
}
