package com.hortonworks.iotas.notification.store.hbase;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Collections;
import java.util.List;

/**
 * For serializing and de-serializing the values in the notification
 * fieldsAndValues for storing in hbase.
 */
public class Serializer {
    private final Kryo kryo;
    private final Output output;

    /**
     * Constructs a {@link Serializer} instance with the given list
     * of classes registered in kryo.
     *
     * @param classesToRegister the classes to register.
     */
    public Serializer(List<Class<?>> classesToRegister) {
        kryo = new Kryo();
        output = new Output(2000, 2000000000);
        for (Class<?> klazz : classesToRegister) {
            kryo.register(klazz);
        }
    }

    public Serializer() {
        this(Collections.<Class<?>>emptyList());
    }

    public byte[] serialize(Object obj) {
        output.clear();
        kryo.writeClassAndObject(output, obj);
        return output.toBytes();
    }

    public Object deserialize(byte[] b) {
        Input input = new Input(b);
        return kryo.readClassAndObject(input);
    }
}
