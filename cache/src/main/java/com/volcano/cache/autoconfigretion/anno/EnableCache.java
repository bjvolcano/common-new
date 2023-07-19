package com.volcano.cache.autoconfigretion.anno;

import com.volcano.cache.autoconfigretion.CacheAutoImportSelector;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({CacheAutoImportSelector.class})
public @interface EnableCache {
}
