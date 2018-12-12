package edu.kit.iti.formal.automation.testtables.viz

import com.github.jferard.fastods.*
import com.github.jferard.fastods.datastyle.DataStyle
import com.github.jferard.fastods.datastyle.createFloatStyleBuilder
import com.github.jferard.fastods.style.BorderAttribute
import com.github.jferard.fastods.style.TableCellStyle
import com.github.jferard.fastods.util.SimpleLength
import edu.kit.iti.formal.automation.datatypes.AnyBit
import edu.kit.iti.formal.automation.datatypes.AnyInt
import edu.kit.iti.formal.automation.datatypes.EnumerateType
import edu.kit.iti.formal.automation.sfclang.getUniqueName
import edu.kit.iti.formal.automation.testtables.grammar.TestTableLanguageBaseVisitor
import edu.kit.iti.formal.automation.testtables.grammar.TestTableLanguageParser
import edu.kit.iti.formal.automation.testtables.model.*
import edu.kit.iti.formal.automation.testtables.model.TableRow
import edu.kit.iti.formal.smv.CounterExample
import edu.kit.iti.formal.smv.EnumType
import edu.kit.iti.formal.smv.SMVAstDefaultVisitorNN
import edu.kit.iti.formal.smv.SMVTypes
import edu.kit.iti.formal.smv.ast.*
import java.util.*
import java.util.logging.Logger

abstract class ODSWriter {
    protected val odsFactory = OdsFactory.create(Logger.getLogger(""), Locale.US)
    val writer = odsFactory.createWriter()
    protected val document = writer.document()
}

abstract class ODSTestTableWriter(protected val gtt: GeneralizedTestTable) : ODSWriter() {
    protected val input = gtt.programVariables.filter { it.isInput }
    protected val output = gtt.programVariables.filter { it.isOutput }
}

class ODSCounterExampleWriter constructor(
        private val counterExample: CounterExample,
        gtt: GeneralizedTestTable,
        private val mapping: Collection<Mapping>,
        var tableStyle: TableStyle = DefaultTableStyle)
    : Runnable, ODSTestTableWriter(gtt) {
    lateinit var currentTable: Table

    override fun run() = mapping.forEach { createTable(it) }

    private fun createTable(m: Mapping) {
        currentTable = document.addTable(getUniqueName())
        writeHeader()
        writeCounterExample(m)
    }

    private fun writeHeader() {
        writeCategories()
        writeVariableColumns()
    }

    private fun writeCategories() {
        val row = currentTable.nextRow()
        val cell = row.walker

        cell.setStringValue("#")
        cell.setStyle(tableStyle.styleRowIdHeader)
        cell.next()

        if (gtt.options.relational) {
            //TODO
            cell.setStringValue("PAUSE")
            cell.setStyle(tableStyle.styleCategoryHeader)
            cell.setColumnsSpanned(gtt.maxProgramRun)
            cell.next()
        }

        cell.setStringValue("INPUT")
        cell.setStyle(tableStyle.styleCategoryHeader)
        cell.setColumnsSpanned(input.size)
        cell.next()

        cell.setStringValue("OUTPUT")
        cell.setStyle(tableStyle.styleCategoryHeader)
        cell.setColumnsSpanned(output.size)
        cell.next()
    }

    private fun writeVariableColumns() {
        val row = currentTable.nextRow()
        val cell = row.walker

        cell.setStringValue("")
        cell.next()

        if (gtt.options.relational) {
            gtt.programRuns.forEach {
                cell.setStringValue(it)
                cell.setStyle(tableStyle.stylePauseVariableHeader)
                cell.next()
            }
        }


        if (input.isEmpty()) {
            cell.setStringValue("")
            cell.next()
        } else {
            input.forEach {
                cell.setStringValue(it.name)
                cell.setStyle(tableStyle.styleInputVariableHeader)
                cell.next()
            }
        }
        output.forEach {
            cell.setStringValue(it.name)
            cell.setStyle(tableStyle.styleOutputVariableHeader)
            cell.next()
        }
    }

    fun writeCounterExample(map: Mapping) {
        val rowIds = map.asRowList()
        rowIds.forEachIndexed { index, rowId ->
            writeLine(index, rowId)
        }
    }

    private fun writeLine(index: Int, rowId: String) {
        val row = currentTable.nextRow()
        val cell = row.walker

        val tableRow = gtt.getTableRow(rowId)

        cell.setStringValue(tableRow?.id)
        cell.setStyle(tableStyle.styleRowId)
        cell.next()

        if (gtt.options.relational) {
            gtt.programRuns.forEach {
                cell.setStringValue(it)
                cell.setStyle(tableStyle.stylePauseVariableHeader)
                cell.next()
            }
        }

        if (input.isEmpty()) {
            cell.setStringValue("")
            cell.next()
        } else {
            input.forEach {
                //Fix input clashes
                val v = counterExample[index, it.name]
                cell.setStyle(tableStyle.styleInputValue)
                cell.setStringValue(v)
                cell.setTooltip(tableRow?.rawFields?.get(it)?.text)
                cell.next()
            }
        }
        output.forEach {
            val v = counterExample[index, it.externalVariable(gtt.programRuns).name]
            cell.setStyle(tableStyle.styleOutputValue)
            cell.setTooltip(tableRow?.rawFields?.get(it)?.text)
            cell.setStringValue(v)
            cell.next()
        }
    }
}

