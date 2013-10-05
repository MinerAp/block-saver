package in.nikitapek.blocksaver.serialization;

import in.nikitapek.blocksaver.util.SupplementaryTypes;

import java.lang.reflect.Type;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.amshulman.typesafety.gson.GsonTypeAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

public final class ReinforcementTypeAdapter implements GsonTypeAdapter<Reinforcement> {
    private static final Type TYPE = SupplementaryTypes.REINFORCEMENT;

    public static String currentWorld;

    @Override
    public JsonElement serialize(final Reinforcement src, final Type typeOfSrc, final JsonSerializationContext context) {

        return null;
    }

    @Override
    public Reinforcement deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {

        return null;
    }

    @Override
    public Type getType() {
        return TYPE;
    }
}
