package in.nikitapek.blocksaver.util;

import in.nikitapek.blocksaver.serialization.Reinforcement;

import java.lang.reflect.Type;
import java.util.TreeSet;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.google.gson.reflect.TypeToken;

public final class SupplimentaryTypes {
    private SupplimentaryTypes() {}

    @SuppressWarnings("rawtypes")
    public static final Type TREESET = new TypeToken<TreeSet>() {}.getType();

    public static final Type BLOCK = new TypeToken<Block>() {}.getType();
    public static final Type BYTE = new TypeToken<Byte>() {}.getType();
    public static final Type MATERIAL = new TypeToken<Material>() {}.getType();
    public static final Type REINFORCEMENT = new TypeToken<Reinforcement>() {}.getType();
}
