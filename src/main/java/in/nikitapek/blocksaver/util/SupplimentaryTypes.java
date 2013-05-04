package in.nikitapek.blocksaver.util;

import java.lang.reflect.Type;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.google.gson.reflect.TypeToken;

public final class SupplimentaryTypes {
    private SupplimentaryTypes() {}

    public static final Type BLOCK = new TypeToken<Block>() {}.getType();
    public static final Type BYTE = new TypeToken<Byte>() {}.getType();
    //public static final Type BOOLEAN = new TypeToken<Boolean>() {}.getType();
    public static final Type MATERIAL = new TypeToken<Material>() {}.getType();
}
