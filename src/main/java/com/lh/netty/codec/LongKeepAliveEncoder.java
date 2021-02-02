package com.lh.netty.codec;

import com.lh.netty.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @Author haol
 * @Date 21-2-2 14:49
 * @Version 1.0
 * @Desciption 自定义消息编码器
 */
public class LongKeepAliveEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        out.writeByte(msg.getType());                    // 写消息类型
        out.writeInt(msg.getLength());                   // 写消息体长度
        if (msg.getContent() != null) {                  // 写消息内容
            out.writeBytes(msg.getContent().getBytes());
        }
    }
}
