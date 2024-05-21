package dev.diamond.luafy.script.api;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.diamond.luafy.mixin.ParsedArgumentAccessor;
import dev.diamond.luafy.script.ScriptManager;
import dev.diamond.luafy.script.abstraction.lang.AbstractBaseValue;
import dev.diamond.luafy.script.abstraction.lang.AbstractScript;
import dev.diamond.luafy.script.abstraction.api.AbstractScriptApi;
import dev.diamond.luafy.script.abstraction.AdaptableFunction;
import dev.diamond.luafy.script.api.obj.argument.ICommandArgumentScriptObject;
import dev.diamond.luafy.util.HexId;
import net.minecraft.server.command.ServerCommandSource;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandApi extends AbstractScriptApi {


    public CommandApi(AbstractScript<?> script) {
        super(script, "command");
    }

    @Override
    public HashMap<String, AdaptableFunction> getFunctions() {
        HashMap<String, AdaptableFunction> f = new HashMap<>();

        f.put("execute", args -> {
            var parsed = parseCommand(args[0].asString(), script.source);
            return executeCommand(parsed, script.source);
        });

        f.put("parse", args -> {
            var parsed = parseCommand(args[0].asString(), script.source);
            var hexid = HexId.makeNewUnique(ScriptManager.Caches.PREPARSED_COMMANDS.keySet());
            ScriptManager.Caches.PREPARSED_COMMANDS.put(hexid, parsed);

            return hexid;
        });

        f.put("get_preparsed_argument", args -> {
            var hi = HexId.fromString(args[0].asString());
            var parsed = hi.getHashed(ScriptManager.Caches.PREPARSED_COMMANDS);
            return getArgument(parsed, args[1].asString());
        });

        f.put("modify_preparsed_argument", args -> {
            var hi = HexId.fromString(args[0].asString());
            var parsed = hi.getHashed(ScriptManager.Caches.PREPARSED_COMMANDS);
            modifyArgument(parsed, args[1].asString(), args[2].value);
            return null;
        });

        f.put("execute_preparsed", args -> {
            var hi = HexId.fromString(args[0].asString());
            var parse = hi.getHashed(ScriptManager.Caches.PREPARSED_COMMANDS);
            return executeCommand(parse, script.source);
        });

        f.put("free_preparsed", args -> {
            var hi = HexId.fromString(args[0].asString());
            hi.removeHashed(ScriptManager.Caches.PREPARSED_COMMANDS);
            return null;
        });

        return f;
    }

    public static ParseResults<ServerCommandSource> parseCommand(String command, ServerCommandSource source) {
        return source.getDispatcher().parse(command, source);
    }
    public static int executeCommand(ParseResults<ServerCommandSource> command, ServerCommandSource source) {
        try {
            AtomicInteger r = new AtomicInteger();
            source.getDispatcher().setConsumer((context, success, result) -> {
                r.set(result);
            });

            source.getDispatcher().execute(command);

            return r.get();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractBaseValue<?, ?> getArgument(ParseResults<ServerCommandSource> command, String argument) {
        return ICommandArgumentScriptObject.adapt(command.getContext().getArguments().get(argument).getResult(), o -> script.getNullBaseValue().adapt(o));
    }

    public static void modifyArgument(ParseResults<ServerCommandSource> command, String argument, Object value) {
        ((ParsedArgumentAccessor) command.getContext().getArguments().get(argument)).setResult(value);
    }

}
