package edu.kit.iti.formal.automation.st

import edu.kit.iti.formal.automation.IEC61131Facade
import edu.kit.iti.formal.automation.VariableScope
import edu.kit.iti.formal.automation.datatypes.AnyDt
import edu.kit.iti.formal.automation.datatypes.IECString
import edu.kit.iti.formal.automation.datatypes.values.ReferenceValue
import edu.kit.iti.formal.automation.datatypes.values.Value
import edu.kit.iti.formal.automation.il.IlPrinter
import edu.kit.iti.formal.automation.operators.Operator
import edu.kit.iti.formal.automation.operators.UnaryOperator
import edu.kit.iti.formal.automation.scope.Scope
import edu.kit.iti.formal.automation.st.ast.*
import edu.kit.iti.formal.automation.st.util.AstVisitor
import edu.kit.iti.formal.util.CodeWriter
import edu.kit.iti.formal.util.joinInto
import java.util.*

/**
 * Created by weigla on 15.06.2014.
 *
 * @author weigl, Augusto Modanese
 * @version $Id: $Id
 */
open class StructuredTextPrinter(var sb: CodeWriter = CodeWriter()) : AstVisitor<Unit>() {
    private val literals: StringLiterals = SL_ST
    var bodyPrintingOrder = listOf(BodyPrinting.ST, BodyPrinting.SFC, BodyPrinting.IL)
    var isPrintComments: Boolean = false

    val string: String
        get() = sb.toString()


    override fun defaultVisit(obj: Any) {
        throw IllegalArgumentException("not implemented: " + obj::class.java)
    }

    override fun visit(blockStatement: BlockStatement) {
        sb.nl().print("//! REGION ${blockStatement.name}")
        blockParam(blockStatement.state, "[", "]")
        blockParam(blockStatement.input, "(", ")")
        sb.write(" => ")
        blockParam(blockStatement.output, "(", ")")
        sb.increaseIndent()
        blockStatement.statements.accept(this)
        sb.decreaseIndent()
        sb.nl().print("//! END_REGION")
    }

    private fun blockParam(p: MutableList<SymbolicReference>, pre: String, suf: String) =
            p.joinInto(sb, ", ", pre, suf) {
                it.accept(this@StructuredTextPrinter)
            }


    override fun visit(empty: EMPTY_EXPRESSION) {
        sb.print("(* empty expression *)")
    }

    override fun visit(arrayTypeDeclaration: ArrayTypeDeclaration) {
        sb.printf("ARRAY[")
        arrayTypeDeclaration.ranges.forEachIndexed { i, it ->
            it.start.accept(this)
            sb.printf("..")
            it.stop.accept(this)
            if (i < arrayTypeDeclaration.ranges.size - 1)
                sb.printf(", ")
        }
        sb.printf("] OF ")
        sb.printf(arrayTypeDeclaration.baseType.identifier ?: "<missing>")
    }

    override fun visit(stringTypeDeclaration: StringTypeDeclaration) {
        sb.printf("STRING")
    }

    override fun visit(elements: PouElements) {
        elements.forEach { it.accept(this) }
    }


    override fun visit(exitStatement: ExitStatement) {
        sb.printf(literals.exit()).printf(literals.statement_separator())

    }


    override fun visit(integerCondition: CaseCondition.IntegerCondition) {
        sb.appendIdent()
        integerCondition.value.accept(this)

    }


    override fun visit(enumeration: CaseCondition.Enumeration) {
        if (enumeration.start == enumeration.stop) {
            enumeration.start.accept(this)
        } else {
            enumeration.start.accept(this)
            sb.printf("..")
            enumeration.stop!!.accept(this)
        }


    }


    override fun visit(binaryExpression: BinaryExpression) {
        sb.append('(')
        binaryExpression.leftExpr.accept(this)
        sb.printf(" ").printf(literals.operator(binaryExpression.operator)).printf(" ")
        binaryExpression.rightExpr.accept(this)
        sb.append(')')

    }


    override fun visit(assignmentStatement: AssignmentStatement) {
        sb.nl()
        assignmentStatement.location.accept(this)
        if (assignmentStatement.isAssignmentAttempt)
            sb.printf(literals.assignmentAttempt())
        else
            sb.printf(literals.assign())
        assignmentStatement.expression.accept(this)
        sb.printf(";")
    }


    override fun visit(configurationDeclaration: ConfigurationDeclaration) {

    }


