/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */

package de.flexiprovider.common.exceptions;

/**
 * This exception is the parentclass of all exceptions, that relate to the ec -
 * arithmetic.
 * 
 * @author Birgit Henhapl
 * @see de.flexiprovider.common.math.ellipticcurves.Point
 * @see de.flexiprovider.common.math.ellipticcurves.PointGFP
 */
public class ECException extends RuntimeException {

    private static final String diagnostic = "An ec-specific exception was thrown";

    /**
     * Default constructor. Calls super-constructor with the message "An
     * ec-specific exception was thrown".
     */
    public ECException() {
	super(diagnostic);
    }

    /**
     * Constructor with the message "An ec-specific exception was thrown:
     * <em>cause</em>"
     * 
     * @param cause
     *                String specifying cause of exception
     */
    public ECException(String cause) {
	super(diagnostic + ": " + cause);
    }

}
