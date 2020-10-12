package com.bormannqds.mds.lib.protocoladaptor.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by User on 19/03/2015.
 *
 * NOTE: Incomprehensible bit fiddling. Why use a BigInteger when we have NIO ByteBuffers (which has an Endianness
 * re-ordering method and primitive type getters)? Java 'long' Endianness maps to the underlying platform Endianness
 * anyway.
 *
 * Of course, the problem is the underlying ad-hoc undocumented, source-only serialisation format which does...
 * unnecessary bit fiddling (f.i. such that it can only send absolute integer values but signed decimals, map 96-bit
 * OnixS::CME::Decimal data to 128-bit bit-fiddled data, ...).
 *
 * Signedness of bytes does not affect the bit patterns
 * as long as no operations are performed on the bytes before value reconstruction.
 *
 * Because it is such brittle code, it is currently used (algorithmically) ad verbatim. This makes us liable
 * to regressions introduced by silent changes.
 */
public class LegacyConverter {
    public static boolean ONIX_STYLE = true;

    // int values are transmitted as bin. reps. of  Little Endian (as opposed to htonl()'d vals!), absolute value ints!
    public static int sanerNetAbsIntBytes2Int(byte[] buffer) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.nativeOrder());
        return bb.getInt();
    }

    public static int netIntBytes2Int(byte[] buffer) {
        byte[] rev = new byte[buffer.length];
        int j = buffer.length - 1;
        for (int i = 0; i < buffer.length; i++) {
            rev[i] = buffer[j--];
        }
        BigInteger bint = new BigInteger(rev);
        return bint.intValue();
    }

    public static long netLongBytes2Long(byte[] buffer) {
        byte[] rev = new byte[buffer.length];
        int j = buffer.length - 1;
        for (int i = 0; i < buffer.length; i++) {
            rev[i] = buffer[j--];
        }
        BigInteger bint = new BigInteger(rev);
        return bint.longValue();
    }

    // NOTE: conversion to double added since ICE price Decimals are derived from doubles anyway (which actually makes
    // precision problem much worse because the chosen conversion precision is set TOO LOW at 12)!
    public static double netDecimalBytes2Double(byte[] buffer) {
        // .NET bytes are unsigned, java's are signed. Otherwise, buffer is just a
        // string of bytes, which make up a long binary representation of the
        // number as an integer, with the least significant byte in buffer[0],
        // and the most significant in buffer[12]. The other issue is that these
        // bytes are unsigned and java has 'em signed.
        byte[] unscaledValueBytes = new byte[12];
        // The array is little endian, java BigIntegers expect a big endian array,
        // SIGH.
        int j = 11;
        for (int i = 0; i < 12; i++) {
            unscaledValueBytes[i] = buffer[j--];
        }
        //System.arraycopy(buffer,0, unscaledValueBytes, 0, unscaledValueBytes.length);
        BigInteger unscaledValue = new BigInteger(unscaledValueBytes);
        if ((buffer[15] == 128) || (buffer[15] == -128)){
            unscaledValue = unscaledValue.negate();
        }
        byte scale = buffer[14];
        boolean negScale = ((int)scale < 0);
        if (ONIX_STYLE && negScale) {
            return (new BigDecimal(unscaledValue, -(int) scale)).doubleValue();
        } else {
            return (new BigDecimal(unscaledValue, scale)).doubleValue();
        }
    }
}
