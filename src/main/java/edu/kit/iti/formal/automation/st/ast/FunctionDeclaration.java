package edu.kit.iti.formal.automation.st.ast;

import edu.kit.iti.formal.automation.datatypes.Any;
import edu.kit.iti.formal.automation.visitors.Visitor;

/**
 * Created by weigl on 13.06.14.
 */
public class FunctionDeclaration extends TopLevelScopeElement {
    private String functionName;
    private String returnTypeName;
    private Any returnType;
    private StatementList statements = new StatementList();

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getReturnTypeName() {
        return returnTypeName;
    }

    public void setReturnTypeName(String returnType) {
        this.returnTypeName = returnType;
    }

    public StatementList getStatements() {
        return statements;
    }

    public void setStatements(StatementList statements) {
        this.statements = statements;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getBlockName() {
        return getFunctionName();
    }
}
