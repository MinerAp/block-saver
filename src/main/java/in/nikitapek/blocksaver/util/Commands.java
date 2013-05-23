package in.nikitapek.blocksaver.util;

import com.amshulman.mbapi.util.PermissionsEnum;

public enum Commands implements PermissionsEnum {
    BLOCKSAVER, FEEDBACK;

    @Override
    public String getPrefix() {
        return "blocksaver.";
    }
}
