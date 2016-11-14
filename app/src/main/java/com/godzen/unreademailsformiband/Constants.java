package com.godzen.unreademailsformiband;

import java.util.UUID;

/**
 * Created by godzen on 14/11/16.
 */

public class Constants {

    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb";

    //services
    public static final UUID UUID_SERVICE_1802 = UUID.fromString(String.format(BASE_UUID, "1802"));
    public static final UUID UUID_SERVICE_1811 = UUID.fromString(String.format(BASE_UUID, "1811"));

    //
    public static final UUID UUID_CHARACTERISTIC_2A46 = UUID.fromString(String.format(BASE_UUID, "2A46"));
    public static final UUID UUID_CHARACTERISTIC_2A06 = UUID.fromString(String.format(BASE_UUID, "2A06"));


    public static final String INTENT_NOTIFY_GMAIL = "com.godzen.notifyGmail";
}
