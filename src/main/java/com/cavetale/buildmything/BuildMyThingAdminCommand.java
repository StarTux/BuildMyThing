package com.cavetale.buildmything;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class BuildMyThingAdminCommand extends AbstractCommand<BuildMyThingPlugin> {
    protected BuildMyThingAdminCommand(final BuildMyThingPlugin plugin) {
        super(plugin, "buildmythingadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Build my Thing admin command");
        rootNode.addChild("start").denyTabCompletion()
            .description("Start a game")
            .senderCaller(this::start);
        rootNode.addChild("skip").denyTabCompletion()
            .description("Skip something")
            .playerCaller(this::skip);
    }

    private void start(CommandSender sender) {
        final Game game = new Game(plugin, "test");
        for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
            game.addPlayer(player);
        }
        game.enable();
        plugin.getGames().add(game);
        sender.sendMessage(textOfChildren(text("Game started: ", YELLOW),
                                          text(game.getName(), GOLD)));
    }

    private void skip(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) throw new CommandWarn("There is no game here");
        player.sendMessage(text("Trying to skip in game " + game.getName() + "...", YELLOW));
        game.getMode().skip();
    }
}
