package com.synchronoss.saw.analysis.service.migrationservice;

import com.google.gson.JsonObject;
import com.synchronoss.saw.analysis.modal.Analysis;

public interface AnalysisSipDslConverter {
  public Analysis convert(JsonObject oldAnalysisDefinition);

  default Analysis setCommonParams(Analysis analysis) {
    if (analysis == null) {
      return null;
    }

    return analysis;
  }
}
