package com.example.scsaattend.fragment;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    String name;
    boolean isSelected;

    public User(String name, boolean isSelected) {
        this.name = name;
        this.isSelected = isSelected;
    }

    protected User(Parcel in) {
        name = in.readString();
        isSelected = in.readByte() != 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }
}