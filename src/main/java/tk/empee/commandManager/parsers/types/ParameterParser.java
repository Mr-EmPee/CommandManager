package tk.empee.commandManager.parsers.types;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import tk.empee.commandManager.parsers.ParserDescription;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class ParameterParser<T> {

    @Getter protected ParserDescription descriptor = new ParserDescription("", "", null);
    @Getter private final Class<? extends Annotation> identifier;
    @Getter private final String label;
    @Getter private final String defaultValue;

    protected ParameterParser(Class<? extends Annotation> identifier, String label, String defaultValue) {
        this.identifier = identifier;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public T parse(String... args) {
        return parse(0, args);
    }
    public abstract T parse(int offset, String... args);

    public List<String> getSuggestions(CommandSender source, int offset, String[] args) {
        List<String> suggestions = getSuggestions(source, args[offset]);

        if(suggestions != null && !args[offset].isEmpty() && !suggestions.isEmpty()) {
            String arg = args[offset].toUpperCase(Locale.ROOT);
            List<String> matchedSuggestions = new ArrayList<>();
            for (String suggestion : suggestions) {
                if(suggestion.toUpperCase(Locale.ROOT).startsWith(arg)) {
                    matchedSuggestions.add(suggestion);
                }
            }

            return matchedSuggestions;
        }

        return suggestions;
    }

    public List<String> getSuggestions(CommandSender source, String arg) {
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass() == getClass() && ((ParameterParser<?>) o).label.equals(label) && ((ParameterParser<?>) o).defaultValue.equals(defaultValue);
    }

    public final T parseDefaultValue() {
        return parse(defaultValue);
    }

    public final boolean isOptional() {
        return !defaultValue.isEmpty();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Property {
        int index();
    }

}