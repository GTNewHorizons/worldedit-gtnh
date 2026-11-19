package com.sk89q.worldedit.forge;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * FML calls event handlers from generated classes in another class loader, which can only reach public
 * classes and methods. Anything else crashes the server on the first event.
 */
public class EventSubscribersArePublicTest {

    @Test
    public void everyClassWithAnEventHandlerIsPublic() throws Exception {
        List<String> problems = new ArrayList<String>();
        for (Class<?> type : classesUnder(Paths.get("src/main/java/com/sk89q/worldedit/forge"))) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(SubscribeEvent.class)) {
                    continue;
                }
                if (!Modifier.isPublic(type.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
                    problems.add(type.getName() + "." + method.getName());
                }
            }
        }
        assertTrue("event handlers that FML can't call: " + problems, problems.isEmpty());
    }

    private static List<Class<?>> classesUnder(Path dir) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName()
                    .toString();
                if (name.endsWith(".java")) {
                    String className = "com.sk89q.worldedit.forge." + name.substring(0, name.length() - 5);
                    try {
                        classes
                            .add(Class.forName(className, false, EventSubscribersArePublicTest.class.getClassLoader()));
                    } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
                        // Client-only classes can't load in tests.
                    }
                }
            }
        }
        return classes;
    }
}
