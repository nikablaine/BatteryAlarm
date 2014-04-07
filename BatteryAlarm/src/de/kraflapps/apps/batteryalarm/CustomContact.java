package de.kraflapps.apps.batteryalarm;

import java.io.Serializable;

public class CustomContact implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String name;
	private final String email;
	private final String type;

	public CustomContact(final String cName, final String cEmail, final String cType) {
		name = cName;
		email = cEmail;
		type = cType;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return email;
	}
}
