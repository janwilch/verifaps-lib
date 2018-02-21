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

import edu.kit.iti.formal.automation.sfclang.ast.ActionDeclaration;
import edu.kit.iti.formal.automation.sfclang.ast.SFCImplementation;
import edu.kit.iti.formal.automation.st.Identifiable;
import edu.kit.iti.formal.automation.visitors.Visitor;
import lombok.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by weigl on 13.06.14.
 *
 * @author weigl
 * @version $Id: $Id
 */
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProgramDeclaration extends TopLevelScopeElement
        implements Identifiable {
    private StatementList stBody;
    private String programName;
    private SFCImplementation sfcBody;
    private Map<String, ActionDeclaration> actions = new LinkedHashMap<>();

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
    public String getIdentifier() {
        return getProgramName();
    }

    public ProgramDeclaration copy() {
        ProgramDeclaration pd = new ProgramDeclaration();
        pd.setRuleContext(getRuleContext());
        pd.setScope(getScope().copy());
        pd.programName = programName;

        if (stBody != null)
            pd.stBody = stBody.copy();

        if (sfcBody != null)
            pd.sfcBody = sfcBody.copy();

        actions.forEach((k, v) -> pd.getActions().put(k, v.copy()));
        return pd;
    }

    public void addAction(ActionDeclaration act) {
        actions.put(act.getName(), act);
    }
}