    override fun visit(enumerationTypeDeclaration: EnumerationTypeDeclaration) {
        sb.nl().printf(enumerationTypeDeclaration.name).printf(" : ")
        enumerationTypeDeclaration.allowedValues.joinTo(sb, ", ", "(", ");")
    }

    override fun visit(init: IdentifierInitializer) {
        sb.printf(init.value!!)

    }


    override fun visit(repeatStatement: RepeatStatement) {
        sb.nl()
        sb.printf("REPEAT").increaseIndent()
        repeatStatement.statements.accept(this)

        sb.decreaseIndent().nl().printf("UNTIL ")
        repeatStatement.condition.accept(this)
        sb.printf("END_REPEAT")

    }


    override fun visit(whileStatement: WhileStatement) {
        sb.nl()
        sb.printf("WHILE ")
        whileStatement.condition.accept(this)
        sb.printf(" DO ").increaseIndent()
        whileStatement.statements.accept(this)
        sb.decreaseIndent().nl()
        sb.printf("END_WHILE")

    }


    override fun visit(unaryExpression: UnaryExpression) {
        sb.printf(literals.operator(unaryExpression.operator)!!).printf(" ")
        unaryExpression.expression.accept(this)

    }


    override fun visit(typeDeclarations: TypeDeclarations) {

        if (typeDeclarations.size > 0) {
            sb.printf("TYPE ").increaseIndent()
            for (decl in typeDeclarations) {
                decl.accept(this)
            }
            sb.decreaseIndent().nl().printf("END_TYPE").nl().nl()
        }

    }

    override fun visit(caseStatement: CaseStatement) {
        sb.nl().printf("CASE ")
        caseStatement.expression.accept(this)
        sb.printf(" OF ").increaseIndent()

        for (c in caseStatement.cases) {
            c.accept(this)
            sb.nl()
        }

        if (caseStatement.elseCase!!.size > 0) {
            sb.nl().printf("ELSE ")
            caseStatement.elseCase!!.accept(this)
        }

        sb.nl().decreaseIndent().appendIdent().printf("END_CASE;")

    }

    override fun visit(symbolicReference: SymbolicReference) {
        sb.printf(quoteIdentifier(symbolicReference.identifier))

        for (i in 0 until symbolicReference.derefCount)
            sb.printf("^")

        if (symbolicReference.subscripts != null && !symbolicReference.subscripts!!.isEmpty()) {
            symbolicReference.subscripts!!.joinInto(sb, ", ", "[", "]") { it.accept(this) }
        }

        if (symbolicReference.sub != null) {
            sb.printf(".")
            symbolicReference.sub!!.accept(this)
        }


    }

    val QUOTED_IDENTIFIER = listOf("STEP", "END_STEP", "TRANSITION", "END_TRANSITION", "INITIAL_STEP", "FROM")
    private fun quoteIdentifier(identifier: String): String {
        return if (identifier.toUpperCase() in QUOTED_IDENTIFIER) {
            "`$identifier`"
        } else {
            identifier
        }
    }


    override fun visit(statements: StatementList) {
        for (stmt in statements) {
            stmt.accept(this)
        }

    }


    override fun visit(programDeclaration: ProgramDeclaration) {
        printComment(programDeclaration.comment)
        sb.printf("PROGRAM ").printf(programDeclaration.name).increaseIndent()
        programDeclaration.scope.accept(this)

        sb.nl()

        if (!programDeclaration.actions.isEmpty()) {
            programDeclaration.actions.forEach { v -> v.accept(this) }
            sb.nl()
        }

        printBody(programDeclaration)

        sb.decreaseIndent().nl().printf("END_PROGRAM").nl()
    }


    override fun visit(expressions: ExpressionList) {
        expressions.joinInto(sb) { it.accept(this) }
    }


    override fun visit(invocation: Invocation) {
        invocation.callee.accept(this)
        visitInvocationParameter(invocation.parameters)
    }

    private fun visitInvocationParameter(parameters: MutableList<InvocationParameter>) {
        parameters.joinInto(sb, ", ", "(", ")") {
            if (it.name != null) {
                sb.printf(it.name!!)
                if (it.isOutput)
                    sb.printf(" => ")
                else
                    sb.printf(" := ")
            }
            it.expression.accept(this)
        }
    }


