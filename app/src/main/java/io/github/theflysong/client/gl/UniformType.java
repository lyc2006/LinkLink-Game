package io.github.theflysong.client.gl;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

@SideOnly(Side.CLIENT)
public enum UniformType {
    I1(4),
    F1(4),
    F4(16),
    MAT_F4(64);

    public final int length;

    UniformType(int length) {
        this.length = length;
    }
}