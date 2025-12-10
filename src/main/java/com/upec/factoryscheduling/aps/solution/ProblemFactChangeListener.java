package com.upec.factoryscheduling.aps.solution;

import org.optaplanner.core.api.solver.change.ProblemChange;
import org.optaplanner.core.api.solver.change.ProblemChangeDirector;

public class ProblemFactChangeListener implements ProblemChange<FactorySchedulingSolution> {

    @Override
    public void doChange(FactorySchedulingSolution workingSolution, ProblemChangeDirector problemChangeDirector) {
        problemChangeDirector.updateShadowVariables();
    }
}
