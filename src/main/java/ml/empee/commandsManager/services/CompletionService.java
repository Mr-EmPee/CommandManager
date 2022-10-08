package ml.empee.commandsManager.services;

import org.bukkit.command.PluginCommand;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import me.lucko.commodore.Commodore;
import ml.empee.commandsManager.command.Command;
import ml.empee.commandsManager.command.CommandNode;
import ml.empee.commandsManager.parsers.ParameterParser;
import ml.empee.commandsManager.parsers.types.BoolParser;
import ml.empee.commandsManager.parsers.types.DoubleParser;
import ml.empee.commandsManager.parsers.types.FloatParser;
import ml.empee.commandsManager.parsers.types.IntegerParser;
import ml.empee.commandsManager.parsers.types.LongParser;
import ml.empee.commandsManager.parsers.types.greedy.GreedyParser;

public final class CompletionService {

  private final Commodore commodore;

  public CompletionService(Commodore commodore) {
    this.commodore = commodore;
  }

  public void registerCompletions(Command command) {
    PluginCommand pluginCommand = command.getPluginCommand();

    pluginCommand.setTabCompleter(command);

    LiteralArgumentBuilder<Object> rootNode = convertNodeToBrigadier(command.getRootNode());
    //Registering completion for default commandNode "help"
    rootNode.then(LiteralArgumentBuilder.literal("help")
        .then(RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer())));

    commodore.register(pluginCommand, rootNode);
  }

  private LiteralArgumentBuilder<Object> convertNodeToBrigadier(CommandNode node) {

    String[] labels = node.getLabel().split(" ");
    LiteralArgumentBuilder<Object> rootNode = LiteralArgumentBuilder.literal(labels[labels.length - 1]);

    ParameterParser<?>[] parsers = node.getParameterParsers();
    ArgumentBuilder<Object, ?> lastArg;

    if (parsers.length > 0) {
      lastArg = RequiredArgumentBuilder.argument(parsers[parsers.length - 1].getLabel(),
          findArgType(parsers[parsers.length - 1]));
    } else {
      lastArg = rootNode;
    }

    for (CommandNode child : node.getChildren()) {
      lastArg.then(convertNodeToBrigadier(child));
    }

    for (int i = parsers.length - 2; i >= 0; i--) {
      ArgumentBuilder<Object, ?> arg = RequiredArgumentBuilder.argument(parsers[i].getLabel(), findArgType(parsers[i]));
      arg.then(lastArg);
      lastArg = arg;
    }

    if (lastArg != rootNode) {
      rootNode.then(lastArg);
    }

    for (int i = labels.length - 2; i >= 0; i--) {
      rootNode = LiteralArgumentBuilder.literal(labels[i]).then(rootNode);
    }

    return rootNode;
  }

  private ArgumentType<?> findArgType(ParameterParser<?> rawType) {

    if (rawType instanceof GreedyParser) {
      return StringArgumentType.greedyString();
    } else if (rawType instanceof IntegerParser) {
      return IntegerArgumentType.integer();
    } else if (rawType instanceof FloatParser) {
      return FloatArgumentType.floatArg();
    } else if (rawType instanceof DoubleParser) {
      return DoubleArgumentType.doubleArg();
    } else if (rawType instanceof LongParser) {
      return LongArgumentType.longArg();
    } else if (rawType instanceof BoolParser) {
      return BoolArgumentType.bool();
    } else {
      return StringArgumentType.string();
    }

  }

}
