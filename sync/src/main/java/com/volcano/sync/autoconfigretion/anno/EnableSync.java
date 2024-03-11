package com.volcano.sync.autoconfigretion.anno;

import com.volcano.sync.autoconfigretion.SyncAutoImportSelector;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({SyncAutoImportSelector.class})
public @interface EnableSync {
}
