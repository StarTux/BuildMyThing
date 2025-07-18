package com.cavetale.buildmything;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class BuildMyThingCommand extends AbstractCommand<BuildMyThingPlugin> {
    protected BuildMyThingCommand(final BuildMyThingPlugin plugin) {
        super(plugin, "buildmything");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").denyTabCompletion()
            .description("Start a game")
            .senderCaller(this::start);
    }

    protected void start(CommandSender sender) {
        final Game game = new Game(plugin, "test");
        for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
            game.addPlayer(player);
        }
        game.enable();
        plugin.getGames().add(game);
        sender.sendMessage(textOfChildren(text("Game started: ", YELLOW),
                                          text(game.getName(), GOLD)));
    }
}
