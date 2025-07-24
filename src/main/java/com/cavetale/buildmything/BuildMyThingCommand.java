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
            .description("Rate a build")
            .playerCaller(this::rate);
    }

    protected void rate(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) return;
        game.getMode().onRateCommand(player);
    }
}
