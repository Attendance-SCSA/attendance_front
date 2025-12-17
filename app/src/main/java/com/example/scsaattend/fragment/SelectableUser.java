package com.example.scsaattend.fragment;

import android.os.Parcel;
import android.os.Parcelable;

public class SelectableUser implements Parcelable {
    long id;
    String name;
    boolean isSelected;

    public SelectableUser(long id, String name, boolean isSelected) {
        this.id = id;
        this.name = name;
        this.isSelected = isSelected;
    }

    protected SelectableUser(Parcel in) {
        id = in.readLong();
        name = in.readString();
        isSelected = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SelectableUser> CREATOR = new Creator<SelectableUser>() {
        @Override
        public SelectableUser createFromParcel(Parcel in) {
            return new SelectableUser(in);
        }

        @Override
        public SelectableUser[] newArray(int size) {
            return new SelectableUser[size];
        }
    };
}
