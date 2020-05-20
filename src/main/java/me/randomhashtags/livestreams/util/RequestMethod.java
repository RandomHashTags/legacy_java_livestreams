package me.randomhashtags.livestreams.util;

public enum RequestMethod {
    DELETE,
    GET,
    PATCH,
    POST,
    ;

    private String name;

    RequestMethod() {
        name = name();
    }

    public String getName() {
        return name;
    }
}
