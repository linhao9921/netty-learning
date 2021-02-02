package com.lh.netty;

import com.lh.netty.codec.LongKeepAliveDecoder;
import com.lh.netty.codec.LongKeepAliveEncoder;
import com.lh.netty.message.Message;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ScheduledFuture;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author haol
 * @Date 21-2-2 10:25
 * @Version 1.0
 * @Desciption 基于Netty的长连接服务端
 */
public class NettyLongKeepAliveServer {
    private static InetSocketAddress httpSocketAddress = new InetSocketAddress(9002);


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
                    .localAddress(httpSocketAddress)
                    .childHandler(new LongKeepAliveServerInitializer());
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

    /**
     * 初始化处理器
     */
    private static class LongKeepAliveServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                    .addLast(new LongKeepAliveDecoder())        // 自定义消息解码器
                    .addLast(new LongKeepAliveEncoder())        // 自定义消息编码器
                    .addLast(new LongKeepAliveHandler());       // 消息处理器
        }
    }

    /**
     * 自定义消息处理器
     */
    private static class LongKeepAliveHandler extends SimpleChannelInboundHandler<Message> {
        private static Map<Integer, LongKeepAliveChannelCache> cache = new ConcurrentHashMap<>();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
            Channel channel = ctx.channel();
            final int hashCode = channel.hashCode();

            System.out.println("---------------------------------------------------------------------------------------------");
            System.out.println(String.format("Channel hash code: %d, msg: %s, cache size: %d.", hashCode, msg, cache.size()));

            if (!cache.containsKey(hashCode)) {
                System.out.println("Add channel cache, hash code: " + hashCode + ".");

                // 增加监听
                channel.closeFuture().addListener(listener -> {
                    System.out.println("Channel close, hash code: " + hashCode + ".");
                    cache.remove(hashCode);
                });

                // 执行关闭操作(10后关闭)
                ScheduledFuture<?> scheduleFeature = ctx.executor().schedule(() -> {
                    System.out.println("Schedule execute, hash code: " + hashCode + ". Close this channel.");
                    channel.close();
                }, 5, TimeUnit.SECONDS);

                // 放入缓存中
                cache.put(hashCode, new LongKeepAliveChannelCache(channel, scheduleFeature));
            }

            // 处理消息
            LongKeepAliveChannelCache longKeepAliveChannelCache = cache.get(hashCode);
            switch (msg.getType()) {
                // 心跳
                case Message.TYPE_HEART: {
                    ScheduledFuture scheduledFuture = ctx.executor().schedule(()-> {
                        System.out.println("Schedule execute(2), hash code: " + hashCode + ". Close this channel.");
                        channel.close();
                    }, 5, TimeUnit.SECONDS);

                    System.out.println("Schedule refresh by heart, hash code: " + hashCode + ".");
                    // 去掉关闭channel通道关闭调度任务
                    longKeepAliveChannelCache.getScheduleFeature().cancel(true);

                    // 更新调度器
                    longKeepAliveChannelCache.setScheduleFeature(scheduledFuture);

                    // 响应心跳信息
                    ctx.channel().writeAndFlush(new Message(Message.TYPE_MESSAGE, "PONG".getBytes().length, "PONG"));
                    break;
                }
                // 消息
                case Message.TYPE_MESSAGE: {
                    // 响应数据
                    String content = "Hi, I am netty long alive server! Server address: " + longKeepAliveChannelCache.getChannel().localAddress();
                    longKeepAliveChannelCache.getChannel().writeAndFlush(new Message(Message.TYPE_MESSAGE, content.getBytes().length, content));
                    break;
                }
            }

            System.out.println("---------------------------------------------------------------------------------------------");
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

    /**
     * channel
     */
    private static class LongKeepAliveChannelCache {
        private final Channel channel;
        private ScheduledFuture<?> scheduleFeature;

        LongKeepAliveChannelCache(Channel channel, ScheduledFuture<?> scheduleFeature) {
            this.channel = channel;
            this.scheduleFeature = scheduleFeature;
        }

        Channel getChannel() {
            return channel;
        }

        ScheduledFuture<?> getScheduleFeature() {
            return scheduleFeature;
        }

        void setScheduleFeature(ScheduledFuture<?> scheduleFeature) {
            this.scheduleFeature = scheduleFeature;
        }
    }
}
