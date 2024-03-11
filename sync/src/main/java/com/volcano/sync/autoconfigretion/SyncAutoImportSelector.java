package com.volcano.sync.autoconfigretion;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class SyncAutoImportSelector implements ImportSelector {
   @Override
   public String[] selectImports(AnnotationMetadata annotationMetadata) {
      return new String[]{SyncAutoConfig.class.getName()};
   }
}
