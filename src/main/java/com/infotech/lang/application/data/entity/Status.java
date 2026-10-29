package com.infotech.lang.application.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Status /*extends AbstractEntity */{
	@Id
	private int id;
    private String name;

    public Status() {

    }

    public Status(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
