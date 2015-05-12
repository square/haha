package org.eclipse.mat.util;

import java.text.MessageFormat;
import org.eclipse.mat.hprof.Messages;

public final class MessageUtil {

  public static String format(Messages message, Object... objects) {
    if (objects.length == 0) {
      return message.pattern;
    }
    return MessageFormat.format(message.pattern, objects);
  }

  private MessageUtil() {
    throw new AssertionError();
  }
}
