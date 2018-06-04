package edu.kit.iti.formal.automation.builtin

/*-
 * #%L
 * iec61131lang
 * %%
 * Copyright (C) 2018 Alexander Weigl
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

import edu.kit.iti.formal.automation.IEC61131Facade
import edu.kit.iti.formal.automation.st.ast.TopLevelElements
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Alexander Weigl
 * @version 1 (08.03.18)
 */
object BuiltinLoader {
    val DEFAULT_LIST = "/builtins/default"

    private val logger = LoggerFactory.getLogger(BuiltinLoader::class.java!!)

    @Throws(IOException::class, URISyntaxException::class)
    fun loadDefault(): TopLevelElements {
        return loadFromClasspath(DEFAULT_LIST)
    }

    @Throws(IOException::class, URISyntaxException::class)
    private fun loadFromClasspath(indexFile: String): TopLevelElements {
        val resource = BuiltinLoader::class.java!!.getResource(indexFile)
                ?: throw RuntimeException("Could not find $indexFile in classpath.")
        val p = Paths.get(resource.toURI())
        val lines = IOUtils.readLines(resource.openStream(), "utf-8")
        val prefix = p.getParent()
        val tle = TopLevelElements()
        lines.forEach { it ->
            try {
                val load = prefix.resolve(it)
                val a = IEC61131Facade.file(load)
                tle.addAll(a)
            } catch (e: IOException) {
                logger.error("could not parse built in $it", e)
            }
        }
        return tle
    }
}
