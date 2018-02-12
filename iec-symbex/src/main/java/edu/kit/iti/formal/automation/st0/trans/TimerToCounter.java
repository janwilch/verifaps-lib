package edu.kit.iti.formal.automation.st0.trans;

/*-
 * #%L
 * iec-symbex
 * %%
 * Copyright (C) 2016 Alexander Weigl
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import edu.kit.iti.formal.automation.datatypes.DataTypes;
import edu.kit.iti.formal.automation.datatypes.TimeType;
import edu.kit.iti.formal.automation.datatypes.values.TimeValue;
import edu.kit.iti.formal.automation.datatypes.values.Value;
import edu.kit.iti.formal.automation.st.ast.Literal;
import edu.kit.iti.formal.automation.st.ast.ProgramDeclaration;
import edu.kit.iti.formal.automation.st.ast.SimpleTypeDeclaration;
import edu.kit.iti.formal.automation.st.ast.VariableDeclaration;
import edu.kit.iti.formal.automation.st.util.AstMutableVisitor;
import edu.kit.iti.formal.automation.visitors.Visitable;

import java.util.Objects;

/**
 * @author Alexander Weigl (06.07.2014)
 * @version 1
 */
public class TimerToCounter extends AstMutableVisitor {
    public static long DEFAULT_CYCLE_TIME = 4;
    private final long cycleTime;

    public TimerToCounter(long cycleTime) {
        this.cycleTime = cycleTime;
    }

    public static ST0Transformation getTransformation() {
        return state -> {
            TimerToCounter ttc = new TimerToCounter(DEFAULT_CYCLE_TIME);
            state.theProgram = (ProgramDeclaration) ttc.visit(state.theProgram);
        };
    }

    @Override
    public Object defaultVisit(Visitable visitable) {
        return visitable;
    }

    @Override
    public Object visit(VariableDeclaration vd) {
        if (vd.getDataTypeName().equalsIgnoreCase("TIME")
                || vd.getDataType() == TimeType.TIME_TYPE) {

            VariableDeclaration newVariable = new VariableDeclaration(
                    vd.getName(), vd.getType(), DataTypes.UINT);

            int cycles = 0;
            if (vd.getInit() != null) {
                Value<TimeType, TimeValue> val = ((Literal) vd.getInit()).asValue();
                cycles = (int) (val.getValue().getMilliseconds() / cycleTime);
            } else {
                cycles = 0;
            }

            Literal newVal = Literal.integer(cycles);
            SimpleTypeDeclaration sd = new SimpleTypeDeclaration();
            sd.setInitialization(newVal);
            newVariable.setTypeDeclaration(sd);
            return newVariable;
        }
        return super.visit(vd);
    }

    @Override
    public Object visit(Literal literal) {
        if (literal.getDataType() == TimeType.TIME_TYPE ||
                Objects.equals(literal.getDataTypeName(), TimeType.TIME_TYPE.getName())) {
            Value<TimeType, TimeValue> val = literal.asValue();
            int cycles = (int) (val.getValue().getMilliseconds() / this.cycleTime);
            return Literal.integer(cycles);
        }
        return super.visit(literal);
    }
}
