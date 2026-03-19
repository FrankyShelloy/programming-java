package calc;

import calc.commands.Command;
import calc.exceptions.CalcException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommandFactory {
    private final Map<String, Class<?>> commands = new HashMap<>();

    public CommandFactory() throws CalcException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("factory.properties")) {
            if (in == null) {
                throw new CalcException("Файл конфигурации 'factory.properties' не найден");
            }
            properties.load(in);
            for (String commandName : properties.stringPropertyNames()) {
                String className = properties.getProperty(commandName);
                commands.put(commandName, Class.forName(className));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new CalcException("Ошибка загрузки фабрики команд: " + e.getMessage());
        }
    }

    public Command createCommand(String name) throws CalcException {
        Class<?> commandClass = commands.get(name);
        if (commandClass == null) {
            throw new CalcException("Неизвестная команда: " + name);
        }
        try {
            return (Command) commandClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new CalcException("Ошибка создания команды '" + name + "': " + e.getMessage());
        }
    }
}
