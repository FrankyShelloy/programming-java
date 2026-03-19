package calc.commands;

import calc.ExecutionContext;
import calc.exceptions.ExecutionException;

public interface Command {
    void execute(ExecutionContext context, String[] args) throws ExecutionException;
}
