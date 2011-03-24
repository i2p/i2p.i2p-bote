/* ========================================================================
 *
 *  This file is part of CODEC, which is a Java package for encoding
 *  and decoding ASN.1 data structures.
 *
 *  Author: Fraunhofer Institute for Computer Graphics Research IGD
 *          Department A8: Security Technology
 *          Fraunhoferstr. 5, 64283 Darmstadt, Germany
 *
 *  Rights: Copyright (c) 2004 by Fraunhofer-Gesellschaft 
 *          zur Foerderung der angewandten Forschung e.V.
 *          Hansastr. 27c, 80686 Munich, Germany.
 *
 * ------------------------------------------------------------------------
 *
 *  The software package is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 2.1 of the 
 *  License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public 
 *  License along with this software package; if not, write to the Free 
 *  Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 *  MA 02110-1301, USA or obtain a copy of the license at 
 *  http://www.fsf.org/licensing/licenses/lgpl.txt.
 *
 * ------------------------------------------------------------------------
 *
 *  The CODEC library can solely be used and distributed according to 
 *  the terms and conditions of the GNU Lesser General Public License for 
 *  non-commercial research purposes and shall not be embedded in any 
 *  products or services of any user or of any third party and shall not 
 *  be linked with any products or services of any user or of any third 
 *  party that will be commercially exploited.
 *
 *  The CODEC library has not been tested for the use or application 
 *  for a determined purpose. It is a developing version that can 
 *  possibly contain errors. Therefore, Fraunhofer-Gesellschaft zur 
 *  Foerderung der angewandten Forschung e.V. does not warrant that the 
 *  operation of the CODEC library will be uninterrupted or error-free. 
 *  Neither does Fraunhofer-Gesellschaft zur Foerderung der angewandten 
 *  Forschung e.V. warrant that the CODEC library will operate and 
 *  interact in an uninterrupted or error-free way together with the 
 *  computer program libraries of third parties which the CODEC library 
 *  accesses and which are distributed together with the CODEC library.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not warrant that the operation of the third parties's computer 
 *  program libraries themselves which the CODEC library accesses will 
 *  be uninterrupted or error-free.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  shall not be liable for any errors or direct, indirect, special, 
 *  incidental or consequential damages, including lost profits resulting 
 *  from the combination of the CODEC library with software of any user 
 *  or of any third party or resulting from the implementation of the 
 *  CODEC library in any products, systems or services of any user or 
 *  of any third party.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not provide any warranty nor any liability that utilization of 
 *  the CODEC library will not interfere with third party intellectual 
 *  property rights or with any other protected third party rights or will 
 *  cause damage to third parties. Fraunhofer Gesellschaft zur Foerderung 
 *  der angewandten Forschung e.V. is currently not aware of any such 
 *  rights.
 *
 *  The CODEC library is supplied without any accompanying services.
 *
 * ========================================================================
 */
package codec;

/**
 * Signals an inconsistent object state. Objects can enter inconsistent states
 * for instance if an exception is caught that shouldn't happen. Such exceptions
 * can occur due to unexpected exceptions from other code or internal errors.
 * <p>
 * A typical example is when an application knows that a specific error
 * condition cannot occur because the preconditions required for a successful
 * completion of a method call are met, but an exception is thrown nevertheless.
 * In this case, the exception can be caught and wrapped into an exception of
 * this class in order to pass on the original cause of the exception to code
 * further up the calling stack.
 * 
 * @author Volker Roth
 * @version "$Id: InconsistentStateException.java,v 1.3 2005/04/06 09:33:26
 *          flautens Exp $"
 */
public class InconsistentStateException extends RuntimeException {
    /**
     * The wrapped exception
     */
    private Exception e_;

    /**
     * Creates an instance.
     */
    public InconsistentStateException() {
    }

    /**
     * Creates an instance with the given message.
     * 
     * @param message
     *                The message.
     */
    public InconsistentStateException(String message) {
	super(message);
    }

    /**
     * Creates an exception that wraps around the given exception. The message
     * of this exception is set to the one of the given exception. The given
     * exception must not be <code>null</code>.
     * 
     * @param e
     *                The exception that shall be wrapped in this one.
     * @throws NullPointerException
     *                 if the given exception is <code>null</code>.
     */
    public InconsistentStateException(Exception e) {
	super(e.getMessage());
	e_ = e;
    }

    /**
     * Returns the wrapped exception or <code>this</code> if no exception is
     * wrapped in this one.
     * 
     * @return The wrapped exception or <code>this</code> if there is no
     *         exception wrapped by this one.
     */
    public Exception getException() {
	if (e_ == null) {
	    return this;
	}

	return e_;
    }

    /**
     * Prints the stack trace of this exception. If this exception has a wrapped
     * exception then the stack trace of the wrapped exception is printed
     * instead of the one of this exception. Since this exception was created in
     * the course of catching the wrapped exception, the location where this
     * exception was thrown is included in the stack trace of the wrapped
     * exception.
     */
    public void printStackTrace() {
	if (e_ == null) {
	    super.printStackTrace();
	} else {
	    e_.printStackTrace();
	}
    }
}
