package com.mobwal.walker.beacon;

public class Names {
    public static String BASE_URL = "http://10.10.6.76:5001";
    public static String VIRTUAL_DIR_PATH = "/walker/dev";

    public static String getConnectUrl() {
        return BASE_URL + VIRTUAL_DIR_PATH;
    }
}
