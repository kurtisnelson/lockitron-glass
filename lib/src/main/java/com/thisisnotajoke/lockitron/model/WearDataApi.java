package com.thisisnotajoke.lockitron.model;

public class WearDataApi {
    private static final byte[] onePayload = new byte[]{0x1};
    private static final byte[] zeroPayload = new byte[]{0x0};

    public static final String LOCK_ITEM_PATH = "/wearatron/selected_lock";
    public static final String LOCK_ITEM_KEY = "LockJson";

    public static final String ACTION_PATH = "/wearatron/action";
    public static final byte[] ACTION_LOCK_PAYLOAD = onePayload;
    public static final byte[] ACTION_UNLOCK_PAYLOAD = zeroPayload;
}
