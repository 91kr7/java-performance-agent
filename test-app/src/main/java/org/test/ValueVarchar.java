package org.test;

public final class ValueVarchar extends ValueStringBase {

    /**
     * Empty string. Should not be used in places where empty string can be
     * treated as {@code NULL} depending on database mode.
     */
    public static final ValueVarchar EMPTY = new ValueVarchar("");

    private ValueVarchar(String value) {
        super(value);
    }



}
