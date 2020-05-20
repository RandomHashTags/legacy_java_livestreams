package me.randomhashtags.livestreams.util;

public interface CompletionHandler {
    void handle(Object object);
    default void handleThree(Object object1, Object object2, Object object3) {}
}