open class TableUnwinder(private val gtt: GeneralizedTestTable,
                         private val unwinding: Map<TableNode, Int>) {
    private val ret = ArrayList<TableRow>()
    operator fun invoke(): List<TableRow> {
        ret.clear()
        unwind(gtt.region)
        return ret.toList()
    }

    private fun unwind(tn: TableNode) =
            when (tn) {
                is Region -> unwind(tn);
                is TableRow -> unwind(tn)
            }

    private fun unwind(tr: TableRow) {
        val num = unwindingsOf(tr)
        for (i in 1..num) {
            ret.add(tr)
        }
    }

    private fun unwind(region: Region) {
        val num = unwindingsOf(region)
        for (i in 1..num) {
            region.children.forEach { unwind(it) }
        }
    }

    protected fun unwindingsOf(tn: TableNode): Int =
            unwinding.getOrDefault(tn, tn.duration.defaultUnwindings)

    private val Duration.defaultUnwindings: Int
        get() = when (this@defaultUnwindings) {
            is Duration.Omega -> 1
            is Duration.ClosedInterval -> Math.max(lower, 1)
            is Duration.OpenInterval -> Math.max(lower, 1)
        }
}

fun createTableWithoutProgram(gtt: GeneralizedTestTable, tableStyle: TableStyle, unwindedRows: List<TableRow>): ODSDebugTable {
    val cat = arrayListOf<ValueColumn<ODSDebugTable>>()
    val styleMap = HashMap<String, TableCellStyle>()

    cat += RowIdDebugColumn(arrayOf("", "ROW"))

    if (gtt.options.relational) {
        val categoryPause = "PAUSE"
        styleMap[categoryPause] = tableStyle.styleCategoryHeader
        gtt.programRuns.forEach {
            //TODO add a new column type for pause == BoolColumn
            cat += EmptyDebugColumn(arrayOf("PAUSE", it))
        }
    }

    val inputCategory = "INPUT"
    styleMap["INPUT"] = tableStyle.styleCategoryHeader

    gtt.programVariables.filter { it.isInput }
            .forEach {
                val group = arrayOf(inputCategory, it.name)
                cat += ValueDebugColumn(group, it, RandomValueOracle)
                cat += ConstraintDebugColumn(group, it)
            }


    val outputCategory = "OUTPUT"
    styleMap[outputCategory] = tableStyle.styleCategoryHeader
    gtt.programVariables.filter { it.isOutput }
            .forEach {
                val group = arrayOf(outputCategory, it.name)
                cat += ValueDebugColumn(group, it, RandomValueOracle)
                cat += ConstraintDebugColumn(group, it)
            }

    return ODSDebugTable(gtt, cat, unwindedRows)
}


