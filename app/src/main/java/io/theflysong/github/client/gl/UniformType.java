package io.theflysong.github.client.gl;

import io.theflysong.github.util.Side;
import io.theflysong.github.util.SideOnly;

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