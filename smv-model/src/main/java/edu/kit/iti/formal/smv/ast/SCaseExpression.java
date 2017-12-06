// --------------------------------------------------------
// Code generated by Papyrus Java
// --------------------------------------------------------

package edu.kit.iti.formal.smv.ast;

/*-
 * #%L
 * smv-model
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

import edu.kit.iti.formal.smv.SMVAstVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/************************************************************/

/**
 *
 */
public class SCaseExpression extends SMVExpr {
    public List<Case> cases = new LinkedList<>();

    public void add(SMVExpr condition, SMVExpr value) {
        cases.add(new Case(condition, value));
    }

    public SMVExpr compress() {
        // if all cases have the same value then finish
        if (cases.size() == 0) return this;
        final Case firstCase = cases.get(0);
        boolean b = cases.stream().allMatch(aCase -> firstCase.then.equals(aCase.then));
        if (b)
            return firstCase.then;
        //
        SCaseExpression esac = new SCaseExpression();
        Case previous = firstCase;
        SMVExpr condition = previous.condition;

        for (int i = 1; i < cases.size(); i++) {
            Case current = cases.get(i);
            if (previous.then.equals(current.then)) {
                condition = condition.or(current.condition);
            } else {
                esac.addCase(condition, previous.then);
                previous = current;
                condition = current.condition;
            }
        }
        esac.addCase(condition, previous.then);
        return esac;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SCaseExpression that = (SCaseExpression) o;

        return cases.equals(that.cases);

    }

    @Override
    public int hashCode() {
        return cases.hashCode();
    }

    public <T> T accept(SMVAstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public SMVType getSMVType() {
        List<SMVType> list = cases.stream().map((Case a) -> {
            return a.then.getSMVType();
        }).collect(Collectors.toList());

        return SMVType.infer(list);
    }

    @Override
    public @NotNull SCaseExpression inModule(@NotNull String module) {
        SCaseExpression sCaseExpression = new SCaseExpression();
        for (Case c : cases) {
            sCaseExpression.add(c.condition.inModule(module), c.then.inModule(module));
        }
        return sCaseExpression;
    }

    public Case addCase(SMVExpr cond, SMVExpr var) {
        Case c = new Case(cond, var);
        cases.add(c);
        return c;
    }

    @Override
    public String toString() {
        return "if " +
                cases.stream()
                        .map(c -> c.toString()).reduce((a, b) -> a + "\n" + b)
                        .orElseGet(() -> "")
                + " fi";
    }

    /**
     *
     */
    public static class Case {
        /**
         *
         */
        public SMVExpr condition;
        /**
         *
         */
        public SMVExpr then;

        public Case() {
        }

        public Case(SMVExpr cond, SMVExpr var) {
            condition = cond;
            then = var;
        }

        @Override
        public String toString() {
            return ":: " + condition + "->" + then;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Case aCase = (Case) o;

            if (!condition.equals(aCase.condition)) return false;
            return then.equals(aCase.then);

        }

        @Override
        public int hashCode() {
            int result = condition.hashCode();
            result = 31 * result + then.hashCode();
            return result;
        }
    }
}