fun createTableWithProgram(program: SMVModule,
                           gtt: GeneralizedTestTable,
                           tableStyle: TableStyle,
                           unwindedRows: List<TableRow>): ODSDebugTable {
    val cat = arrayListOf<ValueColumn<ODSDebugTable>>()
    val styleMap = HashMap<String, TableCellStyle>()

    cat += RowIdDebugColumn(arrayOf("", "ROW"))

    if (gtt.options.relational) {
        val categoryPause = "PAUSE"
        styleMap[categoryPause] = tableStyle.styleCategoryHeader
        gtt.programRuns.forEach {
            //TODO add a new column type for pause == BoolColumn
            cat += EmptyDebugColumn(arrayOf("PAUSE", it))
        }
    }

    val inputCategory = "INPUT"
    styleMap["INPUT"] = tableStyle.styleCategoryHeader
    val allInputVars = program.inputVars.map { it.name to false }.toMap().toMutableMap()

    gtt.programVariables.filter { it.isInput }
            .forEach {
                allInputVars[it.name] = true
                val group = arrayOf(inputCategory, it.name)
                cat += ValueDebugColumn(group, it, RandomValueOracle)
                cat += ConstraintDebugColumn(group, it)
            }

    allInputVars.filter { (a, b) -> !b }
            .forEach { (a, b) ->
                val group = arrayOf(inputCategory, a)
                cat += EmptyDebugColumn(group)
            }

    val outputCategory = "OUTPUT"
    styleMap[outputCategory] = tableStyle.styleCategoryHeader
    gtt.programVariables.filter { it.isOutput }
            .forEach {
                val group = arrayOf(outputCategory, it.name)
                cat += ConstraintDebugColumn(group, it)
            }

    val programCategory = "INTERNAL"
    styleMap[programCategory] = tableStyle.styleCategoryHeader

    program.stateVars.forEach { variable ->
        val init = program.initAssignments.find { it.target == variable }
                ?: throw IllegalStateException()
        val next = program.nextAssignments.find { it.target == variable }
                ?: throw IllegalStateException()
        val group = arrayOf(programCategory, variable.name)
        cat += ProgramOutputDebugColumn(group, variable, init, next)
    }

    return ODSDebugTable(gtt, cat, unwindedRows).also {
        it.programInputVariables = program.inputVars.map { it.name }.toMutableSet()

    }
}

/*
abstract class TableModel<T> {
    abstract val rowSpan: Int
    abstract fun writeCell(cell: TableCellWalker, cindex: Int, odsDebugTable: T)
    abstract fun getLayers(): List<List<TableModel<T>>>
}

class ColumnGroup<T>(
        val text: String,
        val children: MutableCollection<TableModel<T>>) : TableModel<T>() {
    override fun getLayers(): List<List<TableModel<T>>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val style: TableCellStyle? = null

    override val rowSpan: Int
        get() = children.sumBy { it.rowSpan }

    override fun writeCell(cell: TableCellWalker, cindex: Int, odsDebugTable: T) {
        cell.setStringValue(text)
        cell.setStyle(style)
        cell.setRowsSpanned(rowSpan)
    }
}

class VariableMulti<T>(
        val text: String,
        val columns: MutableCollection<ValueColumn<T>> = arrayListOf()) : TableModel<T>() {
    val style: TableCellStyle? = null

    override val rowSpan: Int
        get() = columns.size

    override fun writeCell(cell: TableCellWalker, cindex: Int, odsDebugTable: T) {
        cell.setStringValue(text)
        cell.setStyle(style)
        cell.setRowsSpanned(rowSpan)
    }
}
*/

abstract class ValueColumn<T>(val group: Array<String>,
                              val variableName: String = "") {
    fun getGroup(x: Int) = if (group.size <= x) "" else group[x]
    abstract fun writeCell(cell: TableCell, cindex: Int, row: TableRow, table: T)
}

class EmptyDebugColumn(group: Array<String>) : ValueColumn<ODSDebugTable>(group) {
    override fun writeCell(cell: TableCell, cindex: Int, row: TableRow, table: ODSDebugTable) {}
}

class RowIdDebugColumn(group: Array<String>) : ValueColumn<ODSDebugTable>(group) {
    override fun writeCell(cell: TableCell, cindex: Int, row: TableRow, table: ODSDebugTable) {
        cell.setStringValue(row.id)
        //cell.setStyle(cellStyle)
    }
}

