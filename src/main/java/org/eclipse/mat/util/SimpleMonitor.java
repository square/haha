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

public class SimpleMonitor
{
    String task;
    IProgressListener delegate;
    int currentMonitor;
    int[] percentages;

    public SimpleMonitor(String task, IProgressListener monitor, int[] percentages)
    {
        this.task = task;
        this.delegate = monitor;
        this.percentages = percentages;
    }

    public IProgressListener nextMonitor()
    {
        if (currentMonitor == 0)
        {
            int total = 0;
            for (int ii : percentages)
                total += ii;
            delegate.beginTask(task, total);
        }

        return new Listener(percentages[currentMonitor++]);
    }

    public class Listener implements IProgressListener
    {
        long counter;

        int majorUnits;
        int unitsReported;
        long workDone;
        long workPerUnit;

        boolean isSmaller;

        public Listener(int majorUnits)
        {
            this.majorUnits = majorUnits;
        }

        public void beginTask(String name, int totalWork)
        {
            if (name != null)
                delegate.subTask(name);

            if (totalWork == 0)
                return;

            isSmaller = totalWork < majorUnits;
            workPerUnit = isSmaller ? majorUnits / totalWork : totalWork / majorUnits;
            unitsReported = 0;
        }

        public void subTask(String name)
        {
            delegate.subTask(name);
        }

        public void done()
        {
            if (majorUnits - unitsReported > 0)
                delegate.worked(majorUnits - unitsReported);
        }

        public boolean isCanceled()
        {
            return delegate.isCanceled();
        }

        public boolean isProbablyCanceled()
        {
            return counter++ % 5000 == 0 ? isCanceled() : false;
        }

        public void totalWorkDone(long work)
        {
            if (workDone == work)
                return;

            if (workPerUnit == 0)
                return;

            workDone = work;
            int unitsWorked = isSmaller ? (int) (work * workPerUnit) : (int) (work / workPerUnit);
            int unitsToReport = unitsWorked - unitsReported;

            if (unitsToReport > 0)
            {
                delegate.worked(unitsToReport);
                unitsReported += unitsToReport;
            }
        }

        public void worked(int work)
        {
            totalWorkDone(workDone + work);
        }

        public void setCanceled(boolean value)
        {
            delegate.setCanceled(value);
        }

        public void sendUserMessage(Severity severity, String message, Throwable exception)
        {
            delegate.sendUserMessage(severity, message, exception);
        }

        public long getWorkDone()
        {
            return workDone;
        }

    }

}
