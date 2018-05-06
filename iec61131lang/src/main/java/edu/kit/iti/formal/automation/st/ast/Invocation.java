package edu.kit.iti.formal.automation.st.ast;

/*-
 * #%L
 * iec61131lang
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

import edu.kit.iti.formal.automation.datatypes.AnyDt;
import edu.kit.iti.formal.automation.parser.IEC61131Parser;
import edu.kit.iti.formal.automation.scope.Scope;
import edu.kit.iti.formal.automation.visitors.Visitor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by weigla on 09.06.2014.
 *
 * @author weigl, Augusto Modanese
 * @version 3, adapt function call as invocation
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Invocation extends Initialization {
    @NotNull
    private SymbolicReference callee = new SymbolicReference();
    @NotNull
    private List<Parameter> parameters = new ArrayList<>();

    public Invocation(@NotNull String calleeName) {
        setCalleeName(calleeName);
    }

    public Invocation(@NotNull String calleeName, @NotNull Expression... expr) {
        setCalleeName(calleeName);
        for (Expression e : expr) {
            parameters.add(new Parameter(e));
        }
    }

    @Deprecated
    public Invocation(@NotNull Invocation invocation) {
        callee = invocation.getCallee();
        parameters.addAll(invocation.parameters);
    }

    public Invocation(@NotNull String calleeName, @NotNull List<Expression> expr) {
        setCalleeName(calleeName);
        for (Expression e : expr) {
            parameters.add(new Parameter(e));
        }
    }

    public Invocation(@NotNull FunctionDeclaration function) {
        setCallee(new SymbolicReference(function.getName()));
        callee.setIdentifiedObject(function);
    }

    public void addParameter(@NotNull Parameter parameter) {
        parameters.add(parameter);
        parameters.sort(Parameter::compareTo);
    }

    public void addParameters(@NotNull List<@NotNull Parameter> parameterList) {
        parameters.addAll(parameterList);
        parameters.sort(Parameter::compareTo);
    }

    public void addExpressionParameters(@NotNull List<@NotNull Expression> expressionList) {
        expressionList.forEach(e -> parameters.add(new Parameter(e)));
        parameters.sort(Parameter::compareTo);
    }

    @NotNull
    public List<Parameter> getInputParameters() {
        return parameters.stream().filter(Parameter::isInput).collect(Collectors.toList());
    }

    @NotNull
    public List<Parameter> getOutputParameters() {
        return parameters.stream().filter(Parameter::isOutput).collect(Collectors.toList());
    }

    @NotNull
    public String getCalleeName() {
        return callee.toString();
    }

    public void setCalleeName(@NotNull String calleeName) {
        callee = new SymbolicReference(calleeName);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T accept(@NotNull Visitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnyDt dataType(@NotNull Scope localScope) {
        return ((Invocable) callee.getIdentifiedObject()).getReturnType();
    }

    @NotNull
    @Override
    public Invocation copy() {
        Invocation fc = new Invocation(this);
        fc.setRuleContext(getRuleContext());
        fc.callee = callee.copy();
        fc.setParameters(new ArrayList<>(parameters.stream().map(Parameter::copy).collect(Collectors.toList())));
        return fc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter
            extends Top<IEC61131Parser.Param_assignmentContext> implements Comparable {
        private String name;
        private boolean output;
        private Expression expression;

        public Parameter(@NotNull Expression expr) {
            this(null, false, expr);
        }

        public boolean isInput() {
            return !output;
        }

        @NotNull
        @Override
        public Parameter copy() {
            return new Parameter(name, output, expression.copy());
        }

        @Override
        public <T> T accept(@NotNull Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int compareTo(@NotNull Object o) {
            if (!(o instanceof Parameter))
                throw new IllegalArgumentException();
            if (((Parameter) o).getName() != null)
                return name.compareTo(((Parameter) o).getName());
            else
                return 0;
        }
    }
}
