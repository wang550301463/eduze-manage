package com.eduze.manage.common.util;

public final class PhoneMask {

    private PhoneMask() {}

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        int visible = Math.min(3, phone.length() / 3);
        int tail = Math.min(4, phone.length() - visible);
        return phone.substring(0, visible) + "****" + phone.substring(phone.length() - tail);
    }
}
