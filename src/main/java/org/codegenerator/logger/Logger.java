package org.codegenerator.logger;

import org.codegenerator.utils.Config;
import java.util.LinkedList;
import java.util.Queue;
import static org.codegenerator.utils.GUI.viewManager;

public class Logger {
    public static void showMessage(Message message){
        long messageDelay = message.delay;

        if(messageDelay == 0)
            viewManager.showMessage(message.messageType.name() + ": " + message.message, Config.PLUGIN_NAME);
        else
            new Thread(() -> {
                try {
                    Thread.sleep(messageDelay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                viewManager.showMessage(message.messageType.name() + ": " + message.message, Config.PLUGIN_NAME);
            } ).start();
    }

    private static final Queue<Message> messageQueue;

    static {
        messageQueue = new LinkedList<>();
    }

    public static void queueMessage(Message message){
        messageQueue.add(message);
    }

    public static void queueInfoMessage(String message){
        messageQueue.add(new Message(Message.MessageType.INFO, message));
    }

    public static void queueWarningMessage(String message){
        messageQueue.add(new Message(Message.MessageType.WARNING, message));
    }

    public static void queueErrorMessage(String message){
        messageQueue.add(new Message(Message.MessageType.ERROR, message));
    }

    public static void queueDebugMessage(String message){
        messageQueue.add(new Message(Message.MessageType.DEBUG, message));
    }

    public static void consumeHead(){
        Message message = messageQueue.poll();

        if(message != null)
            showMessage(message);
    }

    public static void consumeQueue(){
        Message message;

        while ((message = messageQueue.poll()) != null)
            showMessage(message);
    }

    public static void clearQueue(){
        messageQueue.clear();
    }

    public static class Message{
        public enum MessageType{INFO, WARNING, ERROR, DEBUG}

        private final MessageType messageType;

        private final String message;

        private final long delay;

        public Message(MessageType messageType, String message, long delay) {
            this.messageType = messageType;
            this.message = message;
            this.delay = delay;
        }

        public Message(MessageType messageType, String message) {
            this(messageType, message, 0);
        }
    }
}
