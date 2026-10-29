package com.infotech.lang.application.data.entity;

import java.util.UUID;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

//@MappedSuperclass
//public abstract class AbstractEntity {
//
//    @Id
//    @GeneratedValue
////    @Type(type = "uuid-char")
//    private Integer id;
//
//    public Integer getId() {
//        return id;
//    }
//
//    public void setId(Integer id) {
//        this.id = id;
//    }
//
//    @Override
//    public int hashCode() {
//        if (id != null) {
//            return id.hashCode();
//        }
//        return super.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (!(obj instanceof AbstractEntity)) {
//            return false; // null or other class
//        }
//        AbstractEntity other = (AbstractEntity) obj;
//
//        if (id != null) {
//            return id.equals(other.id);
//        }
//        return super.equals(other);
//    }
//} -- kumar experimenting uuid feature 03.25.23

@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue
//    @Type(value = UUID.class)
    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractEntity)) {
            return false; // null or other class
        }
        AbstractEntity other = (AbstractEntity) obj;

        if (id != null) {
            return id.equals(other.id);
        }
        return super.equals(other);
    }
}

