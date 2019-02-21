package com.arckenver.nations.cmdexecutor.nation;

import com.arckenver.nations.*;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NationWarExecutor implements CommandExecutor {

    public static void create(CommandSpec.Builder cmd) {
        cmd.child(CommandSpec.builder()
                .description(Text.of(""))
                .permission("nations.command.nation.war")
                .arguments(GenericArguments.optional(GenericArguments.string(Text.of("nation"))))
                .executor(new NationWarExecutor())
                .build(), "war");
    }

    public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException
    {
        if (!ctx.<String>getOne("nation").isPresent())
        {
            src.sendMessage(Text.of(TextColors.YELLOW, "/n war <nation>"));
            return CommandResult.success();
        }
        if (src instanceof Player)
        {
            Player player = (Player) src;
            if (!ConfigHandler.getNode("worlds").getNode(player.getWorld().getName()).getNode("enabled").getBoolean())
            {
                src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_PLUGINDISABLEDINWORLD));
                return CommandResult.success();
            }
            if (DataHandler.getNationOfPlayer(player.getUniqueId()) == null)
            {
                src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOTINNATION));
                return CommandResult.success();
            }
            String nationName = ctx.<String>getOne("nation").get();
            if (DataHandler.getNation(nationName) == null)
            {
                src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NATIONDOESNTEXIST));
                return CommandResult.success();
            }
            if (!nationName.matches("[\\p{Alnum}\\p{IsIdeographic}\\p{IsLetter}\"_\"]*"))
            {
                src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NAMEALPHA));
                return CommandResult.success();
            }

            MessageChannel.TO_ALL.send(Text.of(TextColors.AQUA, LanguageHandler.INFO_NEWWARANNOUNCE.replaceAll("\\{NATION\\}", DataHandler.getNationOfPlayer(player.getUniqueId()).getName()).replaceAll("\\{ENEMYNATION\\}", nationName)));

            ServerBossBar warScore = ServerBossBar.builder()
                    .name(Text.of("War Score"))
                    .percent(.5f)
                    .color(BossBarColors.RED)
                    .overlay(BossBarOverlays.PROGRESS)
                    .build();

            for (UUID playerUUID : Objects.requireNonNull(DataHandler.getNationOfPlayer(player.getUniqueId())).getCitizens()) {
                Sponge.getServer().getPlayer(playerUUID).ifPresent(warScore::addPlayer);
            }

            for (UUID playerUUID : Objects.requireNonNull(DataHandler.getNation(nationName)).getCitizens()) {
                Sponge.getServer().getPlayer(playerUUID).ifPresent(warScore::addPlayer);
            }

            Task.builder().execute(() -> {
                for (Player playerToMessage : warScore.getPlayers()) {
                    playerToMessage.sendMessage(Text.of("Not enough land to war!"));
                }
                warScore.removePlayers(warScore.getPlayers());
            }).async().delay(15, TimeUnit.SECONDS).name("War Removal").submit(NationsPlugin.getInstance());
        }
        else
        {
            src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOPLAYER));
        }
        return CommandResult.success();
    }
}
