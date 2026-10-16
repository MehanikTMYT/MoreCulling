package ca.fxco.moreculling.config;

import ca.fxco.moreculling.MoreCulling;
import ca.fxco.moreculling.config.option.LeavesCullingMode;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fleet build: reads/writes {@code config/moreculling.toml} without Cloth Config / AutoConfig.
 * Supports exactly the TOML subset Toml4j wrote for {@link MoreCullingConfig}: {@code key = value} for booleans,
 * numbers, quoted strings/enum names and string arrays, plus the {@code [modCompatibility]} table. Unknown keys
 * are ignored, missing keys keep their defaults, an unreadable file falls back to defaults (as before).
 */
public final class MoreCullingConfigIO {
    private static final String TABLE = "modCompatibility";

    private MoreCullingConfigIO() {}

    public static Path file(Path configDir) {
        return configDir.resolve("moreculling.toml");
    }

    public static MoreCullingConfig load(Path file) {
        MoreCullingConfig config = new MoreCullingConfig();
        if (!Files.exists(file)) {
            save(file, config);
            return config;
        }
        try {
            boolean inTable = false;
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = stripComment(raw).trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("[")) {
                    inTable = line.equals("[" + TABLE + "]");
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = unquote(line.substring(0, eq).trim());
                String value = line.substring(eq + 1).trim();
                if (inTable) {
                    config.modCompatibility.put(key, Boolean.parseBoolean(value));
                } else {
                    set(config, key, value);
                }
            }
        } catch (Exception e) {
            MoreCulling.LOGGER.warn("Could not read {}, using defaults", file, e);
            return new MoreCullingConfig();
        }
        return config;
    }

    public static void save(Path file, MoreCullingConfig config) {
        StringBuilder out = new StringBuilder();
        try {
            for (Field f : fields()) {
                if (f.getName().equals(TABLE)) continue;
                out.append(f.getName()).append(" = ").append(format(f.get(config))).append('\n');
            }
            out.append('\n').append('[').append(TABLE).append("]\n");
            for (var e : config.modCompatibility.object2BooleanEntrySet()) {
                out.append(quote(e.getKey())).append(" = ").append(e.getBooleanValue()).append('\n');
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException | IllegalAccessException e) {
            MoreCulling.LOGGER.error("Could not save {}", file, e);
        }
    }

    private static List<Field> fields() {
        List<Field> list = new ArrayList<>();
        for (Field f : MoreCullingConfig.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers())) list.add(f);
        }
        return list;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void set(MoreCullingConfig config, String key, String value) throws IllegalAccessException {
        Field f;
        try {
            f = MoreCullingConfig.class.getField(key);
        } catch (NoSuchFieldException e) {
            return;
        }
        Class<?> t = f.getType();
        if (t == boolean.class) f.setBoolean(config, Boolean.parseBoolean(value));
        else if (t == int.class) f.setInt(config, (int) Double.parseDouble(value));
        else if (t == float.class) f.setFloat(config, Float.parseFloat(value));
        else if (t == double.class) f.setDouble(config, Double.parseDouble(value));
        else if (t == String.class) f.set(config, unquote(value));
        else if (t.isEnum()) {
            try {
                f.set(config, Enum.valueOf((Class<Enum>) t, unquote(value)));
            } catch (IllegalArgumentException ignored) {
                // unknown enum constant: keep the default
            }
        } else if (List.class.isAssignableFrom(t)) {
            List<String> list = new ArrayList<>();
            String body = value.startsWith("[") && value.endsWith("]") ? value.substring(1, value.length() - 1) : value;
            for (String part : splitArray(body)) {
                String s = unquote(part.trim());
                if (!s.isEmpty()) list.add(s);
            }
            f.set(config, list);
        }
    }

    private static String format(Object v) {
        if (v instanceof String s) return quote(s);
        if (v instanceof Enum<?> e) return quote(e.name());
        if (v instanceof List<?> list) {
            StringBuilder b = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) b.append(i == 0 ? "" : ", ").append(quote(String.valueOf(list.get(i))));
            return b.append(']').toString();
        }
        return String.valueOf(v);
    }

    private static List<String> splitArray(String body) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) inString = !inString;
            if (c == ',' && !inString) { parts.add(cur.toString()); cur.setLength(0); } else cur.append(c);
        }
        if (!cur.isEmpty()) parts.add(cur.toString());
        return parts;
    }

    private static String stripComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) inString = !inString;
            if (c == '#' && !inString) return line.substring(0, i);
        }
        return line;
    }

    private static String quote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return s;
    }

}
