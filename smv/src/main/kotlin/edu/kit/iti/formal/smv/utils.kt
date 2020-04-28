package edu.kit.iti.formal.smv

import edu.kit.iti.formal.smv.ast.*

/**Extensions**/

fun Iterable<SMVExpr>.joinToExpr(operator: SBinaryOperator): SMVExpr =
        reduce { a, b -> a.op(operator, b) }

fun Iterable<SMVExpr>.disjunction(): SMVExpr = joinToExpr(SBinaryOperator.OR)
fun Iterable<SMVExpr>.conjunction(): SMVExpr = joinToExpr(SBinaryOperator.AND)

fun Collection<SMVExpr>.joinToExpr(operator: SBinaryOperator = SBinaryOperator.AND, default: SMVExpr? = null): SMVExpr =
        if (size > 0 || default == null) {
            reduce { a, b -> a.op(operator, b) }
        } else {
            default
        }

fun Collection<SMVExpr>.disjunction(default: SMVExpr): SMVExpr =
        joinToExpr(SBinaryOperator.OR, default)

fun Collection<SMVExpr>.conjunction(default: SMVExpr): SMVExpr =
        joinToExpr(SBinaryOperator.AND, default)


/**
 * Creates a modules that maintains the history of the given variables.
 * @author Alexander Weigl
 * @version 1 (31.07.18)
 */
open class HistoryModuleBuilder(
        val name: String = "History",
        val variables: List<SVariable>,
        val length: Int) : Runnable {
    val module = SMVModule(name)
    val moduleType = ModuleType(name, variables)

    init {
        assert(length > 0)
    }

    open fun addVariable(v: SVariable) {
        val first = SVariable("${v.name}_$0", v.dataType!!)
        module.moduleParameters.add(first)

        // state variables
        val vars = (1..length).map {
            SVariable("${v.name}_$$it", v.dataType!!)
        }
        module.stateVars.addAll(vars)

        val next = vars.toList()
        val from = vars.subList(0, vars.lastIndex).toMutableList()
        from.add(0, first)

        assert(next.size == from.size)

        next.zip(from).forEach { (n, f) ->
            module.nextAssignments.add(SAssignment(n, f))
        }
    }

    override fun run() {
        variables.forEach { addVariable(it) }
    }
}


fun SMVExpr.find(pred: (SMVExpr) -> Boolean) = this.accept(FindSExpr(pred))

class FindSExpr(val pred: (SMVExpr) -> Boolean) : SMVAstDefaultVisitor<SMVExpr>() {
    override fun defaultVisit(top: SMVAst): SMVExpr? = null
    override fun visit(v: SVariable): SMVExpr? {
        if (pred(v)) return v
        return super.visit(v)
    }

    override fun visit(be: SBinaryExpression): SMVExpr? {
        if (pred(be)) return be
        return super.visit(be)
    }

    override fun visit(ue: SUnaryExpression): SMVExpr? {
        if (pred(ue)) return ue
        return super.visit(ue)
    }

    override fun visit(l: SLiteral): SMVExpr? {
        if (pred(l)) return l
        return super.visit(l)
    }

    override fun visit(ce: SCaseExpression): SMVExpr? {
        if (pred(ce)) return ce
        return super.visit(ce)
    }


    override fun visit(func: SFunction): SMVExpr? {
        if (pred(func)) return func
        return super.visit(func)
    }

    override fun visit(quantified: SQuantified): SMVExpr? {
        if (pred(quantified)) return quantified
        return super.visit(quantified)
    }
}