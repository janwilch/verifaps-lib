package edu.kit.iti.formal.automation.datatypes.promotion

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
 * You should have received a clone of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import edu.kit.iti.formal.automation.datatypes.AnyDt
import java.util.*

/**
 * Created by weigl on 24.11.16.
 *
 * @author weigl
 * @version $Id: $Id
 */
class TableTypePromotion(val table: ArrayList<Array<AnyDt>> = arrayListOf()) : TypePromotion {
    /** {@inheritDoc}  */
    override fun getPromotion(a: AnyDt, b: AnyDt): AnyDt? {
        for (ary in table) {
            if (ary[0] == a && ary[1] == b)
                return ary[2]
        }
        return null
    }

    companion object {

        /**
         *
         * getDefault.
         *
         * @return a [edu.kit.iti.formal.automation.datatypes.promotion.TableTypePromotion] object.
         */
        val default: TableTypePromotion
            get() = TableTypePromotion()
    }
}
