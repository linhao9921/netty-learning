package com.lh.netty.message;

/**
 * @Author haol
 * @Date 21-2-2 10:34
 * @Version 1.0
 * @Desciption 自定义消息
 */
public class Message {
    public static final byte TYPE_HEART = 1;
    public static final byte TYPE_MESSAGE = 2;

    /**表示消息的类型，有心跳类型和内容类型*/
    private byte type;

    /**表示消息的长度*/
    private int length;

    /**表示消息的内容（心跳包在这里没有内容）*/
    private String content;

    public Message() {
    }

    public Message(byte type, int length, String content) {
        this.type = type;
        this.length = length;
        this.content = content;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", length=" + length +
                ", content='" + content + '\'' +
                '}';
    }
}
