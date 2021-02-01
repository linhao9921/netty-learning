package com.lh.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author haol
 * @Date 21-1-29 16:09
 * @Version 1.0
 * @Desciption NIO模型的服务端
 */
public class NioServer {

    public static void main(String[] args) throws IOException {
        // 打开channel通道
        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);   // 配置非阻塞
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(new InetSocketAddress(9001));

        // 打开selector选择器
        Selector selector = Selector.open();

        // channel通道注册elector选择器(注册模式为接受请求)
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            try {
                // 阻塞等待
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Set<SelectionKey> selectorKeys = selector.selectedKeys();
            Iterator<SelectionKey> readIt = selectorKeys.iterator();
            while (readIt.hasNext()) {
                SelectionKey selectionKey = readIt.next();
                readIt.remove();

                try {
                    // 判断是否是可接受的socket连接
                    if (selectionKey.isAcceptable()) {
                        System.out.println("Handle socket accept from client~~~~~~~~~~~~~~~~");
                        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();

                        SocketChannel client = server.accept();
                        client.configureBlocking(false);    // 配置非阻塞
                        // 客户端channel通道注册elector选择器(注册模式为写)
                        client.register(selector, SelectionKey.OP_WRITE);
                    }


                    // 判断是否是可写的socket连接
                    if (selectionKey.isWritable()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();

                        // 处理读取客户端的数据
                        System.out.println("Handle socket read from client~~~~~~~~~~~~~~~~");
                        ByteBuffer read = ByteBuffer.allocate(1024);
                        int len = client.read(read);
                        if (len > 0) {
                            byte[] data = read.array();
                            data = Arrays.copyOf(data, len);
                            String msg = new String(data, Charset.forName("UTF-8"));
                            System.out.println("Msg from client:" + msg);
                        } else {
                            System.out.println("Msg from client: null!!!");
                        }

                        // 处理响应客户端
                        System.out.println("Handle socket write to client~~~~~~~~~~~~~~~~");
                        client.write(ByteBuffer.wrap(("Hi, I'm nio server, " + serverSocket.getInetAddress().getHostName() + "!!!!").getBytes()));
                        client.close();
                    }
                } catch (IOException ex) {
                    selectionKey.cancel();
                    try {
                        selectionKey.channel().close();
                    } catch (IOException cex) {
                        // 在关闭时忽略
                    }
                }
            }

        }
    }
}
