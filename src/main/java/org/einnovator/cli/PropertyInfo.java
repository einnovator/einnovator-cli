package org.einnovator.cli;

import org.einnovator.util.model.ObjectBase;
import org.einnovator.util.model.ToStringCreator;

public class PropertyInfo extends ObjectBase {

	private String name;
	
	private String type;

	private String description;

	private String declaredIn;

	public PropertyInfo() {
		super();
	}

	public PropertyInfo(Object obj) {
		super(obj);
	}

	public PropertyInfo(String name, String type, String declaredIn, String description) {
		super();
		this.name = name;
		this.type = type;
		this.declaredIn = declaredIn;
		this.description = description;
	}

	/**
	 * Get the value of property {@code name}.
	 *
	 * @return the value of {@code name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the value of property {@code name}.
	 *
	 * @param name the value of {@code name}
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the value of property {@code type}.
	 *
	 * @return the value of {@code type}
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the value of property {@code type}.
	 *
	 * @param type the value of {@code type}
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the value of property {@code declaredIn}.
	 *
	 * @return the value of {@code declaredIn}
	 */
	public String getDeclaredIn() {
		return declaredIn;
	}

	/**
	 * Set the value of property {@code declaredIn}.
	 *
	 * @param declaredIn the value of {@code declaredIn}
	 */
	public void setDeclaredIn(String declaredIn) {
		this.declaredIn = declaredIn;
	}

	/**
	 * Get the value of property {@code description}.
	 *
	 * @return the value of {@code description}
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the value of property {@code description}.
	 *
	 * @param description the value of {@code description}
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public ToStringCreator toString1(ToStringCreator creator) {
		return super.toString1(creator
				.append("name", name)
				.append("type", type)
				.append("description", description)
				);
	}

}
