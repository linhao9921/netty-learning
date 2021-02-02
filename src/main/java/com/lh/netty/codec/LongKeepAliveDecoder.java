package com.lh.netty.codec;

import com.lh.netty.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @Author haol
 * @Date 21-2-2 14:47
 * @Version 1.0
 * @Desciption 自定义消息解码器
 */
public class LongKeepAliveDecoder extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte type = in.readByte();          // 读取消息类型
        int length = in.readInt();          // 读取消息内容长度
        Message message = new Message();
        message.setType(type);
        message.setLength(length);
        if (length > 0) {                   // 根据消息长度，读取消息内容
            byte[] bytes = new byte[length];
            in.readBytes(bytes);
            String content = new String(bytes);
            message.setContent(content);
        }

        out.add(message);
    }
}
