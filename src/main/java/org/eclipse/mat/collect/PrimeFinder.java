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
package org.eclipse.mat.collect;

/* package */class PrimeFinder
{
    public static int findNextPrime(int floor)
    {
        boolean isPrime = false;
        while (!isPrime)
        {
            floor++;
            isPrime = true;
            int sqrt = (int) Math.sqrt(floor);
            for (int i = 2; i <= sqrt; i++)
            {
                if ((floor / i * i) == floor)
                {
                    isPrime = false;
                }
            }
        }
        return floor;
    }

    public static int findPrevPrime(int ceil)
    {
        boolean isPrime = false;
        while (!isPrime)
        {
            ceil--;
            isPrime = true;
            int sqrt = (int) Math.sqrt(ceil);
            for (int i = 2; i <= sqrt; i++)
            {
                if ((ceil / i * i) == ceil)
                {
                    isPrime = false;
                }
            }
        }
        return ceil;
    }
}
