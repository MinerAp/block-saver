package in.nikitapek.blocksaver.util;

import com.amshulman.mbapi.util.PermissionsEnum;

public enum Commands implements PermissionsEnum {
    BLOCKSAVER;

    @Override
    public String getPrefix() {
        return "blocksaver.";
    }
}
