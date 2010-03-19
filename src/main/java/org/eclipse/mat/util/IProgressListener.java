/*******************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.util;

/**
 * This interface is closely modeled after
 * {@link org.eclipse.core.runtime.IProgressMonitor}. The latter has not been
 * used to avoid a dependency. It is implemented by objects that monitor the
 * progress of an activity; the methods in this interface are invoked by code
 * that performs the activity.
 * <p>
 * All activity is broken down into a linear sequence of tasks against which
 * progress is reported. When a task begins, a <code>beginTask(String, int)
 * </code> notification is reported,
 * followed by any number and mixture of progress reports (<code>worked()</code>
 * ) and subtask notifications (<code>subTask(String)</code>). When the task is
 * eventually completed, a <code>done()</code> notification is reported. After
 * the <code>done()</code> notification, the progress monitor cannot be reused;
 * i.e., <code>
 * beginTask(String, int)</code> cannot be called again after the call to
 * <code>done()</code>.
 * <p>
 * A request to cancel an operation can be signaled using the
 * <code>setCanceled</code> method. Operations taking a progress monitor are
 * expected to poll the monitor (using <code>isCanceled</code>) periodically and
 * abort at their earliest convenience. Operation can however choose to ignore
 * cancellation requests.
 * <p>
 * Since notification is synchronous with the activity itself, the listener
 * should provide a fast and robust implementation. If the handling of
 * notifications would involve blocking operations, or operations which might
 * throw uncaught exceptions, the notifications should be queued, and the actual
 * processing deferred (or perhaps delegated to a separate thread).
 */
public interface IProgressListener
{
    /**
     * Helper exception class which can be used to exit an operation via an
     * exception if the operation itself has been canceled. This exception is
     * not thrown by the listener itself - this has to be done by the
     * implementation using it. It is only available here for convenience
     * reasons to get rid of the various CanceledException throughout the code.
     */
    public class OperationCanceledException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Constant which may be used to indicate that the total amount of work
     * units is not known.
     */
    public final static int UNKNOWN_TOTAL_WORK = -1;

    /**
     * Notifies that the main task is beginning. This must only be called once
     * on a given progress monitor instance.
     * 
     * @param name
     *            the name (or description) of the main task
     * @param totalWork
     *            the total number of work units into which the main task is
     *            been subdivided. If the value is <code>UNKNOWN</code> the
     *            implementation is free to indicate progress in a way which
     *            doesn't require the total number of work units in advance.
     */
    public void beginTask(String name, int totalWork);

    /**
     * Notifies that the work is done; that is, either the main task is
     * completed or the user canceled it. This method may be called more than
     * once (implementations should be prepared to handle this case).
     */
    public void done();

    /**
     * Returns whether cancelation of current operation has been requested.
     * Long-running operations should poll to see if cancelation has been
     * requested.
     * 
     * @return <code>true</code> if cancellation has been requested, and
     *         <code>false</code> otherwise
     * @see #setCanceled(boolean)
     */
    public boolean isCanceled();

    /**
     * Sets the cancel state to the given value.
     * 
     * @param value
     *            <code>true</code> indicates that cancelation has been
     *            requested (but not necessarily acknowledged);
     *            <code>false</code> clears this flag
     * @see #isCanceled()
     */
    public void setCanceled(boolean value);

    /**
     * Notifies that a subtask of the main task is beginning. Subtasks are
     * optional; the main task might not have subtasks.
     * 
     * @param name
     *            the name (or description) of the subtask
     */
    public void subTask(String name);

    /**
     * Notifies that a given number of work unit of the main task has been
     * completed. Note that this amount represents an installment, as opposed to
     * a cumulative amount of work done to date.
     * 
     * @param work
     *            the number of work units just completed
     */
    public void worked(int work);

    /**
     * Defines the severites possible for a user message.
     * <ul>
     * <li><code>ERROR</code> - a serious error (most severe)</li> <li><code>
     * WARNING</code> - a warning (less severe)</li> <li><code>INFO</code> - an
     * informational ("fyi") message (least severe)</li>
     * </ul>
     * <p>
     */
    public enum Severity
    {
        /** Severity indicating this user message is informational only. */
        INFO,
        /** Severity indicating this user message represents a warning. */
        WARNING,
        /** Severity indicating this user message represents an error. */
        ERROR
    }

    /**
     * Sends a message to the user.
     * 
     * @param severity
     *            Severity as defined in {@link Severity}
     * @param message
     *            The message localized to the current locale.
     * @param exception
     *            The relevant low-level exception, or <code>null</code> if
     *            none.
     */
    public void sendUserMessage(Severity severity, String message, Throwable exception);

}
