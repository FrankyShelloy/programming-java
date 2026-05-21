package chat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class XmlProtocol {

    public static void writeMessage(OutputStream os, String xmlContent) throws Exception {
        DataOutputStream dos = new DataOutputStream(os);
        byte[] bytes = xmlContent.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    public static String readMessage(InputStream is) throws Exception {
        DataInputStream dis = new DataInputStream(is);
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}