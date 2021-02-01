package com.lh.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * @Author haol
 * @Date 21-2-1 11:01
 * @Version 1.0
 * @Desciption 基于Netty的模型http web的服务端
 */
public class NettyHttpWebServer {
    private static InetSocketAddress httpSocketAddress  = new InetSocketAddress(9002);

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
                    .childHandler(new HttpServerInitializer());
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
     * 定义一个服务初始化类
     */
    private static class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                    .addLast(new HttpServerCodec())                                 // Http消息解码器
                    .addLast(new HttpObjectAggregator(512 * 1024))  // Http消息聚合器
                    .addLast(new HttpRequestHandler());                             // Http请求处理器
        }
    }

    /**
     * Http请求处理器
     */
    private static class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            // flush缓冲区
            ctx.flush();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            // 处理请求
            if (is100ContinueExpected(request)) {
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1
                        , HttpResponseStatus.CONTINUE));
            }
            
            // 获取url
            Map<String, Set<String>> params = new HashMap<>();
            String uri = request.uri();
            int i;
            if ((i = uri.indexOf("?")) > -1) {
                // 获取参数并解析
                String param = uri.substring(i + 1);
                if (param.length() > 0) {
                    String[] kvs = param.split("&");
                    for (String kv : kvs) {
                        if (kv != null && kv.length() > 0) {
                            String[] kvItem = kv.split("=");
                            if (kvItem.length == 2) {
                                if (params.containsKey(kvItem[0])) {
                                    params.get(kvItem[0]).add(kvItem[1]);
                                } else {
                                    params.put(kvItem[0], new HashSet<>(Collections.singletonList(kvItem[1])));
                                }
                            }
                        }
                    }
                }

                // 去掉参数部分
                uri = uri.substring(0, i);
            }

            // 创建http响应
            FullHttpResponse response;
            Set<String> type;
            if ((type = params.get("type")) != null && type.contains("json")) {
                String msg = "{\"uri\": \"" + uri + "\", \"method\": \"" + request.method().name() + "\"}\r\n";

                // 创建http响应
                response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
                // 设置头信息
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            } else {
                String msg = "<!DOCTYPE html>\r\n" +
                            "<html>\r\n" +
                            "   <head>\r\n" +
                            "       <title>Netty Http Web Server</title>\r\n" +
                            "   </head>\r\n" +
                            "   <body>你请求uri为：" + uri + ", 你请求的方法是：" + request.method().name() + "</body>\r\n" +
                            "</html>\r\n";

                // 创建http响应
                response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
                // 设置头信息
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            }


            // 响应数据
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }


        private boolean is100ContinueExpected(FullHttpRequest request) {
            return false;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();

            // 创建http响应
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Unpooled.copiedBuffer(cause.getMessage(), CharsetUtil.UTF_8));
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
