package edu.kit.iti.formal.automation.plcopenxml;

/*-
 * #%L
 * iec-xml
 * %%
 * Copyright (C) 2017 Alexander Weigl
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

import edu.kit.iti.formal.automation.IEC61131Facade;
import edu.kit.iti.formal.automation.scope.Scope;
import edu.kit.iti.formal.automation.st.ast.*;
import org.jdom2.Element;

/**
 * @author Alexander Weigl
 * @version 1 (30.05.17)
 */
public class STBuilder extends DefaultPOUBuilder {
    public STBuilder(Element element) {
        super(element);
    }

    @Override
    public TopLevelElements build() {
        TopLevelScopeElement topLevelElement = null;
        Scope scope = parseInterface();
        String name = element.getAttributeValue("name");
        String code = element.getChild("body").getChild("ST").getChildText("xhtml");
        StatementList body = IEC61131Facade.statements(code);

        switch (element.getAttributeValue("pouType")) {
            case "program":
                ProgramDeclaration pd = new ProgramDeclaration();
                pd.setStBody(body);
                pd.setProgramName(name);
                pd.setScope(scope);
                return TopLevelElements.singleton(pd);
            default:
                FunctionBlockDeclaration fbd = new FunctionBlockDeclaration();
                fbd.setStBody(body);
                fbd.setName(name);
                fbd.setScope(scope);
                return TopLevelElements.singleton(fbd);
        }
    }
}