class ValueDebugColumn(group: Array<String>,
                       private val programVar: ProgramVariable,
                       private val oracle: ValueOracle) : ValueColumn<ODSDebugTable>(group, programVar.name) {
    override fun writeCell(cell: TableCell, cindex: Int, row: TableRow, table: ODSDebugTable) {
        val constraint = row.rawFields[programVar]!!
        val first = constraint.chunk(0)
        when (first) { // try to find a constant in the first chunk
            is TestTableLanguageParser.ConstantFalseContext ->
                cell.setBooleanValue(false)
            is TestTableLanguageParser.ConstantTrueContext ->
                cell.setBooleanValue(true)
            is TestTableLanguageParser.ConstantIntContext -> {
                cell.setStringValue((first as TestTableLanguageParser.ConstantIntContext).text)
            }
            else -> {
                val dt = programVar.dataType
                when (dt) {
                    is AnyInt -> cell.setFloatValue(oracle.getInteger(dt))
                    is AnyBit.BOOL -> cell.setBooleanValue(oracle.getBoolean())
                    is EnumerateType -> cell.setStringValue(oracle.getEnumValue(dt))
                    else -> cell.setStringValue("no oracle for ${dt.name}")
                }
            }
        }
    }
}

class ConstraintDebugColumn(group: Array<String>,
                            private val programVar: ProgramVariable) : ValueColumn<ODSDebugTable>(group) {
    override fun writeCell(cell: TableCell, cindex: Int, row: TableRow, table: ODSDebugTable) {
        val constraint = row.rawFields[programVar]!!
        val fml = constraint.accept(table.formulaTblPrinter(programVar.name))
        println(fml)
        cell.setFormula(fml)
    }
}

class ProgramOutputDebugColumn(group: Array<String>,
                               val variable: SVariable,
                               val init: SAssignment,
                               val next: SAssignment)
    : ValueColumn<ODSDebugTable>(group, variable.name) {
    var firstRow = true
    override fun writeCell(cell: TableCell, cindex: Int, row: TableRow, table: ODSDebugTable) {
        val formula =
                if (firstRow)
                    init.expr.accept(table.formulaSmvPrinter())
                else
                    next.expr.accept(table.formulaSmvPrinter())
        cell.setFormula(formula)
        firstRow = false
    }
}

interface ValueOracle {
    fun getInteger(dt: AnyInt): Int
    fun getEnumValue(dt: EnumerateType): String
    fun getBoolean(): Boolean
}

object RandomValueOracle : ValueOracle {
    val random = Random(2432632525234)
    override fun getInteger(dt: AnyInt): Int = random.nextInt(dt.upperBound.toInt())
    override fun getEnumValue(dt: EnumerateType) =
            if (dt.allowedValues.isEmpty()) ""
            else {
                val values = dt.allowedValues.keys.toList()
                val pos = random.nextInt(
                        Math.max(dt.allowedValues.size - 1, 1))
                values[pos]
            }

    override fun getBoolean() = random.nextBoolean()
}

