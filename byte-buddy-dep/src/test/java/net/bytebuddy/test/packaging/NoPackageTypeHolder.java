/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package net.bytebuddy.test.packaging;

public class NoPackageTypeHolder {

	public static final Class<?> TYPE;

	static {
		try {
			/*
			 * We have to use reflection to access a type from the default package,
			 * because normally those types can only be used from the default package,
			 * and we don't really want to put tests in the default package.
			 */
			TYPE = Class.forName("NoPackageType");
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException(
					"Unexpected error while trying to access a class from the default package", e
			);
		}
	}

}
