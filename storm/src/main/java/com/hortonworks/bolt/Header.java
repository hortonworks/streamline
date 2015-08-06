package com.hortonworks.bolt;

import com.google.common.base.Charsets;

import java.nio.ByteBuffer;

public class Header {
    private String deviceId;
    private Long version;

    public Header(String deviceId, Long version) {
        this.deviceId = deviceId;
        this.version = version;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Header{" +
                "deviceId='" + deviceId + '\'' +
                ", version=" + version +
                '}';
    }

    public static Header readHeader(ByteBuffer byteBuffer) {
        int size = byteBuffer.getInt();
        byte[] idBytes = new byte[size];

        byteBuffer.get(idBytes);
        String deviceId = new String(idBytes, Charsets.UTF_8);
        Long version = byteBuffer.getLong();
        return new Header(deviceId, version);
    }

    //writes the header to provided bytebuffer.
    public void writeHeader(ByteBuffer byteBuffer) {
        byte[] bytes = deviceId.getBytes();
        int idSize = bytes.length;

        byteBuffer.putInt(idSize);
        byteBuffer.put(bytes);
        byteBuffer.putLong(version);
    }

    public int size() {
        byte[] bytes = deviceId.getBytes();
        int idSize = bytes.length;
        //deviceIdSize + 4 bytes as we write deviceIdSize + 8 as version is a long.
        return idSize + 4 + 8;
    }
}