class ODSDebugTable(
        gtt: GeneralizedTestTable,
        private val categories: List<ValueColumn<ODSDebugTable>>,
        internal val unwinding: List<TableRow>,
        var tableStyle: TableStyle = DefaultTableStyle
) : Runnable, ODSTestTableWriter(gtt) {
    var programInputVariables = mutableSetOf<String>()

    var currentTable: Table = document.addTable(getUniqueName(),
            unwinding.size * 2, categories.size * 2)
    var currentRow = 1
    val headerRows: Int
        get() = (categories.minBy { it.group.size })?.group?.size ?: 0

    override fun run() {
        writeHeader()
        writeBody()
    }

    protected fun writeHeader() {
        for (r in 0 until headerRows) {
            val row = currentTable.nextRow()
            //val cell = row.walker
            val cat = categories.map { it.getGroup(r) }
            val runLengthEncoding = cat.runLengthEncoding()
            var count = 0
            for ((text, rspan) in runLengthEncoding) {
                val cell = row.getOrCreateCell(count)
                cell.setStringValue(text)
                //cell.setStyle(col.getCategoryStyle(cat))
                if (rspan - 1 >= 0)
                    cell.setColumnsSpanned(rspan - 1)
                count += rspan
            }
            ++currentRow
        }
    }

    protected fun writeBody() = unwinding.forEach { writeRow(it) }

    protected fun writeRow(trow: TableRow) {
        val row = currentTable.nextRow()
        val cell = row.walker
        categories.forEachIndexed { cindex, it ->
            it.writeCell(row.getOrCreateCell(cindex), cindex, trow, this)
//            cell.next()
        }
        ++currentRow
    }

    private val var2column: Map<String, Int> by lazy {
        categories.mapIndexed { i, c ->
            c.variableName to i
        }.toMap()
    }

    fun formulaSmvPrinter(): Smv2OdsFml = Smv2OdsFml(var2column, programInputVariables, currentRow)

    fun formulaTblPrinter(variable: String) = ODSFormulaPrinter(gtt, variable, currentRow, var2column)

    //fun getCurrentConstraint(programVar: ProgramVariable) =
    //        currentTableRow.rawFields[programVar]!!
}

private fun <T> Iterable<T>.runLengthEncoding(): List<Pair<T, Int>> {
    val ret = arrayListOf<Pair<T, Int>>()
    var cur: T? = null
    var len = 0
    for (elem in this) {
        if (cur == null) {
            cur = elem; len = 1; continue
        }
        if (cur == elem) {
            ++len
        } else {
            ret += cur to len
            cur = elem; len = 1;
        }
    }
    if (cur != null)
        ret.add(cur to len)
    return ret
}


class Smv2OdsFml(val var2column: Map<String, Int>,
                 val inputVariables: Set<String>,
                 val currentRow: Int)
    : SMVAstDefaultVisitorNN<String>() {
    override fun defaultVisit(top: SMVAst): String = ""
    override fun visit(v: SVariable): String {
        val free = v.name !in var2column
        val input = v.name in inputVariables
        val row = currentRow - (if (input) 0 else 1)
        return when {
            free -> v.name
            else -> ('A' + var2column[v.name]!!) + "" + row
        }
    }

    override fun visit(be: SBinaryExpression): String {
        val l = be.left.accept(this)
        val r = be.right.accept(this)
        return when (be.operator) {
            SBinaryOperator.PLUS -> "$l+$r"
            SBinaryOperator.MINUS -> "$l-$r"
            SBinaryOperator.DIV -> "$l/$r"
            SBinaryOperator.MUL -> "$l*$r"
            SBinaryOperator.AND -> "AND($l,$r)"
            SBinaryOperator.OR -> "AND($l,$r)"
            SBinaryOperator.LESS_THAN -> "$l<$r"
            SBinaryOperator.LESS_EQUAL -> "$l<=$r"
            SBinaryOperator.GREATER_THAN -> "$l>$r"
            SBinaryOperator.GREATER_EQUAL -> "$l>=$r"
            SBinaryOperator.XOR -> "XOR($l,$r)"
            SBinaryOperator.XNOR -> TODO()
            SBinaryOperator.EQUAL -> "$l=$r"
            SBinaryOperator.IMPL -> "OR(NOT($l),$r)"
            SBinaryOperator.EQUIV -> "$l=$r"
            SBinaryOperator.NOT_EQUAL -> "$l<>$r"
            SBinaryOperator.MOD -> "MOD($l,$r)"
            SBinaryOperator.SHL -> TODO()
            SBinaryOperator.SHR -> TODO()
            SBinaryOperator.WORD_CONCAT -> TODO()
        }

    }

    override fun visit(ue: SUnaryExpression): String {
        val e = ue.expr.accept(this)
        return when (ue.operator) {
            SUnaryOperator.NEGATE -> "NOT($e)"
            SUnaryOperator.MINUS -> "-($e)"
        }
    }

    override fun visit(l: SLiteral): String =
            when (l.dataType) {
                is EnumType -> '"' + l.value.toString() + '"'
                is SMVTypes.BOOLEAN -> {
                    if (l.value.toString().equals("TRUE", true))
                        "TRUE()" else "FALSE()"
                }
                else -> l.value.toString()
            }

    override fun visit(ce: SCaseExpression): String {
        val ret = StringBuffer()
        ce.cases.forEachIndexed { index, case ->
            val last = index - 1 == ce.cases.size
            val g = case.condition.accept(this)
            val t = case.then.accept(this)
            if (last) ret.append(t)
            else ret.append("IF(").append(g).append(";").append(t).append(";")
        }
        if (ce.cases.size > 1)
            ret.append(")".repeat(ce.cases.size - 1))
        return ret.toString()
    }
}

