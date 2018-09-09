package io.anuke.ucore.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StreamUtils.OptimizedByteArrayOutputStream;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.io.DefaultSerializers;
import io.anuke.ucore.io.ReusableByteArrayInputStream;
import io.anuke.ucore.io.TypeSerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

@SuppressWarnings("unchecked")
public class Settings{
    private static Preferences prefs;
    private static ObjectMap<String, Object> defaults = new ObjectMap<>();
    private static boolean disabled = false;
    private static Runnable errorHandler;

    private static ObjectMap<Class<?>, TypeSerializer<?>> serializers = new ObjectMap<>();
    private static ObjectMap<String, TypeSerializer<?>> serializerNames = new ObjectMap<>();
    private static ObjectMap<Class<?>, String> classNames = new ObjectMap<>();

    private static ByteArrayOutputStream byteStream = new OptimizedByteArrayOutputStream(16);
    private static ReusableByteArrayInputStream byteInputStream = new ReusableByteArrayInputStream();
    private static DataOutputStream dataOutput = new DataOutputStream(byteStream);
    private static DataInputStream dataInput = new DataInputStream(byteInputStream);

    static{
        DefaultSerializers.register();
    }

    public static Preferences prefs(){
        return prefs;
    }

    public static void setErrorHandler(Runnable handler){
        errorHandler = handler;
    }

    public static void load(String name){
        prefs = Gdx.app.getPreferences(name);
    }

    /** Loads binds as well as prefs. */
    public static void loadAll(String name){
        load(name);
        KeyBinds.load();
    }

    public static Object getDefault(String name){
        return defaults.get(name);
    }

    public static void put(String name, Object val){
        if(val instanceof Float)
            putFloat(name, (Float) val);
        else if(val instanceof Integer)
            putInt(name, (Integer) val);
        else if(val instanceof String)
            putString(name, (String) val);
        else if(val instanceof Boolean)
            putBool(name, (Boolean) val);
        else if(val instanceof Long)
            putLong(name, (Long) val);
    }

    public static void putString(String name, String val){
        prefs.putString(name, val);
    }

    public static <T> void setSerializer(Class<T> type, TypeSerializer<T> serializer){
        serializers.put(type, serializer);
        serializerNames.put(classID(type), serializer);
    }

    public static TypeSerializer getSerializer(String name){
        return serializerNames.get(name);
    }

    public static TypeSerializer getSerializer(Class<?> type){
        return serializers.get(type);
    }

    public static String classID(Class<?> type){
        if(classNames.containsKey(type)){
            return classNames.get(type);
        }
        classNames.put(type, type.toString().split("@")[0]);
        return classNames.get(type);
    }

    public static synchronized void putBinary(String name, Object value){
        putBinary(name, value, value.getClass());
    }

    public static synchronized void putBinary(String name, Object value, Class<?> type){
        byteStream.reset();
        if(!serializers.containsKey(type)){
            throw new IllegalArgumentException(type + " does not have a serializer registered!");
        }
        TypeSerializer serializer = serializers.get(type);
        try{
            serializer.write(dataOutput, value);
            byte[] bytes = byteStream.toByteArray();
            String str = new String(Base64Coder.encode(bytes));
            putString(name, str);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void putFloat(String name, float val){
        prefs.putFloat(name, val);
    }

    public static void putInt(String name, int val){
        prefs.putInteger(name, val);
    }

    public static void putBool(String name, boolean val){
        prefs.putBoolean(name, val);
    }

    public static void putLong(String name, long val){
        prefs.putLong(name, val);
    }

    public static String getString(String name){
        return prefs.getString(name, (String) def(name));
    }

    public static synchronized <T> T getBinary(String name, Class<T> type, Supplier<T> def){
        T t = getBinary(name, type);
        return t == null ? def.get() : t;
    }

    private static synchronized <T> T getBinary(String name, Class<T> type){
        if(!serializers.containsKey(type)){
            throw new IllegalArgumentException("Type " + type + " does not have a serializer registered!");
        }

        String str = getString(name, null);
        if(str == null) return null;

        TypeSerializer serializer = serializers.get(type);

        try{
            byteInputStream.setBytes(Base64Coder.decode(str));
            Object obj = serializer.read(dataInput);
            return (T)obj;
        }catch(Exception e){
            return null;
        }
    }

    public static float getFloat(String name){
        return prefs.getFloat(name, (Float) def(name));
    }

    public static int getInt(String name){
        return prefs.getInteger(name, (Integer) def(name));
    }

    public static boolean getBool(String name){
        return prefs.getBoolean(name, (Boolean) def(name));
    }

    public static long getLong(String name){
        return prefs.getLong(name, (Long) def(name));
    }


    public static String getString(String name, String def){
        return prefs.getString(name, def);
    }

    public static float getFloat(String name, float def){
        return prefs.getFloat(name, def);
    }

    public static int getInt(String name, int def){
        return prefs.getInteger(name, def);
    }

    public static boolean getBool(String name, boolean def){
        return prefs.getBoolean(name, def);
    }

    public static long getLong(String name, long def){
        return prefs.getLong(name, def);
    }

    public static boolean has(String name){
        return prefs.contains(name);
    }

    public static void save(){
        try{
            prefs.flush();
        }catch(GdxRuntimeException e){
            if(errorHandler != null){
                if(!disabled){
                    errorHandler.run();
                }
            }else{
                throw e;
            }

            disabled = true;
        }
    }

    public static Object def(String name){
        if(!defaults.containsKey(name))
            throw new IllegalArgumentException("No setting with name \"" + name + "\" exists!");
        return defaults.get(name);
    }

    /**
     * Set up a bunch of defaults.
     * Format: name1, default1, name2, default2, etc
     */
    public static void defaultList(Object... objects){
        for(int i = 0; i < objects.length; i += 2){
            defaults((String) objects[i], objects[i + 1]);
        }
    }

    /**
     * Sets a default value up.
     * This is REQUIRED for every pref value.
     */
    public static void defaults(String name, Object object){
        defaults.put(name, object);
    }
}
