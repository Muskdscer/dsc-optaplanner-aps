package com.upec.factoryscheduling.aps.solution;

import org.optaplanner.core.api.solver.change.ProblemChange;

public interface IncrementalSolution <Solution_>{
    void addProblemChange(ProblemChange<Solution_> problemChange);
    void clearProcessedChanges();
}
