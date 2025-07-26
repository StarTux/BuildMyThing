package com.cavetale.buildmything;

import com.cavetale.buildmything.mode.GameplayType;
import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.item.trophy.TrophyCategory;
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
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload the tag")
            .senderCaller(this::reload);
        rootNode.addChild("start").arguments("[type]")
            .completers(CommandArgCompleter.enumLowerList(GameplayType.class))
            .description("Start a game")
            .senderCaller(this::start);
        rootNode.addChild("skip").denyTabCompletion()
            .description("Skip something")
            .playerCaller(this::skip);
        rootNode.addChild("stop").denyTabCompletion()
            .description("Stop this game")
            .playerCaller(this::stop);
        final CommandNode scoreNode = rootNode.addChild("score")
            .description("Score commands");
        scoreNode.addChild("clear").denyTabCompletion()
            .description("Clear all scores")
            .senderCaller(this::scoreClear);
        rootNode.addChild("event").arguments("[true|false]")
            .completers(CommandArgCompleter.BOOLEAN)
            .description("Set event mode")
            .senderCaller(this::event);
        scoreNode.addChild("reward").denyTabCompletion()
            .description("Reward all scores")
            .senderCaller(this::scoreReward);
        scoreNode.addChild("add")
            .description("Manipulate score")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i != 0))
            .senderCaller(this::scoreAdd);
    }

    private void reload(CommandSender sender) {
        plugin.loadTag();
        sender.sendMessage(text("Tag reloaded", YELLOW));
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 0 && args.length != 1) return false;
        final GameplayType type = args.length >= 1
            ? CommandArgCompleter.requireEnum(GameplayType.class, args[0])
            : null;
        final Game game = new Game(plugin, "test");
        if (type != null) {
            game.setMode(type.createGameplayMode(game));
        }
        for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
            game.addPlayer(player);
        }
        game.enable();
        plugin.getGames().add(game);
        sender.sendMessage(textOfChildren(text("Game started: ", YELLOW),
                                          text(game.getName(), GOLD)));
        return true;
    }

    private void skip(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) throw new CommandWarn("There is no game here");
        player.sendMessage(text("Trying to skip in game " + game.getName() + "...", YELLOW));
        game.getMode().skip();
    }

    private void stop(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) throw new CommandWarn("There is no game here");
        player.sendMessage(text("Trying to stop game " + game.getName() + "...", YELLOW));
        game.setFinished(true);
    }

    private void scoreClear(CommandSender sender) {
        plugin.getTag().getScores().clear();
        plugin.computeHighscore();
        sender.sendMessage(text("Scores cleared", YELLOW));
    }

    private boolean event(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 1) {
            plugin.getTag().setEvent(CommandArgCompleter.requireBoolean(args[0]));
            plugin.saveTag();
        }
        sender.sendMessage(textOfChildren(text("Event mode: ", YELLOW),
                                          (plugin.getTag().isEvent()
                                           ? text("Yes", GREEN)
                                           : text("No", RED))));
        return true;
    }

    private void scoreReward(CommandSender sender) {
        final int count = Highscore.reward(plugin.getTag().getScores(),
                                           "build_my_thing",
                                           TrophyCategory.CUP,
                                           plugin.getTitle(),
                                           hi -> "You scored " + hi.score + " points");
        sender.sendMessage(text(count + " players rewarded with trophies", YELLOW));
    }

    private boolean scoreAdd(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        int value = CommandArgCompleter.requireInt(args[1], i -> i != 0);
        plugin.getTag().addScore(target.uuid, value);
        plugin.computeHighscore();
        sender.sendMessage(text("Score of " + target.name + " manipulated by " + value, AQUA));
        return true;
    }
}
