/**
 * SysML 2 Pilot Implementation
 * Copyright (c) 2023 Budapest University of Technology and Economics (BME)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of theGNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @license LGPL-3.0-or-later <http://spdx.org/licenses/LGPL-3.0-or-later>
 * 
 * Contributors:
 *  Kristóf Marussy, BME
 */

package org.omg.sysml.xtext.conversion;

import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SysMLValueConverterService extends DefaultTerminalConverters {
	@Inject
	private RegularCommentValueConverter regularCommentValueConverter;

	@ValueConverter(rule = "REGULAR_COMMENT")
	public IValueConverter<String> REGULAR_COMMENT() {
		return regularCommentValueConverter;
	}

	@ValueConverter(rule = "org.omg.kerml.expressions.xtext.KerMLExpressions.REGULAR_COMMENT")
	public IValueConverter<String> KerMLExpressionsREGULAR_COMMENT() {
		return regularCommentValueConverter;
	}
}
