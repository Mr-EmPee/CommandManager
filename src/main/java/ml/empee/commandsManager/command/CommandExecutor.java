package ml.empee.commandsManager.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import ml.empee.commandsManager.CommandManager;
import ml.empee.commandsManager.parsers.ParameterParser;
import ml.empee.commandsManager.services.HelpMenuService;
import ml.empee.commandsManager.utils.CommandMapUtils;
import ml.empee.commandsManager.utils.PluginCommandUtils;
import ml.empee.commandsManager.utils.helpers.Tuple;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

public abstract class CommandExecutor extends Controller implements org.bukkit.command.CommandExecutor {

  @Setter
  private static String prefix = "&4&l > ";
  protected static String malformedCommandMSG = "The command is missing arguments, check the help menu";
  protected static String missingPermissionsMSG = "You haven't enough permissions";
  protected static String runtimeErrorMSG = "Error while executing the command";
  protected static String invalidSenderMSG = "You aren't an allowed sender type of this command";

  @Getter
  protected PluginCommand pluginCommand;
  @Getter
  protected Node rootNode;
  @Getter
  protected HelpMenuService helpMenu;
  protected Logger logger;

  public final boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
    try {
      parseParametersAndExecuteNode(new CommandContext(sender), rootNode, args, 0);
    } catch (CommandException exception) {
      String message = exception.getMessage().replace("&r", "&c");
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&c" + message));

      Throwable cause = exception.getCause();
      if (cause != null) {
        if (cause instanceof InvocationTargetException) {
          cause = cause.getCause();
        }

        logger.log(Level.SEVERE, "Error while executing the command {0} \n\t - Arguments: {1}",
            new Object[] {command.getName(), Arrays.toString(args)}
        );

        logger.log(Level.SEVERE, "Stacktrace:", cause);
      }
    }

    return true;
  }

  private void parseParametersAndExecuteNode(
      CommandContext context, Node node, String[] args, int offset
  ) throws CommandException {
    if (node == null) {
      throw new CommandException(malformedCommandMSG);
    } else {
      if (!context.getSource().hasPermission(node.getData().permission())) {
        throw new CommandException(missingPermissionsMSG);
      }

      ParameterParser<?>[] parsers = node.getParameterParsers();
      List<Tuple<String, Object>> arguments = parseArguments(parsers, args, offset);
      executeNode(context, node, arguments);
      context.addArguments(arguments);
      offset += parsers.length;

      findAndExecuteChild(context, node, args, offset);
    }
  }

  private void findAndExecuteChild(CommandContext context, Node node, String[] args, int offset) throws CommandException {
    if (node.getChildren().length == 0) {
      if (!node.getData().executable()) {
        throw new CommandException(malformedCommandMSG);
      }
    } else {
      Node nextNode = node.findNextNode(args, offset);
      if (nextNode == null && !node.getData().executable()) {
        throw new CommandException(malformedCommandMSG);
      } else if (nextNode != null) {
        parseParametersAndExecuteNode(context, nextNode, args, offset + nextNode.getData().label().split(" ").length);
      }
    }
  }

  private void executeNode(CommandContext context, Node node, List<Tuple<String, Object>> arguments) throws CommandException {
    Object[] args = new Object[arguments.size() + 1];
    args[0] = context.getSource();
    if (!node.getSenderType().isInstance(args[0])) {
      throw new CommandException(invalidSenderMSG);
    }

    int i = 1;
    for (Tuple<String, Object> arg : arguments) {
      args[i] = arg.getSecond();
      i += 1;
    }

    try {
      executeNode(context, node, args);
    } catch (Exception e) {
      if (e.getCause() instanceof CommandException) {
        throw (CommandException) e.getCause();
      }

      throw new CommandException(runtimeErrorMSG, e);
    }
  }

  private List<Tuple<String, Object>> parseArguments(ParameterParser<?>[] parsers, String[] args, int offset) {
    List<Tuple<String, Object>> arguments = new ArrayList<>();

    for (ParameterParser<?> parser : parsers) {
      if (offset >= args.length) {
        if (parser.isOptional()) {
          arguments.add(Tuple.of(parser.getLabel(), parser.getDefaultValue()));
        } else {
          throw new CommandException(malformedCommandMSG);
        }
      } else {
        arguments.add(Tuple.of(parser.getLabel(), parser.parse(offset, args)));
      }
      offset += 1;
    }

    return arguments;
  }

  public PluginCommand build(CommandManager commandManager) {
    logger = commandManager.getPlugin().getLogger();
    rootNode = Node.buildCommandTree(commandManager, this);
    //No need to check existence of the annotation, it's already done in the CommandNode
    pluginCommand = PluginCommandUtils.of(getClass().getAnnotation(CommandNode.class));
    pluginCommand.setExecutor(this);
    helpMenu = new HelpMenuService(pluginCommand.getPlugin().getName(), rootNode);
    return pluginCommand;
  }

  @Override
  public void unregister() {
    super.unregister();
    CommandMapUtils.unregisterCommand(pluginCommand);
  }
}