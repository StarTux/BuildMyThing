package com.cavetale.buildmything;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class BuildMyThingCommand extends AbstractCommand<BuildMyThingPlugin> {
    protected BuildMyThingCommand(final BuildMyThingPlugin plugin) {
        super(plugin, "bmt");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Build my Thing command interface");
        rootNode.addChild("rate").denyTabCompletion()
            .hidden(true)
            .description("Rate a build")
            .playerCaller(this::rate);
        rootNode.addChild("guess").denyTabCompletion()
            .alias("suggest")
            .alias("choose")
            .hidden(true)
            .description("Guess what a build is")
            .playerCaller(this::guess);
        rootNode.addChild("vote").arguments("[type]")
            .denyTabCompletion()
            .hidden(true)
            .description("Vote for a Game Mode")
            .playerCaller(this::vote);
    }

    private void rate(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) return;
        game.getMode().onRateCommand(player);
    }

    private boolean guess(Player player, String[] args) {
        if (args.length == 0) return true;
        final Game game = Game.in(player.getWorld());
        if (game == null) return true;
        game.getMode().onGuessCommand(player, String.join(" ", args));
        return true;
    }

    private boolean vote(Player player, String[] args) {
        if (plugin.getGameVote() == null) return true;
        if (args.length == 0) {
            plugin.getGameVote().open(player);
        } else {
            plugin.getGameVote().onVote(player, String.join(" ", args));
        }
        return true;
    }
}
