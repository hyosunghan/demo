package com.example.demo.utils;

public class BitPermission {
    public static final int PERMISSION_INSERT = 1 << 0; // 1
    public static final int PERMISSION_DELETE = 1 << 1; // 2
    public static final int PERMISSION_UPDATE = 1 << 2; // 4
    public static final int PERMISSION_SELECT = 1 << 3; // 8

    private int permissions;

    public void setPermissions(int per) {
        this.permissions = per;
    }

    public int getPermissions() {
        return this.permissions;
    }

    public void enablePermissions(int per) {
        permissions = permissions | per;
    }

    public void disablePermissions(int per) {
        permissions = permissions & ~per;
    }

    public boolean isAllow(int per) {
        return (permissions & per) == per;
    }

    public boolean isNotAllow(int per) {
        return (permissions & per) == 0;
    }
}