class ODSFormulaPrinter(
        val gtt: GeneralizedTestTable,
        val variable: String,
        val currentRow: Int,
        val var2column: Map<String, Int>)
    : TestTableLanguageBaseVisitor<String>() {
    override fun visitCell(ctx: TestTableLanguageParser.CellContext): String =
            ctx.chunk().joinToString("; ", "AND(", ")") { it.accept(this) }


    override fun visitCconstant(ctx: TestTableLanguageParser.CconstantContext): String {
        val constant = ctx.constant().accept(this)
        return constant + "=" + columnOf(variable, 0)
    }

    override fun visitCvariable(ctx: TestTableLanguageParser.CvariableContext): String {
        val v = ctx.variable().accept(this)
        return v + "=" + columnOf(variable, 0)
    }

    override fun visitDontcare(ctx: TestTableLanguageParser.DontcareContext) = "TRUE()"

    override fun visitConstantInt(ctx: TestTableLanguageParser.ConstantIntContext) = ctx.text

    override fun visitConstantTrue(ctx: TestTableLanguageParser.ConstantTrueContext?) = "TRUE()"

    override fun visitConstantFalse(ctx: TestTableLanguageParser.ConstantFalseContext?) = "FALSE()"

    override fun visitSinglesided(ctx: TestTableLanguageParser.SinglesidedContext): String {
        return columnOf(variable, 0) + ctx.relational_operator().text + ctx.expr().accept(this)
    }

    private fun columnOf(variable: String, i: Int): String =
            when {
                gtt.isProgramVariable(variable) ->
                    ('A' + var2column[variable]!!) + "" + (currentRow - i)
                gtt.isConstraintVariable(variable) -> variable
                else -> "\"$variable\"" //ENUM CONSTANT
            }

    override fun visitInterval(ctx: TestTableLanguageParser.IntervalContext): String =
            "AND(" + ctx.lower.accept(this) + "<=" + columnOf(variable, 0) + "; " +
                    columnOf(variable, 0) + ">=" + ctx.upper.accept(this) + ")"

    override fun visitMinus(ctx: TestTableLanguageParser.MinusContext) = "-" + ctx.expr().accept(this)

    override fun visitNegation(ctx: TestTableLanguageParser.NegationContext) = "NOT(" + ctx.expr().accept(this) + ")"


    override fun visitParens(ctx: TestTableLanguageParser.ParensContext) = "(" + ctx.expr().accept(this) + ")"

    override fun visitCompare(ctx: TestTableLanguageParser.CompareContext) = ctx.left.accept(this) + ctx.op.text + ctx.right.accept(this)

    override fun visitMod(ctx: TestTableLanguageParser.ModContext) = "MOD(" + ctx.left.accept(this) + "," + ctx.right.accept(this) + ")"

    override fun visitMult(ctx: TestTableLanguageParser.MultContext) = ctx.left.accept(this) + "*" + ctx.right.accept(this)

    override fun visitFunctioncall(ctx: TestTableLanguageParser.FunctioncallContext) = ctx.IDENTIFIER().text +
            ctx.expr().joinToString("; ", "(", ")")
            { it.accept(this) }

    override fun visitLogicalAnd(ctx: TestTableLanguageParser.LogicalAndContext) = ctx.left.accept(this) + "AND" + ctx.right.accept(this)

    override fun visitPlus(ctx: TestTableLanguageParser.PlusContext) = ctx.left.accept(this) + "+" + ctx.right.accept(this)

    override fun visitDiv(ctx: TestTableLanguageParser.DivContext) = " DIV(" + ctx.left.accept(this) + "," + ctx.right.accept(this) + ")"

    override fun visitInequality(ctx: TestTableLanguageParser.InequalityContext) = ctx.left.accept(this) + "<>" + ctx.right.accept(this)


    override fun visitLogicalXor(ctx: TestTableLanguageParser.LogicalXorContext) = "XOR(" + ctx.left.accept(this) + ";" + ctx.right.accept(this) + ")"

    override fun visitLogicalOr(ctx: TestTableLanguageParser.LogicalOrContext) = "OR( " + ctx.left.accept(this) + "; " + ctx.right.accept(this) + ")"

    override fun visitEquality(ctx: TestTableLanguageParser.EqualityContext) = ctx.left.accept(this) + "=" + ctx.right.accept(this)

    override fun visitSubstract(ctx: TestTableLanguageParser.SubstractContext) = ctx.left.accept(this) + "-" + ctx.right.accept(this)

    override fun visitVariable(ctx: TestTableLanguageParser.VariableContext) = columnOf(ctx.IDENTIFIER().text, if (ctx.INTEGER() != null) ctx.INTEGER().text.toInt() else 0)

    override fun visitGuardedcommand(ctx: TestTableLanguageParser.GuardedcommandContext): String {
        val ret = StringBuffer()
        var i = 0
        while (i <= ctx.expr().lastIndex) {
            val g = ctx.expr(i).accept(this)
            val t = ctx.expr(i + 1).accept(this)
            ret.append("IF(")
                    .append(g).append(";")
                    .append(t).append(";")
            i += 2
        }
        ret.append("FALSE").append(")".repeat(i / 2))
        return ret.toString()
    }
}

