package me.sepehrhn.pocketdice.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.sepehrhn.pocketdice.util.DiceParser;
import me.sepehrhn.pocketdice.util.RollMode;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.util.stream.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FabricRollCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var rollNode = dispatcher.register(Commands.literal("roll")
                .executes(ctx -> roll(ctx, FabricConfig.DATA.default_notation, RollMode.NORMAL))
                .then(Commands.argument("notation", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, StringArgumentType.getString(ctx, "notation"), RollMode.NORMAL))));
        dispatcher.register(Commands.literal("dice").redirect(rollNode));

        var grollNode = dispatcher.register(Commands.literal("groll")
                .executes(ctx -> roll(ctx, FabricConfig.DATA.default_notation, RollMode.GLOBAL))
                .then(Commands.argument("notation", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, StringArgumentType.getString(ctx, "notation"), RollMode.GLOBAL))));
        dispatcher.register(Commands.literal("gdice").redirect(grollNode));

        var prollNode = dispatcher.register(Commands.literal("proll")
                .executes(ctx -> roll(ctx, FabricConfig.DATA.default_notation, RollMode.PRIVATE))
                .then(Commands.argument("notation", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, StringArgumentType.getString(ctx, "notation"), RollMode.PRIVATE))));
        dispatcher.register(Commands.literal("pdice").redirect(prollNode));
    }

    private static int roll(CommandContext<CommandSourceStack> ctx, String notation, RollMode mode) {
        CommandSourceStack source = ctx.getSource();
        if (!source.isPlayer()) {
            sendError(source, me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.registryAccess(), "en_US", "messages.command.player_only"));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        
        String localeCode = player.clientInformation().language();

        if (mode == RollMode.GLOBAL) {
            var server = source.getServer();
            String playerName = player.getName().getString();
            boolean isOp = Stream.of(server.getPlayerList().getOpNames()).anyMatch(name -> name.equalsIgnoreCase(playerName));
            boolean isSinglePlayer = server.isSingleplayer();
            
            if (!isOp && !isSinglePlayer) {
                 sendError(source, me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.registryAccess(), localeCode, "messages.command.no_permission"));
                 return 0;
            }
        }

        // Anti-Spam Check
        String playerName = player.getName().getString();
        boolean isOp = java.util.stream.Stream.of(source.getServer().getPlayerList().getOpNames()).anyMatch(name -> name.equalsIgnoreCase(playerName));

        if (FabricConfig.DATA.cooldowns.enabled) {
            if (!isOp) {
                var res = PocketDiceFabric.ANTI_SPAM.checkCooldown(player.getUUID(), true, FabricConfig.DATA.cooldowns.seconds);
                if (!res.isAllowed()) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("remaining", String.valueOf(res.getRemainingSeconds()));
                    sendError(source, me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.registryAccess(), localeCode, "messages.roll.cooldown_active", map));
                    return 0;
                }
            }
        }

        if (FabricConfig.DATA.rate_limit.enabled) {
            if (!isOp) {
                var res = PocketDiceFabric.ANTI_SPAM.checkRateLimit(player.getUUID(), true, FabricConfig.DATA.rate_limit.window_seconds, FabricConfig.DATA.rate_limit.max_rolls);
                if (!res.isAllowed()) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("remaining", String.valueOf(res.getRemainingSeconds()));
                    sendError(source, me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.registryAccess(), localeCode, "messages.roll.rate_limited", map));
                    return 0;
                }
            }
        }

        if (notation.equalsIgnoreCase("help") || notation.equalsIgnoreCase("?")) {
            sendInfo(source, me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.registryAccess(), localeCode, "messages.command.roll_help"));
            return 1;
        }

        DiceParser.DiceSpec spec;
        try {
            spec = DiceParser.parse(notation, FabricConfig.DATA.allow_shorthand);
        } catch (DiceParser.DiceParseException e) {
            sendError(source, me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.registryAccess(), localeCode, "messages.roll.invalid_notation"));
            return 0;
        }

        PocketDiceFabric.debug(player.getName().getString() + " executing roll command. Notation: '" + notation + "' parsed to " + spec.dice() + "d" + spec.faces() + " Mode: " + mode.name());

        if (spec.dice() > FabricConfig.DATA.max_dice || spec.faces() > FabricConfig.DATA.max_faces) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("notation", notation);
            map.put("max_dice", String.valueOf(FabricConfig.DATA.max_dice));
            map.put("max_faces", String.valueOf(FabricConfig.DATA.max_faces));
            sendError(source, me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.registryAccess(), localeCode, "messages.roll.limits_exceeded", map));
            return 0;
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<Integer> rolls = new ArrayList<>(spec.dice());
        int total = 0;
        for (int i = 0; i < spec.dice(); i++) {
            int val = rng.nextInt(1, spec.faces() + 1);
            rolls.add(val);
            total += val;
        }

        // --- MiniMessage Formatting ---
        net.kyori.adventure.text.Component adventureComponent = DiceParser.formatResult(player.getScoreboardName(), new me.sepehrhn.pocketdice.util.DiceRoll(spec, rolls, total));
        
        // Convert to native Minecraft Component via JSON using TextCodecs (1.21+)
        String json = GsonComponentSerializer.gson().serialize(adventureComponent);
        Component message;
        try {
            message = ComponentSerialization.CODEC.parse(source.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                .result()
                .orElse(Component.literal("Error formatting message: Parse failed"));
        } catch (Exception e) {
            message = Component.literal("Error formatting message: " + e.getMessage());
        }

        // Send to player
        player.sendSystemMessage(message);

        // Broadcast logic based on RollMode
        if (mode == RollMode.GLOBAL) {
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                if (p != player) {
                    p.sendSystemMessage(message);
                }
            }
        } else if (mode == RollMode.NORMAL) {
            double radius = FabricConfig.DATA.radius;
            double r2 = radius * radius;
            for (ServerPlayer p : source.getLevel().players()) {
                if (p != player && p.distanceToSqr(player) <= r2) {
                    p.sendSystemMessage(message);
                }
            }
        }
        // PRIVATE mode does nothing else since it's already sent to the player.

        PocketDiceFabric.ANTI_SPAM.recordRoll(player.getUUID());

        // --- Sound Logic ---
        if (FabricConfig.DATA.sounds.roll.enabled) {
            String soundId = FabricConfig.DATA.sounds.roll.sound_key;
            Identifier id = Identifier.tryParse(soundId);
            if (id != null) {
                var optionalSound = BuiltInRegistries.SOUND_EVENT.getOptional(id);
                if (optionalSound.isPresent()) {
                    SoundEvent sound = optionalSound.get();
                    // Play sound
                    source.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        sound, SoundSource.PLAYERS,
                        (float) FabricConfig.DATA.sounds.roll.volume, 
                        (float) FabricConfig.DATA.sounds.roll.pitch);
                }
            }
        }

        return 1;
    }

    private static void sendInfo(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    private static void sendError(CommandSourceStack source, Component message) {
        source.sendFailure(message);
    }
}
