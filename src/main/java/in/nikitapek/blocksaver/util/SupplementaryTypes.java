package in.nikitapek.blocksaver.util;

import com.google.gson.reflect.TypeToken;
import in.nikitapek.blocksaver.serialization.Reinforcement;
import org.bukkit.Location;
import org.bukkit.Material;

import java.lang.reflect.Type;
import java.util.List;
import java.util.TreeSet;

public final class SupplementaryTypes {
    @SuppressWarnings("rawtypes")
    public static final Type TREESET = new TypeToken<TreeSet>() {}.getType();

    public static final Type INTEGER = new TypeToken<Integer>() {}.getType();
    public static final Type MATERIAL = new TypeToken<Material>() {}.getType();
    public static final Type REINFORCEMENT = new TypeToken<Reinforcement>() {}.getType();
    public static final Type LOCATION = new TypeToken<Location>() {}.getType();
    public static final Type LIST = new TypeToken<List<?>>() {}.getType();

    private SupplementaryTypes() {}
}