    override fun visit(forStatement: ForStatement) {
        sb.nl()
        sb.printf("FOR ").printf(forStatement.variable)
        sb.printf(" := ")
        forStatement.start.accept(this)
        sb.printf(" TO ")
        forStatement.stop.accept(this)
        sb.printf(" DO ").increaseIndent()
        forStatement.statements.accept(this)
        sb.decreaseIndent().nl()
        sb.printf("END_FOR")

    }


    override fun visit(functionBlockDeclaration: FunctionBlockDeclaration) {
        printComment(functionBlockDeclaration.comment)
        sb.printf("FUNCTION_BLOCK ")
        if (functionBlockDeclaration.isFinal)
            sb.printf("FINAL ")
        if (functionBlockDeclaration.isAbstract)
            sb.printf("ABSTRACT ")

        sb.printf(functionBlockDeclaration.name)

        if (functionBlockDeclaration.parent.identifier != null) {
            sb.printf(" EXTENDS ").printf(functionBlockDeclaration.parent.identifier!!)
        }

        if (functionBlockDeclaration.interfaces.isNotEmpty()) {
            val interf = functionBlockDeclaration.interfaces
                    .map { it.identifier!! }
                    .joinToString(", ")
            sb.printf(" IMPLEMENTS ").printf(interf)
        }
        sb.increaseIndent().nl()
        functionBlockDeclaration.scope.accept(this)
        sb.nl()

        if (functionBlockDeclaration.methods.isNotEmpty()) {
            functionBlockDeclaration.methods.forEach { it.accept(this) }
            sb.nl()
        }

        if (!functionBlockDeclaration.actions.isEmpty()) {
            functionBlockDeclaration.actions.forEach { v -> v.accept(this) }
        }

        printBody(functionBlockDeclaration)

        sb.decreaseIndent().nl().printf("END_FUNCTION_BLOCK").nl().nl()

    }

    private fun printComment(comment: String) {
        if (comment.isNotBlank()) {
            sb.printf(literals.comment_open())
            sb.printf(comment)
            sb.printf(literals.comment_close() + "\n")
        }
    }

    override fun visit(interfaceDeclaration: InterfaceDeclaration) {
        sb.printf("INTERFACE ").printf(interfaceDeclaration.name)

        val extendsInterfaces = interfaceDeclaration.interfaces.map { it.identifier }
        if (!extendsInterfaces.isEmpty())
            sb.printf(" EXTENDS ").print(extendsInterfaces)

        sb.increaseIndent().nl()

        //interfaceDeclaration.scope.accept(this)

        interfaceDeclaration.methods.forEach { m -> m.accept(this) }

        sb.decreaseIndent().nl().printf("END_INTERFACE").nl().nl()

    }

    override fun visit(clazz: ClassDeclaration) {
        printComment(clazz.comment)
        sb.printf("CLASS ")

        if (clazz.isFinal)
            sb.printf("FINAL ")
        if (clazz.isAbstract)
            sb.printf("ABSTRACT ")

        sb.printf(clazz.name)

        val parent = clazz.parent.identifier
        if (parent != null)
            sb.printf(" EXTENDS ").printf(parent)

        val interfaces = clazz.interfaces.map { it.identifier }
        if (!interfaces.isEmpty())
            sb.printf(" IMPLEMENTS ").printf(interfaces.joinToString(","))

        sb.increaseIndent().nl()

        clazz.scope.accept(this)

        clazz.methods.forEach { m -> m.accept(this) }

        sb.decreaseIndent().nl().printf("END_CLASS").nl().nl()

    }

    override fun visit(method: MethodDeclaration) {
        sb.printf("METHOD ")

        if (method.isFinal)
            sb.printf("FINAL ")
        if (method.isAbstract)
            sb.printf("ABSTRACT ")
        if (method.isOverride)
            sb.printf("OVERRIDE ")

        sb.printf(method.accessSpecifier.toString() + " ")

        sb.printf(method.name)

        val returnType = method.returnTypeName
        if (!returnType!!.isEmpty())
            sb.printf(" : $returnType")

        sb.increaseIndent().nl()

        method.scope.accept(this)

        method.stBody.accept(this)

        sb.decreaseIndent().nl().printf("END_METHOD").nl().nl()

    }

