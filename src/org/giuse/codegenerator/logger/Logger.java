package org.giuse.codegenerator.logger;

import org.giuse.codegenerator.utils.Config;
import java.util.LinkedList;
import java.util.Queue;
import static org.giuse.codegenerator.utils.GUI.viewManager;

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

    private final Queue<Message> messageQueue;

    public Logger(){
        this.messageQueue = new LinkedList<>();
    }

    public void queueMessage(Message message){
        this.messageQueue.add(message);
    }

    public void queueInfoMessage(String message){
        this.messageQueue.add(new Message(Message.MessageType.INFO, message));
    }

    public void queueWarningMessage(String message){
        this.messageQueue.add(new Message(Message.MessageType.WARNING, message));
    }

    public void queueErrorMessage(String message){
        this.messageQueue.add(new Message(Message.MessageType.ERROR, message));
    }

    public void queueDebugMessage(String message){
        this.messageQueue.add(new Message(Message.MessageType.DEBUG, message));
    }

    public void consumeHead(){
        Message message = this.messageQueue.poll();

        if(message != null)
            showMessage(message);
    }

    public void consumeQueue(){
        Message message;

        while ((message = this.messageQueue.poll()) != null)
            showMessage(message);
    }

    public void clearQueue(){
        this.messageQueue.clear();
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
