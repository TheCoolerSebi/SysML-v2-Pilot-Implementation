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

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractLexerBasedConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.omg.sysml.util.ElementUtil;

public class RegularCommentValueConverter extends AbstractLexerBasedConverter<String> {

	@Override
	public String toValue(String string, INode node) throws ValueConverterException {
		return ElementUtil.processCommentBody(string).trim();
	}

	@Override
	protected String toEscapedString(String value) {
		if (value.contains("*/")) {
			throw new ValueConverterException("The value '" + value + "' contains an invalid */", null, null);
		}
		String[] lines = value.split("\n");
		switch (lines.length) {
		case 0:
			return "/* */";
		case 1:
			return "/* %s */".formatted(lines[0]);
		default: {
			StringBuilder builder = new StringBuilder();
			builder.append("/*\n");
			for (String line : lines) {
				builder.append(" * ").append(line).append("\n");
			}
			builder.append(" */");
			return builder.toString();
		}
		}
	}
}
