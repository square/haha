package org.eclipse.mat.util;

import java.text.MessageFormat;

/**
 * @since 0.8
 */
public final class MessageUtil
{
    public static String format(String message, Object... objects)
    {
        return MessageFormat.format(message, objects);
    }

    private MessageUtil()
    {}
}
