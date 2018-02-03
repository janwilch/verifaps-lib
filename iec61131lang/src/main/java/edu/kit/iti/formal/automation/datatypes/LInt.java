package edu.kit.iti.formal.automation.datatypes;

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

import java.util.Optional;

/**
 * <p>LInt class.</p>
 *
 * @author weigl
 * @version $Id: $Id
 */
public final class LInt extends AnySignedInt {
    /**
     * <p>Constructor for LInt.</p>
     */
    public LInt() {
        super(64);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AnyInt> next() {
        return Optional.empty();
    }

    @Override
    public AnyUnsignedInt asUnsgined() {
        return DataTypes.ULINT;
    }

    @Override
    public <T> T accept(DataTypeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
