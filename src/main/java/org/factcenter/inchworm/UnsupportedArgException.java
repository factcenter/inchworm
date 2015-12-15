package org.factcenter.inchworm;


/**
 * UnsupportedArgException is the superclass of the java RuntimeException that can be thrown
 * during the assembly process while writing the VMState ops.
 */

public class UnsupportedArgException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UnsupportedArgException(String info) {
		super(info);
	}
}