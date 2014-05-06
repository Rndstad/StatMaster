package net.amoebaman.statmaster;

import java.util.ArrayList;

public class Statistic {
	
	public String space_name;
	public String underscore_name;
	public ArrayList<String> categories;
	public double defaultValue;
	
	public Statistic(String name, double defaultValue, String... categories){
		this.space_name = name.replace('_', ' ');
		this.underscore_name = name.replace(' ', '_').toLowerCase();
		this.categories = new ArrayList<String>();
		for(String str : categories)
			this.categories.add(str);
		this.defaultValue = defaultValue;
	}
	
	public boolean equals(Object x){
		if(x instanceof Statistic){
			Statistic other = (Statistic) x;
			return underscore_name.equals(other.underscore_name);
		}
		return false;
	}
	
	public int hashCode(){
		return underscore_name.hashCode();
	}
	
	public String toString(){
		return space_name;
	}

}
