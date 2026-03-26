package calc;

import calc.commands.Command;
import calc.exceptions.CalcException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Calculator {
    private static final Logger logger = Logger.getLogger(Calculator.class.getName());

    public void run(InputStream inputStream) {
        ExecutionContext context = new ExecutionContext();
        CommandFactory factory;
        try {
            factory = new CommandFactory();
        } catch (CalcException e) {
            logger.log(Level.SEVERE, "Ошибка инициализации CommandFactory", e);
            return;
        }

        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                String commandName = parts[0];
                String[] args = new String[parts.length - 1];
                System.arraycopy(parts, 1, args, 0, parts.length - 1);

                try {
                    Command command = factory.createCommand(commandName);
                    command.execute(context, args);
                } catch (CalcException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        Calculator calculator = new Calculator();
        if (args.length > 0) {
            try (InputStream input = new FileInputStream(args[0])) {
                calculator.run(input);
            } catch (Exception e) {
                logger.severe("Не удалось прочитать файл: " + args[0]);
            }
        } else {
            calculator.run(System.in);
        }
    }
}
