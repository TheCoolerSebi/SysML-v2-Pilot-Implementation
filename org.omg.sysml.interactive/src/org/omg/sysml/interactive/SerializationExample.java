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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.validation.Issue;
import org.omg.sysml.lang.sysml.SysMLPackage;


public class SerializationExample {
	public static void main(String[] args) throws IOException {
		// Load SysML libraries and example model.
		String workingDirectory = System.getProperty("user.dir");
		String libraryPath = Path.of(workingDirectory, "..", "sysml.library").toRealPath().toString();
//		Path modelPath = Path.of(workingDirectory, "VehicleModel_1_Simplified_06_20_21.sysml");
		Path definitionsPath = Path
				.of(workingDirectory, "..", "sysml", "src", "examples", "Vehicle Example", "VehicleDefinitions.sysml")
				.toRealPath();
		/*Path individualsPath = Path
				.of(workingDirectory, "..", "sysml", "src", "examples", "Vehicle Example", "VehicleIndividuals.sysml")
				.toRealPath();*/
		Path usagesPath = Path
				.of(workingDirectory, "..", "sysml", "src", "examples", "Vehicle Example", "VehicleUsages.sysml")
				.toRealPath();
		SysMLInteractive interactive = SysMLInteractive.getInstance();
		interactive.loadLibrary(libraryPath);
		String definitionsText = new String(Files.readAllBytes(definitionsPath), StandardCharsets.UTF_8);
		String usagesText = new String(Files.readAllBytes(usagesPath), StandardCharsets.UTF_8);
		//String individualsText = new String(Files.readAllBytes(individualsPath), StandardCharsets.UTF_8);
		
		interactive.next();
		interactive.parse(definitionsText);
		interactive.addResourceToIndex(interactive.getResource());
		System.err.println("Parsed model " + definitionsPath);
		interactive.next();
		interactive.parse(usagesText);
		//interactive.addResourceToIndex(interactive.getResource());
		System.err.println("Parsed model " + usagesPath);
		//interactive.next();
		//interactive.parse(individualsText);
		//System.err.println("Parsed model " + individualsPath);
		
		
		List<Issue> issues = interactive.validate();
		System.err.println("Validated model");
		for (Issue issue : issues) {
			System.err.println(issue);
		}

		//Snapshot the serialized model.
		List<ObjectSnapshot> originalSnapshot = snapshotModel(interactive.getRootElement());
		//System.out.println("\nORIGINAL");
		//originalSnapshot.forEach(System.out::println);
		
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
		//Snapshot parsed modell, so the comparison is easier
		List<ObjectSnapshot> parsedSnapshot = snapshotModel(interactive.getRootElement());
		System.out.println("\nPARSED");
		//parsedSnapshot.forEach(System.out::println);
		boolean result = compareSnapshots(originalSnapshot, parsedSnapshot);
		if (result == true) 
			System.err.println("Match!");
		else
			System.err.println("Diff!");
		//compare(copyOfRootElement, interactive.getRootElement());
		
	}
	
	private static final Set<EStructuralFeature> IGNORED_FEATURES = Set.of(
			SysMLPackage.Literals.ELEMENT__ELEMENT_ID,
			SysMLPackage.Literals.ELEMENT__ALIAS_IDS,
			SysMLPackage.Literals.MEMBERSHIP__MEMBER_ELEMENT_ID,
			SysMLPackage.Literals.OWNING_MEMBERSHIP__OWNED_MEMBER_ELEMENT_ID
	);
	
	static class NumberedObject {
		private final EObject object;
		private final int number;
		
		public NumberedObject(EObject object, Map<EObject, Integer> numbering) {
			this.object = object;
			number = numbering.getOrDefault(object, -1);
		}
		
		public boolean isSame(NumberedObject other) {
			if (number == -1 && other.number == -1) {
				return Objects.equals(object, other.object);
			}
			return number == other.number;
		}
		
		@Override
		public String toString() {
			return new StringBuilder()
					.append("<")
					.append(object)
					.append(", ")
					.append(number)
					.append(">")
					.toString();
		}
	}
	
	static class ObjectSnapshot {
		private final EObject object;
		private final Map<EAttribute, Object> attributeValues;
		private final Map<EReference, List<NumberedObject>> referenceValues;
		
