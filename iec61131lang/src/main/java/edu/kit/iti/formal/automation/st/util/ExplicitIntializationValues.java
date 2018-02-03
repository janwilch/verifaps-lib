package edu.kit.iti.formal.automation.st.util;

/*-
 * #%L
 * iec61131lang
 * %%
 * Copyright (C) 2016 - 2017 Alexander Weigl
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

import edu.kit.iti.formal.automation.scope.Scope;
import edu.kit.iti.formal.automation.st.ast.TopLevelElement;
import edu.kit.iti.formal.automation.st.ast.TopLevelElements;
import edu.kit.iti.formal.automation.st.ast.TopLevelScopeElement;

/**
 * @author Alexander Weigl
 * @version 1 (15.04.17)
 */
public class ExplicitIntializationValues {
    public static TopLevelElements makeExplicitInitializationValues(
            TopLevelElements elements) {
        //return new TopLevelElements(elements.stream()
        //   .map(ExplicitIntializationValues::makeExplicitInitializationValues)
        //   .collect(Collectors.toList()));
        return null;
    }

    public static TopLevelElement makeExplicitInitializationValues(
            TopLevelScopeElement element) {
        Scope scope = element.getScope();
        //makeExplicitInitializationValues(scope);
        return element;
    }
}
