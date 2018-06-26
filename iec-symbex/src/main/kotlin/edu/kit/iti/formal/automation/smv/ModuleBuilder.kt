package edu.kit.iti.formal.automation.smv

/*-
 * #%L
 * iec61131lang
 * %%
 * Copyright (C) 2017 Alexander Weigl
 * %%
 * This program isType free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program isType distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a clone of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import edu.kit.iti.formal.automation.smv.translators.*
import edu.kit.iti.formal.automation.st.DefaultInitValue
import edu.kit.iti.formal.automation.st.InitValueTranslator
import edu.kit.iti.formal.automation.st.ast.Literal
import edu.kit.iti.formal.automation.st.ast.ProgramDeclaration
import edu.kit.iti.formal.automation.st.ast.TypeDeclarations
import edu.kit.iti.formal.automation.st.ast.VariableDeclaration
import edu.kit.iti.formal.smv.ast.SAssignment
import edu.kit.iti.formal.smv.ast.SMVExpr
import edu.kit.iti.formal.smv.ast.SMVModule
import edu.kit.iti.formal.smv.ast.SVariable
import java.util.*

/**
 * @author Alexander Weigl
 * @version 1 (12.12.16)
 */
class ModuleBuilder(val program: ProgramDeclaration,
                    val types: TypeDeclarations,
                    val finalState: SymbolicState) : Runnable {
    val module = SMVModule("")
    val vardeps: VariableDependency = VariableDependency(finalState)
    //private Map<VariableDeclaration, SVariable> vars = new HashMap<>();
    var typeTranslator: TypeTranslator = DefaultTypeTranslator.INSTANCE
    var valueTranslator = DefaultValueTranslator.INSTANCE
    var initValueTranslator: InitValueTranslator = DefaultInitValue

    override fun run() {
        module.name = program.name

        val outputVars = HashSet(program.scope
                .filterByFlags(VariableDeclaration.OUTPUT))

        val inputVars = program.scope
                .filterByFlags(VariableDeclaration.INPUT)


        // TODO fix so this terminates
        //Set<SVariable> stateVariables = vardeps.dependsOn(outputVars, inputVars);

        // Using this workaround instead
        val stateVariables = finalState.keys
                .filter { (name) -> inputVars.stream().noneMatch { v2 -> v2.name == name } }

        val outputVarNames = outputVars.map(VariableDeclaration::name)
        for (`var` in stateVariables) {
            if (outputVarNames.contains(`var`.name)) {
                `var`.addMetadata(OUTPUT_VARIABLE)
            }
        }

        insertVariables(stateVariables)
        insertInputVariables(inputVars)
    }

    private fun insertInputVariables(decls: List<VariableDeclaration>) {
        decls.map { this.typeTranslator.translate(it) }
                .map { it.addMetadata(INPUT_VARIABLE) }
                .forEach { module.moduleParameters.add(it) }
    }

    private fun insertVariables(variables: Collection<SVariable>) {
        for (v in variables) {
            addVarDeclaration(v)
            addInitAssignment(v)
            addNextAssignment(v)
        }
    }

    private fun addVarDeclaration(s: SVariable) {
        module.stateVars.add(s)
    }

    private fun addInitAssignment(`var`: SVariable) {
        val s = program.scope.getVariable(`var`.name)
        val e: SMVExpr
        if (s!!.init != null) {
            val sv = s.init as Literal?
            e = sv!!.accept(SymbolicExecutioner())!!
        } else {
            e = this.valueTranslator.translate(
                    this.initValueTranslator.getInit(s.dataType!!))
        }
        val a = SAssignment(`var`, e)
        module.initAssignments.add(a)
    }

    private fun addNextAssignment(s: SVariable) {
        val e = finalState[s]
        val a = SAssignment(s, e!!)
        module.nextAssignments.add(a)
    }

    companion object {
        val INPUT_VARIABLE = 1
        val OUTPUT_VARIABLE = 2
    }
}
