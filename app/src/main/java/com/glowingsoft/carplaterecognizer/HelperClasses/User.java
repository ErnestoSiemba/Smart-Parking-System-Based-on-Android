package com.glowingsoft.carplaterecognizer.HelperClasses;

import java.io.Serializable;

public class User implements Serializable {

    public int userType,isVerified;
    public String fullName, email, ninNumber, phoneNumber;

    public User() {

    }

    public User(String fullName, String email, String ninNumber, String phoneNumber, int userType, int isVerified) {
        this.fullName = fullName;
        this.email = email;
        this.ninNumber = ninNumber;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
        this.isVerified = isVerified;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public int getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(int isVerified) {
        this.isVerified = isVerified;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNinNumber() {
        return ninNumber;
    }

    public void setNinNumber(String ninNumber) {
        this.ninNumber = ninNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

}