		public ObjectSnapshot(EObject object, Map<EObject, Integer> numbering) {
			this.object = object;
			Map<EAttribute, Object> attributeValues = new LinkedHashMap<>();
			Map<EReference, List<NumberedObject>> referenceValues = new LinkedHashMap<>();
			EClass itClass = object.eClass();
			for (EStructuralFeature feature : itClass.getEAllStructuralFeatures()) {
				if (feature.isDerived() || IGNORED_FEATURES.contains(feature)) {	//fölösleges a derived dolgokat menteni, mert azt úgyis csak skippelem
					continue;
				}
				if (feature instanceof EAttribute) {
					if (feature.isMany()) {
						throw new IllegalArgumentException("Snapshotting many attribute "+ feature + "is not supported");
					}
					attributeValues.put((EAttribute) feature, object.eGet(feature));
				} else if (feature instanceof EReference) {
					List<NumberedObject> itList = getList(object, feature, numbering); 
					referenceValues.put((EReference) feature, itList);
				} else {
					throw new IllegalArgumentException("Unknown EStructuralFeature: " + feature);
				}
			}
			this.attributeValues = Collections.unmodifiableMap(attributeValues);
			this.referenceValues = Collections.unmodifiableMap(referenceValues);
		}
		
		public boolean isSame(ObjectSnapshot other) {
			if (object.getClass() != other.object.getClass()) {
				return false;
			}
			for (Map.Entry<EAttribute, Object> pair : attributeValues.entrySet()) {
				EAttribute key = pair.getKey();
				Object value = pair.getValue();
				Object otherValue = other.attributeValues.get(key);
				if (!Objects.equals(value, otherValue)) {
					return false;
				}
			}
			for (Map.Entry<EReference, List<NumberedObject>> pair : referenceValues.entrySet()) {
				EReference key = pair.getKey();
				List<NumberedObject> values = pair.getValue();
				List<NumberedObject> otherValues = other.referenceValues.get(key);
				if (values.size() != otherValues.size()) {
					return false;
				}
				for (int i = 0; i < values.size(); i++) {
					NumberedObject value = values.get(i);
					NumberedObject otherValue = otherValues.get(i);
					if (!value.isSame(otherValue)) {
						return false;
					}
				}
			}
			return true;
		}
		
		private static List<NumberedObject> getList(EObject obj, EStructuralFeature reference, Map<EObject, Integer> numbering) {
			if (reference.isMany()) {
				@SuppressWarnings("unchecked")
				List<EObject> list = (List<EObject>) obj.eGet(reference);
				return list.stream()
						.map(value -> new NumberedObject(value, numbering))
						.collect(Collectors.toUnmodifiableList());
			} else {
				EObject value = (EObject) obj.eGet(reference);
				return value == null ? List.of() : List.of(new NumberedObject(value, numbering));
			}
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(object);
			for (Map.Entry<EAttribute, Object> pair : attributeValues.entrySet()) {
				EAttribute key = pair.getKey();
				Object value = pair.getValue();
				builder.append("\n\t")
					.append(key.getName())
					.append(" = ")
					.append(value);
			}
			for (Map.Entry<EReference, List<NumberedObject>> pair : referenceValues.entrySet()) {
				EReference key = pair.getKey();
				List<NumberedObject> values = pair.getValue();
				builder.append("\n\t")
				.append(key.getName())
				.append(" = ")
				.append(values);
			}
			return builder.toString();
		}
		
	}
	
	private static List<ObjectSnapshot> snapshotModel(EObject root) {
		Map<EObject, Integer> numbering = numberObjects(root);
		List<ObjectSnapshot> snapshotList = new ArrayList<>();
		TreeIterator<EObject> iterator = root.eAllContents();
		snapshotList.add(new ObjectSnapshot(root, numbering));	//első helyre betesszük a rootot
		while (iterator.hasNext()) {
			EObject it = iterator.next();
			snapshotList.add(new ObjectSnapshot(it, numbering));
		}
		return snapshotList;
	}
	
	public static boolean compareSnapshots(List<ObjectSnapshot>original, List<ObjectSnapshot>parsed) {
		if (original.size() != parsed.size()) {
			return false;
		}
		for (int i = 0; i < original.size(); i++) {
			ObjectSnapshot originalSnapshot = original.get(i);
			ObjectSnapshot parsedSnapshot = parsed.get(i);
			if (!originalSnapshot.isSame(parsedSnapshot)) {
				return false;
			}
		}
		return true;
	}
	
	private static Map<EObject, Integer> numberObjects(EObject root) {
		Map<EObject, Integer> map = new LinkedHashMap<>();
		map.put(root, 0);
		TreeIterator<EObject> iterator = root.eAllContents();
		int i = 1;
		while (iterator.hasNext()) {
			EObject obj = iterator.next();
			map.put(obj, i);
			i++;
		}
		return map;
	}
}
