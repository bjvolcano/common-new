package com.volcano.range.autoconfigretion.anno;

import org.springframework.context.annotation.Import;
import com.volcano.range.autoconfigretion.RangeAutoImportSelector;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RangeAutoImportSelector.class)
public @interface EnableRangeFilter {
}
