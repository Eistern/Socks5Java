package net.fit;

import java.io.IOException;
import java.util.Properties;

public class SystemProperties {
    public static void load(String path) throws IOException {
        Properties properties = new Properties();
        properties.load(Main.class.getResourceAsStream(path));
        properties.forEach((key, value) -> System.setProperty((String) key, (String) value));
    }
}
