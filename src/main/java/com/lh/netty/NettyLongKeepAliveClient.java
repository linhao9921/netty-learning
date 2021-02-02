package com.lh.netty;

import com.lh.netty.codec.LongKeepAliveDecoder;
import com.lh.netty.codec.LongKeepAliveEncoder;
import com.lh.netty.message.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author haol
 * @Date 21-2-2 11:46
 * @Version 1.0
 * @Desciption 基于Netty的长连接客户端
 */
public class NettyLongKeepAliveClient {

    public static void main(String[] args) {
        // 创建一个接收事件loop组用于注册channel
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();

            // 注册事件loop组
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new LongKeepAliveClientInitializer());
            // 绑定端口并启动
            ChannelFuture f = bootstrap.connect(InetAddress.getLocalHost(), 9002).sync();

            AtomicBoolean end = new AtomicBoolean(false);
            Thread heartThread = new Thread(() -> {
                System.out.println("Start heart send....");
                while (!end.get()) {
                    // 发送心跳
                    Message message = new Message(Message.TYPE_HEART, 0, null);
                    f.channel().writeAndFlush(message);

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Stop heart send....");
            });
            heartThread.setDaemon(true);
            heartThread.start();

            // 控制台输入数据
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String text = scanner.next();
                if ("quit".equalsIgnoreCase(text) || "exit".equalsIgnoreCase(text)) {
                    end.set(true);
                    break;
                }

                if (text.length() > 0) {
                    // 发送数据
                    Message message = new Message(Message.TYPE_MESSAGE, text.getBytes().length, text);
                    f.channel().writeAndFlush(message);
                }
            }
            System.out.println("Stop client....");

            f.channel().closeFuture().sync();
        } catch (InterruptedException | UnknownHostException e) {
            e.printStackTrace();
        } finally {
            try {
                // 正常关闭
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始化
     */
    private static class LongKeepAliveClientInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                    .addLast(new LongKeepAliveEncoder())        // 自定义消息编码器
                    .addLast(new LongKeepAliveDecoder())        // 自定义消息解码器
                    .addLast(new LongKeepAliveHandler());       // 消息处理器
        }
    }

    /**
     * 自定义消息处理器
     */
    private static class LongKeepAliveHandler extends SimpleChannelInboundHandler<Message> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
            if (!"PONG".equalsIgnoreCase(msg.getContent())) {
                System.out.println("---------------------------------------------------------------------------------------------");
                System.out.println("Msg from server: " + msg + ".");
                System.out.println("---------------------------------------------------------------------------------------------");
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            super.channelReadComplete(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("Exception caught...");
            if(null != cause) cause.printStackTrace();
            if(null != ctx) ctx.close();
        }
    }
}
