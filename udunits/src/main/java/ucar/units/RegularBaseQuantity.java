/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.units;

/**
 * Provides support for a base quantity that is dimensionfull.
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 */
public final class RegularBaseQuantity extends BaseQuantity {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a name and symbol.
	 * 
	 * @param name
	 *            The name of the base unit.
	 * @param symbol
	 *            The symbol of the base unit.
	 */
	public RegularBaseQuantity(final String name, final String symbol)
			throws NameException {
		super(name, symbol);
	}

	/**
	 * Constructs from a name and a symbol. This is a trusted constructor for
	 * use by the parent class only.
	 * 
	 * @param name
	 *            The name of the base unit.
	 * @param symbol
	 *            The symbol of the base unit.
	 */
	protected RegularBaseQuantity(final String name, final String symbol,
			final boolean trusted) {
		super(name, symbol, trusted);
	}

	/**
	 * Indicates if this base quantity is dimensionless. Regular base quantities
	 * are always dimensionfull.
	 * 
	 * @return <code>false</code>.
	 */
	@Override
	public boolean isDimensionless() {
		return false;
	}
}
