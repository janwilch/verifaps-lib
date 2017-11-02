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

import edu.kit.iti.formal.automation.datatypes.Any;
import edu.kit.iti.formal.automation.exceptions.VariableNotDefinedException;
import edu.kit.iti.formal.automation.scope.LocalScope;
import edu.kit.iti.formal.automation.visitors.Utils;
import edu.kit.iti.formal.automation.visitors.Visitor;
import lombok.Data;

/**
 * Created by weigl on 11.06.14.
 *
 * @author weigl
 * @version $Id: $Id
 */
@Data
public class SymbolicReference extends Reference {
    private String identifier;
    private ExpressionList subscripts;
    private Reference sub;
    private Any dataType;

    /**
     * Number of times reference is dereferenced.
     */
    private int derefCount = 0;

    /**
     * <p>Constructor for SymbolicReference.</p>
     *
     * @param s   a {@link java.lang.String} object.
     * @param sub a {@link edu.kit.iti.formal.automation.st.ast.Reference} object.
     */
    public SymbolicReference(String s, Reference sub) {
        if (s == null)
            throw new IllegalArgumentException();
        this.sub = sub;
        identifier = s;
    }

    /**
     * <p>Constructor for SymbolicReference.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public SymbolicReference(String s) {
        this(s, null);
    }

    /**
     * <p>Constructor for SymbolicReference.</p>
     *
     * @param symbolicReference a {@link edu.kit.iti.formal.automation.st.ast.SymbolicReference} object.
     */
    public SymbolicReference(SymbolicReference symbolicReference) {
        this.identifier = symbolicReference.identifier;
        this.subscripts = symbolicReference.getSubscripts();
        this.sub = symbolicReference.sub;
        if (this.identifier == null)
            throw new IllegalArgumentException();
    }

    /**
     * <p>Constructor for SymbolicReference.</p>
     */
    public SymbolicReference() {

    }

    /**
     * <p>addSubscript.</p>
     *
     * @param ast a {@link edu.kit.iti.formal.automation.st.ast.Expression} object.
     */
    public void addSubscript(Expression ast) {
        subscripts.add(ast);
    }

    /**
     * <p>Getter for the field <code>identifier</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * <p>Setter for the field <code>identifier</code>.</p>
     *
     * @param identifier a {@link java.lang.String} object.
     */
    public void setIdentifier(String identifier) {
        if (identifier == null)
            throw new IllegalArgumentException();
        this.identifier = identifier;
    }

    /**
     * <p>Getter for the field <code>subscripts</code>.</p>
     *
     * @return a {@link edu.kit.iti.formal.automation.st.ast.ExpressionList} object.
     */
    public ExpressionList getSubscripts() {
        return subscripts;
    }

    /**
     * <p>Setter for the field <code>subscripts</code>.</p>
     *
     * @param subscripts a {@link edu.kit.iti.formal.automation.st.ast.ExpressionList} object.
     */
    public void setSubscripts(ExpressionList subscripts) {
        this.subscripts = subscripts;
    }

    /**
     * <p>Getter for the field <code>sub</code>.</p>
     *
     * @return a {@link edu.kit.iti.formal.automation.st.ast.Reference} object.
     */
    public Reference getSub() {
        return sub;
    }

    /**
     * <p>Setter for the field <code>sub</code>.</p>
     *
     * @param sub a {@link edu.kit.iti.formal.automation.st.ast.Reference} object.
     */
    public void setSub(Reference sub) {
        this.sub = sub;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Any dataType(LocalScope localScope)
            throws VariableNotDefinedException {
        return localScope.getVariable(this).getDataType();
    }

    @Override
    public SymbolicReference copy() {
        SymbolicReference sr = new SymbolicReference();
        sr.setRuleContext(getRuleContext());
        sr.identifier = identifier;
        sr.subscripts = Utils.copyNull(subscripts);
        sr.sub = Utils.copyNull(sub);
        sr.derefCount = derefCount;
        return sr;
    }
}
