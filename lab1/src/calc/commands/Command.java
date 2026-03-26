package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.CalcException;

public interface Command {
    void execute(ExecutionContext context, String[] args) throws CalcException;
}
