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

package org.omg.sysml.interactive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.validation.Issue;

public class SerializationExample {
	public static void main(String[] args) throws IOException {
		// Load SysML libraries and example model.
		String workingDirectory = System.getProperty("user.dir");
		String libraryPath = Path.of(workingDirectory, "..", "sysml.library").toRealPath().toString();
//		Path modelPath = Path.of(workingDirectory, "VehicleModel_1_Simplified_06_20_21.sysml");
		Path definitionsPath = Path
				.of(workingDirectory, "..", "sysml", "src", "examples", "Vehicle Example", "VehicleDefinitions.sysml")
				.toRealPath();
//		Path usagesPath = Path
//				.of(workingDirectory, "..", "sysml", "src", "examples", "Vehicle Example", "VehicleUsages.sysml")
//				.toRealPath();
		SysMLInteractive interactive = SysMLInteractive.getInstance();
		interactive.loadLibrary(libraryPath);
		String definitionsText = new String(Files.readAllBytes(definitionsPath), StandardCharsets.UTF_8);
//		String usagesText = new String(Files.readAllBytes(usagesPath), StandardCharsets.UTF_8);
		interactive.next();
		interactive.parse(definitionsText);
		System.err.println("Parsed model " + definitionsPath);
//		interactive.next();
//		interactive.parse(usagesText);
//		System.err.println("Parsed model " + usagesPath);
		List<Issue> issues = interactive.validate();
		System.err.println("Validated model");
		for (Issue issue : issues) {
			System.err.println(issue);
		}

		// Create a copy of the loaded model to discard any NodeModel relationships.
		// This way, we are testing the serialization of a new SysML model
		// without any concrete syntax representation known in advance.
		EObject copyOfRootElement = EcoreUtil.copy(interactive.getRootElement());
		interactive.removeResource();
		interactive.next();
		Resource resource = interactive.getResource();
		resource.getContents().add(copyOfRootElement);
		byte[] serializedBytes;
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			resource.save(outputStream, Map.of());
			serializedBytes = outputStream.toByteArray();
		}
		String serializedString = new String(serializedBytes, StandardCharsets.UTF_8);
		System.err.println("Serialized model");
		System.out.println(serializedString);

		// Parse the serialized model again to check for any round-trip errors.
		interactive.removeResource();
		interactive.next();
		interactive.parse(serializedString);
		System.err.println("Parsed serialized model");
		issues = interactive.validate();
		System.err.println("Validated serialized model");
		for (Issue issue : issues) {
			System.err.println(issue);
		}
	}
}