    override fun visit(functionDeclaration: FunctionDeclaration) {
        printComment(functionDeclaration.comment)
        sb.printf("FUNCTION ").printf(functionDeclaration.name)

        val returnType = functionDeclaration.returnType.identifier
        if (!(returnType == null || returnType.isEmpty()))
            sb.printf(" : $returnType")

        sb.increaseIndent().nl()

        functionDeclaration.scope.accept(this)

        printBody(functionDeclaration)

        sb.decreaseIndent().nl().printf("END_FUNCTION").nl().nl()

    }

    override fun visit(gvlDecl: GlobalVariableListDeclaration) {
        gvlDecl.scope.accept(this)
        sb.nl()

    }

    override fun visit(referenceSpecification: ReferenceTypeDeclaration) {
        sb.printf("REF_TO ")
        referenceSpecification.refTo.accept(this)
    }

    override fun visit(referenceValue: ReferenceValue) {
        sb.printf("REF(")
        referenceValue.referenceTo.accept(this)
        sb.printf(")")

    }


    override fun visit(returnStatement: ReturnStatement) {
        sb.nl().printf("RETURN;")
    }


    override fun visit(ifStatement: IfStatement) {
        for (i in 0 until ifStatement.conditionalBranches.size) {
            sb.nl()

            if (i == 0)
                sb.printf("IF ")
            else
                sb.printf("ELSIF ")

            ifStatement.conditionalBranches[i].condition.accept(this)

            sb.printf(" THEN").increaseIndent()
            ifStatement.conditionalBranches[i].statements.accept(this)
            sb.decreaseIndent()
        }

        if (ifStatement.elseBranch.size > 0) {
            sb.nl().printf("ELSE").increaseIndent()
            ifStatement.elseBranch.accept(this)
            sb.decreaseIndent()
        }
        sb.nl().printf("END_IF")

    }

    override fun visit(actionDeclaration: ActionDeclaration) {
        sb.nl().printf("ACTION ").printf(actionDeclaration.name).increaseIndent()
        printBody(actionDeclaration)
        sb.decreaseIndent().nl().printf("END_ACTION")

    }


    override fun visit(invocation: InvocationStatement) {
        sb.nl()
        invocation.callee.accept(this)
        visitInvocationParameter(invocation.parameters)
        sb.printf(";")
    }


    override fun visit(aCase: Case) {
        sb.nl()
        aCase.conditions.joinInto(sb) { it.accept(this) }
        sb.printf(":")
        sb.block() {
            aCase.statements.accept(this@StructuredTextPrinter)
        }
    }


    override fun visit(simpleTypeDeclaration: SimpleTypeDeclaration) {
        sb.printf(simpleTypeDeclaration.baseType.identifier!!)
        /*if (simpleTypeDeclaration.initialization != null) {
            sb.printf(" := ")
            simpleTypeDeclaration.initialization!!.accept(this)
        }*/
    }

    override fun visit(structureTypeDeclaration: StructureTypeDeclaration) {
        sb.printf(structureTypeDeclaration.name)
        sb.printf(": STRUCT").nl().increaseIndent()
        structureTypeDeclaration.fields.forEach { it ->
            sb.nl()
            it.accept(this)
        }
        sb.decreaseIndent().printf("END_STRUCT;").nl()

    }

    override fun visit(subRangeTypeDeclaration: SubRangeTypeDeclaration) {
        sb.printf(subRangeTypeDeclaration.name)
        sb.printf(": ").printf(subRangeTypeDeclaration.baseType.identifier!!)
        sb.printf("(")
        subRangeTypeDeclaration.range!!.start.accept(this)
        sb.printf(" .. ")
        subRangeTypeDeclaration.range!!.stop.accept(this)
        sb.printf(")")
        /*if (subRangeTypeDeclaration.initialization != null) {
            sb.printf(" := ")
            subRangeTypeDeclaration.initialization!!.accept(this)
        }*/
        sb.printf(";")

    }

    val variableDeclarationUseDataType = false
    private fun variableDataType(vd: VariableDeclaration) {
        val dt = vd.dataType
        if (variableDeclarationUseDataType && dt != null) {
            variableDataType(dt)
        } else {
            vd.typeDeclaration?.accept(this)
        }
    }

    fun variableDataType(dt: AnyDt) {
        sb.printf(dt.reprDecl())
    }


    override fun visit(commentStatement: CommentStatement) {
        if (isPrintComments) {
            sb.nl()
            if ('\n' in commentStatement.comment) {
                sb.printf(literals.comment_open())
                sb.printf(commentStatement.comment)
                sb.printf(literals.comment_close())
            } else {
                sb.printf("//%s\n", commentStatement.comment)
            }
        }
    }


