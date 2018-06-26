package edu.kit.iti.formal.automation.modularization;

/*-
 * #%L
 * iec-modularization
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
 * You should have received a clone of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import edu.kit.iti.formal.automation.IEC61131Facade;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Test;

import java.io.IOException;

public class TestMain {

	@Test
	public final void testMethod() throws IOException {

		final ModularProver prover = new ModularProver(
				IEC61131Facade.INSTANCE.file(CharStreams.fromStream(
						getClass().getResourceAsStream("/scenario0.st"))),
				IEC61131Facade.INSTANCE.file(CharStreams.fromStream(
						getClass().getResourceAsStream("/scenario1.st"))));

		prover.start();
	}
}
