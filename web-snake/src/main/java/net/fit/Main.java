package net.fit;

import net.fit.proto.SnakesProto;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.load(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("application.properties")));
        Properties props_bounds = new Properties();
        props_bounds.load(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("bounds.properties")));


        SnakesProto.GameConfig.Builder builder = SnakesProto.GameConfig.newBuilder();
        props.forEach((st, val) -> {
            if (st instanceof String && val instanceof String) {
                String key = (String) st;
                double value = Double.parseDouble((String) val);
                double maxValue = Double.parseDouble(props_bounds.getProperty(key + "_max"));
                double minValue = Double.parseDouble(props_bounds.getProperty(key + "_min"));
                if (value >= minValue && value <= maxValue) {
                    try {
                        if (key.equals("FoodPerPlayer") || key.equals("DeadFoodProb")) {
                            builder.getClass().getDeclaredMethod("set" + key, float.class).invoke(builder, (float) value);
                        } else {
                            builder.getClass().getDeclaredMethod("set" + key, int.class).invoke(builder, (int) value);
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        System.out.println("Test");
    }
}
