package com.example.wordreferenceapp;

import android.os.Parcel;
import android.os.Parcelable;

public enum ApplicationError implements Parcelable {
	OK,
	DICT_NOT_SPECIFIED,
	NET_CONN_UNAVAILABLE,
	NET_SOCKET_TIMEOUT,
	NET_SERVER_RESPONSE,
	NOT_DEFINED;

	public static final Parcelable.Creator<ApplicationError> CREATOR = 
		new Parcelable.Creator<ApplicationError>() {
		public ApplicationError createFromParcel(Parcel in) {
			return ApplicationError.values()[in.readInt()];
		}
		public ApplicationError[] newArray(int size) {
			return new ApplicationError[size];
		}
	};

	@Override 
	public int describeContents() {
		return 0; 
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(ordinal());
	}
};
