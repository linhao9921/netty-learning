package com.lh.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @Author haol
 * @Date 21-1-29 15:24
 * @Version 1.0
 * @Desciption BIO模型的服务端
 */
public class BioServer {

    public static void main(String[] args) throws IOException {
        final ServerSocket serverSocket = new ServerSocket(9000);

        while (true) {
            // 接受客户端的socket请求
            final Socket socket = serverSocket.accept();
            System.out.println("Accept socket from client~~~~~~~~~~~~~~~~");

            new Thread(()->{
                InputStream in;
                OutputStream out;
                try {
                    // 读取请求
                    byte[] data = new byte[8129];
                    in = socket.getInputStream();
                    int len = in.read(data);
                    System.out.println("Handle socket from client~~~~~~~~~~~~~~~~");
                    if (len > 0) {
                        data = Arrays.copyOf(data, len);
                        String msg = new String(data, Charset.forName("UTF-8"));
                        System.out.println("Msg from client:" + msg);
                    } else {
                        System.out.println("Msg from client: null!!!");
                    }

                    // 发起响应
                    out = socket.getOutputStream();
                    out.write(("Hi, I'm bio server, " + serverSocket.getInetAddress().getHostName() + "!!!!").getBytes());
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
