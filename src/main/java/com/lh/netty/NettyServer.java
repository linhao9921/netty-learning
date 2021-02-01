package com.lh.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * @Author haol
 * @Date 21-1-29 16:58
 * @Version 1.0
 * @Desciption Netty模型的服务端
 */
public class NettyServer {
    private static InetSocketAddress socketAddress  = new InetSocketAddress(9002);

    static final ByteBuf data = Unpooled.unreleasableBuffer(Unpooled
            .copiedBuffer(("Hi, I'm netty nio server, " + socketAddress.getHostName() + "!!!!").getBytes()));

    public static void main(String[] args) {

        // 创建一个接收事件loop组用于注册channel
        EventLoopGroup acceptEventLoopGroup = new NioEventLoopGroup();
        // 创建一个处理事件loop组用于注册channel
        EventLoopGroup handleEventLoopGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            // 注册事件loop组
            bootstrap.group(acceptEventLoopGroup, handleEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(socketAddress)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ServerChannelHandlerAdapter());        // 处理器
                        }
                    });
            // 绑定端口并启动
            ChannelFuture f = bootstrap.bind().sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                // 正常关闭
                acceptEventLoopGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                // 正常关闭
                handleEventLoopGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class ServerChannelHandlerAdapter extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // super.channelActive(ctx);

            // 处理新的连接请求
            System.out.println("Accept socket connection from client~~~~~~~~~~~~~~~~" + ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // super.channelRead(ctx, msg);

            // 处理读取客户端的请求
            System.out.println("Handle socket read from client~~~~~~~~~~~~~~~~");
            System.out.println("Msg from client: "+ ((ByteBuf)msg).toString(Charset.defaultCharset()));
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            // ctx.fireChannelReadComplete();();

            // 处理响应客户端
            System.out.println("Handle socket write to client~~~~~~~~~~~~~~~~");
            System.out.println("Msg to client: "+ data.toString(Charset.defaultCharset()));
            ctx.writeAndFlush(data.duplicate())
                    .addListener(ChannelFutureListener.CLOSE);      // 注册关闭事件（发送响应数据完毕后会关闭channel通道）
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // super.exceptionCaught(ctx, cause);

            cause.printStackTrace();
            ctx.close();
        }
    }
}
