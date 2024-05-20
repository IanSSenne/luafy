package dev.diamond.luafy.script.lua;

import dev.diamond.luafy.script.abstraction.AdaptableFunction;
import dev.diamond.luafy.script.abstraction.lang.AbstractBaseValue;
import dev.diamond.luafy.script.abstraction.obj.ScriptObjectProvider;
import dev.diamond.luafy.util.HexId;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class LuaBaseValue extends AbstractBaseValue<LuaValue, LuaBaseValue> {

    public LuaBaseValue(LuaValue value) {
        super(value);
    }

    @Override public String asString() {
        return value.checkjstring();
    }
    @Override public Object getLangNull() {
        return LuaValue.NIL;
    }
    @Override public int asInt() {
        return value.checkint();
    }
    @Override public long asLong() {
        return value.checklong();
    }
    @Override public float asFloat() {
        return (float) value.checkdouble();
    }
    @Override public double asDouble() {
        return value.checkdouble();
    }
    @Override public boolean asBoolean() {
        return value.checkboolean();
    }
    @Override public HashMap<LuaBaseValue, LuaBaseValue> asMap() {
        HashMap<LuaBaseValue, LuaBaseValue> hash = new HashMap<>();
        LuaTable table = value.checktable();
        for (int i = 0; i < table.narg(); i++) {
            hash.put(new LuaBaseValue(table.keys()[i]), new LuaBaseValue(table.get(table.keys()[i])));
        }

        return hash;
    }
    @Override public Collection<LuaBaseValue> asCollection() {
        Collection<LuaBaseValue> collection = new ArrayList<>();
        LuaTable table = value.checktable();
        for (int i = 0; i < table.length(); i++) {
            collection.add(new LuaBaseValue(table.get(i + 1)));
        }
        return collection;
    }

    @Override public boolean isString() {
        return value.isstring();
    }
    @Override public boolean isInt() {
        return value.isint();
    }
    @Override public boolean isLong() {
        return value.islong();
    }
    @Override public boolean isFloat() {
        return value.isnumber();
    }
    @Override public boolean isDouble() {
        return value.isnumber();
    }
    @Override public boolean isBool() {
        return value.isboolean();
    }
    @Override public boolean isMap() {
        return value.istable() && !isCollection();
    }
    @Override public boolean isCollection() {
        if (!value.istable()) return false;
        else {
            LuaValue[] keys = value.checktable().keys();
            boolean allNumbers = true;
            for (var key : keys) {
                if (!key.isnumber()) {
                    allNumbers = false;
                    break;
                }
            }
            return allNumbers;
        }
    }

    @Override
    public LuaBaseValue adaptAbstract(Object obj) {
        try {
            if (obj instanceof LuaValue luaval)
                return new LuaBaseValue(luaval);
            else if (obj instanceof HashMap<?, ?> hash)
                return new LuaBaseValue(LuaTypeConversions.hashToLua(hash, this::adapt));
            else if (obj instanceof Collection<?> collection)
                return new LuaBaseValue(LuaTypeConversions.collToLua(collection, this::adapt));
            else
                return new LuaBaseValue(LuaTypeConversions.luaFromObj(obj));
        } catch (Exception e) {
            throw new RuntimeException("Could not adapt type " + obj.getClass() + ": " + e);
        }
    }

    @Override
    public LuaBaseValue addObject(ScriptObjectProvider obj) {
        LuaTable table = new LuaTable();
        HashMap<String, AdaptableFunction> functions = new HashMap<>();
        obj.provide().addFunctions(functions);
        for (var kvp : functions.entrySet()) {
            table.set(kvp.getKey(), LuaScript.adaptableToArrArg(kvp.getValue()));
        }
        return new LuaBaseValue(table);
    }
}