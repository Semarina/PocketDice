package me.sepehrhn.pocketdice.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.sepehrhn.pocketdice.util.DiceParser;
import me.sepehrhn.pocketdice.util.RollMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundCategory;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.stream.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FabricRollCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var rollNode = dispatcher.register(CommandManager.literal("roll")
                .executes(ctx -> roll(ctx, FabricConfig.DATA.default_notation, RollMode.NORMAL))
                .then(CommandManager.argument("notation", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, StringArgumentType.getString(ctx, "notation"), RollMode.NORMAL))));
        dispatcher.register(CommandManager.literal("dice").redirect(rollNode));

        var grollNode = dispatcher.register(CommandManager.literal("groll")
                .executes(ctx -> roll(ctx, FabricConfig.DATA.default_notation, RollMode.GLOBAL))
                .then(CommandManager.argument("notation", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, StringArgumentType.getString(ctx, "notation"), RollMode.GLOBAL))));
        dispatcher.register(CommandManager.literal("gdice").redirect(grollNode));

        var prollNode = dispatcher.register(CommandManager.literal("proll")
                .executes(ctx -> roll(ctx, FabricConfig.DATA.default_notation, RollMode.PRIVATE))
                .then(CommandManager.argument("notation", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, StringArgumentType.getString(ctx, "notation"), RollMode.PRIVATE))));
        dispatcher.register(CommandManager.literal("pdice").redirect(prollNode));
    }

    private static int roll(CommandContext<ServerCommandSource> ctx, String notation, RollMode mode) {
        ServerCommandSource source = ctx.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendError(me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.getRegistryManager(), "en_US", "messages.command.player_only"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        
        String localeCode = player.getClientOptions().language();

        if (mode == RollMode.GLOBAL) {
            var server = source.getServer();
            String playerName = player.getName().getString();
            boolean isOp = Stream.of(server.getPlayerManager().getOpNames()).anyMatch(name -> name.equalsIgnoreCase(playerName));
            boolean isSinglePlayer = server.isSingleplayer();
            
            if (!isOp && !isSinglePlayer) {
                 source.sendError(me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.getRegistryManager(), localeCode, "messages.command.no_permission"));
                 return 0;
            }
        }

        // Anti-Spam Check
        String playerName = player.getName().getString();
        boolean isOp = java.util.stream.Stream.of(source.getServer().getPlayerManager().getOpNames()).anyMatch(name -> name.equalsIgnoreCase(playerName));

        if (FabricConfig.DATA.cooldowns.enabled) {
            if (!isOp) {
                var res = PocketDiceFabric.ANTI_SPAM.checkCooldown(player.getUuid(), true, FabricConfig.DATA.cooldowns.seconds);
                if (!res.isAllowed()) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("remaining", String.valueOf(res.getRemainingSeconds()));
                    source.sendError(me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.getRegistryManager(), localeCode, "messages.roll.cooldown_active", map));
                    return 0;
                }
            }
        }

        if (FabricConfig.DATA.rate_limit.enabled) {
            if (!isOp) {
                var res = PocketDiceFabric.ANTI_SPAM.checkRateLimit(player.getUuid(), true, FabricConfig.DATA.rate_limit.window_seconds, FabricConfig.DATA.rate_limit.max_rolls);
                if (!res.isAllowed()) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("remaining", String.valueOf(res.getRemainingSeconds()));
                    source.sendError(me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.getRegistryManager(), localeCode, "messages.roll.rate_limited", map));
                    return 0;
                }
            }
        }

        if (notation.equalsIgnoreCase("help") || notation.equalsIgnoreCase("?")) {
            source.sendMessage(me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.getRegistryManager(), localeCode, "messages.command.roll_help"));
            return 1;
        }

        DiceParser.DiceSpec spec;
        try {
            spec = DiceParser.parse(notation, FabricConfig.DATA.allow_shorthand);
        } catch (DiceParser.DiceParseException e) {
            source.sendError(me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.getRegistryManager(), localeCode, "messages.roll.invalid_notation"));
            return 0;
        }

        PocketDiceFabric.debug(player.getName().getString() + " executing roll command. Notation: '" + notation + "' parsed to " + spec.dice() + "d" + spec.faces() + " Mode: " + mode.name());

        if (spec.dice() > FabricConfig.DATA.max_dice || spec.faces() > FabricConfig.DATA.max_faces) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("notation", notation);
            map.put("max_dice", String.valueOf(FabricConfig.DATA.max_dice));
            map.put("max_faces", String.valueOf(FabricConfig.DATA.max_faces));
            source.sendError(me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(source.getRegistryManager(), localeCode, "messages.roll.limits_exceeded", map));
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
        Component adventureComponent = DiceParser.formatResult(player.getNameForScoreboard(), new me.sepehrhn.pocketdice.util.DiceRoll(spec, rolls, total));
        
        // Convert to native Minecraft Text via JSON using TextCodecs (1.21+)
        String json = GsonComponentSerializer.gson().serialize(adventureComponent);
        Text message;
        try {
            message = TextCodecs.CODEC.parse(source.getRegistryManager().getOps(JsonOps.INSTANCE), JsonParser.parseString(json))
                .result()
                .orElse(Text.literal("Error formatting message: Parse failed"));
        } catch (Exception e) {
            message = Text.literal("Error formatting message: " + e.getMessage());
        }

        // Send to player
        player.sendMessage(message, false);

        // Broadcast logic based on RollMode
        if (mode == RollMode.GLOBAL) {
            for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
                if (p != player) {
                    p.sendMessage(message, false);
                }
            }
        } else if (mode == RollMode.NORMAL) {
            double radius = FabricConfig.DATA.radius;
            double r2 = radius * radius;
            for (ServerPlayerEntity p : source.getWorld().getPlayers()) {
                if (p != player && p.squaredDistanceTo(player) <= r2) {
                    p.sendMessage(message, false);
                }
            }
        }
        // PRIVATE mode does nothing else since it's already sent to the player.

        PocketDiceFabric.ANTI_SPAM.recordRoll(player.getUuid());

        // --- Sound Logic ---
        if (FabricConfig.DATA.sounds.roll.enabled) {
            String soundId = FabricConfig.DATA.sounds.roll.sound_key;
            Identifier id = Identifier.tryParse(soundId);
            if (id != null) {
                var optionalSound = Registries.SOUND_EVENT.getOptional(RegistryKey.of(RegistryKeys.SOUND_EVENT, id));
                if (optionalSound.isPresent()) {
                    SoundEvent sound = optionalSound.get().value();
                    // Play sound
                    source.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), 
                        sound, SoundCategory.PLAYERS, 
                        (float) FabricConfig.DATA.sounds.roll.volume, 
                        (float) FabricConfig.DATA.sounds.roll.pitch);
                }
            }
        }

        return 1;
    }
}
