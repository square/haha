package org.eclipse.mat.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * @since 0.8
 */
public final class MessageUtil {

    private static final MessageUtil instance = new MessageUtil();

    private final Properties props = new Properties();

    public static String format(String message, Object... objects) {
        String pMsg = (String) instance.props.get(message);
        if (pMsg != null) {
            return MessageFormat.format(pMsg, objects);
        }
        return message;
    }

    private MessageUtil() {
        try {
            InputStream inputStream = MessageUtil.class.getClassLoader().getResourceAsStream("messages.properties");
            props.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("MessageUtil initialization failed.", e);
        }
    }
}