interface TableStyle {
    val styleRowId: TableCellStyle
    val styleRowIdHeader: TableCellStyle
    val styleCategoryHeader: TableCellStyle
    val stylePauseVariableHeader: TableCellStyle
    val styleOutputVariableHeader: TableCellStyle
    val styleInputVariableHeader: TableCellStyle
    val styleInputValue: TableCellStyle
    val styleOutputValue: TableCellStyle
}

object DefaultTableStyle : TableStyle {
    val EMPTY = TableCellStyle.DEFAULT_CELL_STYLE
    override var styleRowId: TableCellStyle = EMPTY
    override var stylePauseVariableHeader: TableCellStyle = EMPTY

    val intStyle: DataStyle = createFloatStyleBuilder("custom-int-datastyle", Locale.getDefault())
            .decimalPlaces(8).groupThousands(true).build()


    var styleValues = TableCellStyle.builder("values")
            .build()
    override var styleOutputValue = TableCellStyle.builder("values output")
            .parentCellStyle(styleValues).build()
    override val styleInputValue = TableCellStyle.builder("values input")
            .parentCellStyle(styleValues).build()

    override var styleCategoryHeader = TableCellStyle.builder("category header")
            .backgroundColor(SColor("#ff00ff"))
            .fontWeightBold()
            .textAlign(TableCellStyle.Align.CENTER)
            .build()

    class SColor(val s: String) : Color {
        override fun hexValue() = s
    }

    var styleVariableHeader = TableCellStyle.builder("variable header")
            .backgroundColor(SColor("#cccccc"))
            .fontWeightBold()
            .textAlign(TableCellStyle.Align.CENTER)
            .borderBottom(SimpleLength.pt(1.0), SimpleColor.BLACK, BorderAttribute.Style.SOLID)
            .build()

    override val styleInputVariableHeader: TableCellStyle = TableCellStyle.builder("variable input header")
            .parentCellStyle(styleVariableHeader)
            .build()

    override val styleOutputVariableHeader = TableCellStyle.builder("variable output header")
            .parentCellStyle(styleVariableHeader)
            .build()

    override val styleRowIdHeader: TableCellStyle = TableCellStyle.builder("variable rowid header")
            .parentCellStyle(styleVariableHeader)
            .build()

    val styleRowTimeHeader: TableCellStyle = TableCellStyle.builder("variable time header")
            .parentCellStyle(styleVariableHeader)
            .build()
}