    override fun visit(literal: Literal) {
        fun print(prefix: Any?, suffix: Any) =
                (if (prefix != null) "$prefix#" else "") + suffix

        sb.printf(when (literal) {
            is IntegerLit -> print(literal.dataType.obj?.name, literal.value.abs())
            is RealLit -> print(literal.dataType.obj?.name, literal.value.abs())
            is EnumLit -> print(literal.dataType.obj?.name, literal.value)
            is ToDLit -> {
                val (h, m, s, ms) = literal.value
                print(literal.dataType().name, "$h:$m:$s.$ms")
            }
            is DateLit -> {
                val (y, m, d) = literal.value
                print(literal.dataType().name, "$y-$m-$d")
            }
            is DateAndTimeLit -> {
                val (y, mo, d) = literal.value.date
                val (h, m, s, ms) = literal.value.tod
                print(literal.dataType().name, "$y-$mo-$d-$h:$m:$s.$ms")
            }
            is StringLit -> {
                if (literal.dataType() is IECString.WSTRING) "\"${literal.value}\""
                else "'${literal.value}'"
            }
            is NullLit -> "null"
            is TimeLit -> {
                print(literal.dataType().name, "${literal.value.milliseconds}ms")
            }
            is BooleanLit -> literal.value.toString().toUpperCase()
            is BitLit -> {
                print(literal.dataType.obj?.name, "2#" + literal.value.toString(2))
            }
            is UnindentifiedLit -> literal.value
        })
    }

    override fun visit(arrayinit: ArrayInitialization) {
        arrayinit.initValues.joinInto(sb, ", ", "[", "]") {
            it.accept(this)
        }
    }

    override fun visit(localScope: Scope) {
        val variables = VariableScope(localScope.variables)
        variables.groupBy { it.type }
                .forEach { (type, v) ->
                    val vars = v.toMutableList()
                    vars.sortWith(compareBy { it.name })

                    //By { a, b -> a.compareTo(b) }
                    sb.nl().printf("VAR")

                    if (VariableDeclaration.INPUT and type >= VariableDeclaration.INOUT) {
                        sb.printf("_INOUT")
                    } else {
                        when {
                            VariableDeclaration.INPUT and type != 0 -> sb.printf("_INPUT")
                            VariableDeclaration.OUTPUT and type != 0 -> sb.printf("_OUTPUT")
                            VariableDeclaration.EXTERNAL and type != 0 -> sb.printf("_EXTERNAL")
                            VariableDeclaration.GLOBAL and type != 0 -> sb.printf("_GLOBAL")
                            VariableDeclaration.TEMP and type != 0 -> sb.printf("_TEMP")
                        }
                    }
                    sb.printf(" ")
                    if (VariableDeclaration.CONSTANT and type != 0)
                        sb.printf("CONSTANT ")
                    if (VariableDeclaration.RETAIN and type != 0)
                        sb.printf("RETAIN ")
                    sb.printf(" ")
                    //sb.printf(type)

                    sb.increaseIndent()
                    for (vd in vars) {
                        print(vd)
                    }
                    sb.decreaseIndent().nl().printf("END_VAR")
                    sb.nl()
                }
    }

    open fun print(vd: VariableDeclaration) {
        sb.nl()
        sb.printf(vd.name).printf(" : ")
        variableDataType(vd)
        when {
            vd.initValue != null -> {
                sb.printf(" := ")
                val (dt, v) = vd.initValue as Value<*, *>
                sb.printf(dt.repr(v))
            }
            vd.init != null -> {
                sb.printf(" := ")
                vd.init!!.accept(this)
            }
        }
        sb.printf(";")
    }

    override fun visit(structureInitialization: StructureInitialization) {
        structureInitialization.initValues.joinInto(sb, ", ", "(", ")")
        { t, v ->
            sb.printf(t).printf(" := ")
            v.accept(this)
        }

    }

    override fun visit(sfcStep: SFCStep) {
        sb.nl().printf(if (sfcStep.isInitial) "INITIAL_STEP " else "STEP ")
        sb.printf(sfcStep.name).printf(":").increaseIndent()
        sfcStep.events.forEach { aa -> visit(aa) }
        sb.decreaseIndent().nl()
        sb.printf("END_STEP").nl()

    }

