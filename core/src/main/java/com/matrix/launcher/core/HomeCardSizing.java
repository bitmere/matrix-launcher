package com.matrix.launcher.core;

/** Content sizing is independent of installed-app pickers and the user's dock. */
public final class HomeCardSizing {
    public static final int APP_COLUMNS = 5;
    public static int dockColumns(int apps){return Math.max(4,Math.min(5,apps));}
    /** Pixel-exact fit inside a measured row; reserve gaps only between cards. */
    public static int slotWidth(int available,int count,int gap) {
        if(count<=0)return 0;
        return Math.max(1,(available-Math.max(0,gap)*(count-1))/count);
    }
    public static int contentColumns(int preference) { return Math.max(3, Math.min(6, preference)); }
}
