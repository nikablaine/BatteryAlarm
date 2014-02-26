package de.kraflapps.apps.batteryalarm;

import java.io.Serializable;

public class CustomContact implements Serializable {
	    private String name;
	    private String email;
	    private String type;

	    public CustomContact(String n, String e, String t) { name = n; email = e; type = t; }

	    public String getName() { return name; }
	    public String getEmail() { return email; }
	    public String getType() { return type; }

	    @Override
	    public String toString() { return name; }
}
