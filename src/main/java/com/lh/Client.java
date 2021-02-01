package com.lh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @Author haol
 * @Date 21-1-29 16:05
 * @Version 1.0
 * @Desciption BIO模型的客户端
 */
public class Client {

    public static void main(String[] args) throws IOException {
        final Socket clientSocket = new Socket(InetAddress.getLocalHost(), 9002);

        InputStream in;
        OutputStream out;
        try {
            // 发起请求
            out = clientSocket.getOutputStream();
            out.write(("Hi, I'm client!!!!").getBytes());
            out.flush();

            // 读取响应
            byte[] data = new byte[8129];
            in = clientSocket.getInputStream();
            int len = in.read(data);
            System.out.println("Handle socket from server~~~~~~~~~~~~~~~~");
            if (len > 0) {
                data = Arrays.copyOf(data, len);
                System.out.println("Msg from server:" + new String(data, Charset.forName("UTF-8")));
            } else {
                System.out.println("Msg from server: null!!!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
