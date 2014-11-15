package io.github.xsession.test;

import java.io.Serializable;

public class Student implements Serializable{
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	String name = "sss";
	int age = 18;

}
