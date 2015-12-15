package org.factcenter.inchworm;

public class MismatchingHeadersException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * MismatchingHeadersException is the subclass of the java {@link RuntimeException} that
	 * can be thrown during the initialization process of the Inchworm virtual machine.
	 */
	public MismatchingHeadersException(String info) {
		super(info);
	}

}
