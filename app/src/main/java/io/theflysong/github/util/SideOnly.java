package io.theflysong.github.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author squid233
 * @since 1.0
 */
@Retention(RetentionPolicy.CLASS)
public @interface SideOnly {
    Side[] value();
}
