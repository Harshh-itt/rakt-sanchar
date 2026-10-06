package com.example.blood.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "donors")
public class Donor {

    @Id
    private String id;

    private String name;
    private String bloodGroup;
    private String city;
    private String phone;
    private Integer age;

    public Donor() {
    }

    public Donor(String name, String bloodGroup, String city, String phone, Integer age) {
        this.name = name;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.phone = phone;
        this.age = age;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}