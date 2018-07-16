/*
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

package edu.kit.iti.formal.automation.st0.trans

import edu.kit.iti.formal.automation.st.ast.VariableDeclaration
import edu.kit.iti.formal.automation.st0.TransformationState

/**
 * Embed the GVL in the program's scope and rename the GVL variables accordingly.
 *
 * @author Augusto Modanese, Alexander Weigl
 */
class GlobalVariableListEmbedding : CodeTransformation {
    override fun transform(state: TransformationState): TransformationState {
        val global = state.scope.topLevel
        global.variables.forEach {
            it.type = VariableDeclaration.GLOBAL
            state.scope.add(it)
        }
        return state
    }

   /* internal class GVLRenameVisitor : AstMutableVisitor() {
        override fun visit(symbolicReference: SymbolicReference): Expression {
            if (symbolicReference.hasSub())
                symbolicReference.sub!!.accept(this)
            if (symbolicReference.identifier == GVL_NAME) {
                assert(symbolicReference.hasSub())
                symbolicReference.sub!!.identifier = GVL_NEW_PREFIX + symbolicReference.sub!!.identifier
                return symbolicReference.sub!!
            }
            return super.visit(symbolicReference)
        }
    }*/
}
