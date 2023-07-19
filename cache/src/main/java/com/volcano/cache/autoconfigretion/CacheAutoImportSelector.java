package com.volcano.cache.autoconfigretion;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class CacheAutoImportSelector implements ImportSelector {
   public String[] selectImports(AnnotationMetadata annotationMetadata) {
      return new String[]{CacheAutoConfig.class.getName()};
   }
}