    private fun visit(aa: SFCStep.AssociatedAction) {
        sb.nl().printf(aa.actionName).append('(').append(aa.qualifier!!.qualifier.symbol)
        if (aa.qualifier!!.qualifier.hasTime) {
            sb.printf(", ")
            aa.qualifier!!.time.accept(this)
        }
        sb.printf(");")
    }

    override fun visit(sfcNetwork: SFCNetwork) {
        val seq = ArrayList(sfcNetwork.steps)
        seq.sortWith(compareBy(SFCStep::isInitial).thenBy(SFCStep::name))
        seq.forEach { a -> a.accept(this) }
        sfcNetwork.steps.stream()
                .flatMap { s -> s.incoming.stream() }
                .forEachOrdered { t -> t.accept(this) }

    }

    override fun visit(sfc: SFCImplementation) {
        //sfc.actions.forEach { a -> a.accept(this) }
        sfc.networks.forEach { n -> n.accept(this) }
    }

    override fun visit(transition: SFCTransition) {
        val f = transition.from.map { it.name }.reduce { a, b -> "$a, $b" }
        val t = transition.to.map { it.name }.reduce { a, b -> "$a, $b" }

        sb.nl().printf("TRANSITION FROM ")

        if (transition.from.size > 1) {
            sb.append('(').append(f).append(')')
        } else {
            sb.printf(f)
        }
        sb.printf(" TO ")
        if (transition.to.size > 1) {
            sb.append('(').append(t).append(')')
        } else {
            sb.printf(t)
        }
        sb.printf(" := ")

        transition.guard.accept(this)
        sb.printf(";").printf(" END_TRANSITION")

    }

    private fun printBody(a: HasBody) {
        val stBody = a.stBody
        val sfcBody = a.sfcBody
        val ilBody = a.ilBody

        loop@ for (type in bodyPrintingOrder) {
            when (type) {
                BodyPrinting.ST -> stBody?.accept(this) ?: continue@loop
                BodyPrinting.SFC -> sfcBody?.accept(this) ?: continue@loop
                BodyPrinting.IL -> ilBody?.accept(IlPrinter(sb)) ?: continue@loop
            }
            break@loop
        }
    }

    /**
     *
     * clear.
     */
    fun clear() {
        sb = CodeWriter()
    }

    enum class BodyPrinting {
        ST, SFC, IL
    }

    open class StringLiterals {
        open fun operator(operator: Operator?): String {
            return operator!!.symbol
        }

        fun assign(): String {
            return " := "
        }

        fun assignmentAttempt(): String {
            return " ?= "
        }

        fun statement_separator(): String {
            return ";"
        }

        fun exit(): String {
            return "EXIT"
        }

        open fun operator(operator: UnaryOperator): String? {
            return operator.symbol
        }

        fun comment_close(): String {
            return " *)"
        }

        fun comment_open(): String {
            return "(* "
        }

        fun repr(sv: Value<*, *>): String {
            return sv.dataType.repr(sv.value)
        }

        companion object {
            fun create(): StringLiterals {
                return StringLiterals()
            }
        }
    }

    companion object {
        /**
         * Constant `SL_ST`
         */
        var SL_ST = StringLiterals.create()

        fun print(astNode: Top): String {
            val p = StructuredTextPrinter()
            astNode.accept(p)
            return p.string
        }
    }

    override fun visit(jump: JumpStatement) {
        sb.nl().write("JMP ${jump.target};")
    }

    override fun visit(label: LabelStatement) {
        sb.nl().write("${label.label}:")
    }

    override fun visit(special: SpecialStatement) {
        when (special) {
            is SpecialStatement.Assert -> {
                sb.nl().write("//# assert ")
                special.name?.let { sb.write(": $it") }
                special.exprs.joinInto(sb, separator = ", ") { it.accept(this) }
            }
            is SpecialStatement.Assume -> {
                sb.nl().write("//# assume ")
                special.name?.let { sb.write(": $it") }
                special.exprs.joinInto(sb, separator = ", ") { it.accept(this) }
            }
            is SpecialStatement.Havoc -> {
                sb.nl().write("//# havoc ")
                special.name?.let { sb.write(": $it") }
                special.variables.joinInto(sb, separator = ", ") { it.accept(this) }
            }
            else -> sb.nl().write("// special statement of type $special not supported")
        }
    }
}

